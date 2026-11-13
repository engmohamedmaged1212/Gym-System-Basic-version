package com.gymsystem.basic.staff;

import com.gymsystem.basic.branch.Branch;
import com.gymsystem.basic.branch.BranchRepository;
import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.exception.BusinessRuleException;
import com.gymsystem.basic.exception.DuplicateResourceException;
import com.gymsystem.basic.exception.FalseInputData;
import com.gymsystem.basic.exception.ForbiddenOperationException;
import com.gymsystem.basic.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * One service for every kind of employee (owner, manager, receptionist, trainer...).
 * They are the same table; what makes them different is the role and the permissions.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StaffService {
    private static final String OWNER = "OWNER";

    private final StaffRepository staffRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffDto.Response create(StaffDto.CreateRequest req, AuthenticatedUser actor) {
        String email = req.email().trim().toLowerCase();
        if (staffRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        Role role = findRole(req.roleCode());
        assertCanAssign(role, actor);

        Staff s = new Staff();
        s.setBranch(findBranch(req.branchId()));
        s.setRole(role);
        s.setFullName(req.fullName().trim());
        s.setEmail(email);
        s.setPhone(req.phone());
        s.setPasswordHash(passwordEncoder.encode(req.password()));
        s.setHireDate(req.hireDate() != null ? req.hireDate() : LocalDate.now());
        return StaffDto.Response.from(staffRepository.save(s));
    }

    public StaffDto.Response update(Long id, StaffDto.UpdateRequest req, AuthenticatedUser actor) {
        Staff s = getEntity(id);
        assertCanManage(s, actor);
        if (!s.getRole().getCode().equals(req.roleCode())) {
            Role role = findRole(req.roleCode());
            assertCanAssign(role, actor);
            s.setRole(role);
        }
        if (!s.getBranch().getId().equals(req.branchId())) {
            s.setBranch(findBranch(req.branchId()));
        }
        s.setFullName(req.fullName().trim());
        s.setPhone(req.phone());
        return StaffDto.Response.from(s);
    }

    public StaffDto.Response changeStatus(Long id, Staff.Status status, AuthenticatedUser actor) {
        Staff s = getEntity(id);
        assertCanManage(s, actor);
        if (actor.staffId().equals(id) && status != Staff.Status.ACTIVE) {
            throw new BusinessRuleException("You cannot deactivate your own account");
        }
        s.setStatus(status);
        return StaffDto.Response.from(s);
    }

    public void resetPassword(Long id, String newPassword, AuthenticatedUser actor) {
        Staff s = getEntity(id);
        assertCanManage(s, actor);
        s.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    @Transactional(readOnly = true)
    public StaffDto.Response get(Long id) {
        return StaffDto.Response.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffDto.Response> search(String q, String roleCode, Staff.Status status,
                                                  Long branchId, Pageable pageable) {
        return PageResponse.of(staffRepository
                .search(blankToNull(q), blankToNull(roleCode), status, branchId, pageable)
                .map(StaffDto.Response::from));
    }

    // ------------------------------------------------------------ QR
    @Transactional(readOnly = true)
    public StaffDto.QrResponse getQr(Long id) {
        Staff s = getEntity(id);
        return new StaffDto.QrResponse(s.getId(), s.getQrToken());
    }

    public StaffDto.QrResponse regenerateQr(Long id, AuthenticatedUser actor) {
        Staff s = getEntity(id);
        assertCanManage(s, actor);
        s.setQrToken(UUID.randomUUID().toString());
        return new StaffDto.QrResponse(s.getId(), s.getQrToken());
    }

    // ------------------------------------------------------------ permissions
    @Transactional(readOnly = true)
    public StaffDto.PermissionsResponse getPermissions(Long id) {
        Staff s = getEntity(id);
        Set<String> role = new TreeSet<>();
        s.getRole().getPermissions().forEach(p -> role.add(p.getCode()));
        Set<String> extra = new TreeSet<>(userPermissionRepository.findPermissionCodesByStaffId(id));
        Set<String> effective = new TreeSet<>(role);
        effective.addAll(extra);
        return new StaffDto.PermissionsResponse(id, s.getRole().getCode(),
                List.copyOf(role), List.copyOf(extra), List.copyOf(effective));
    }

    /** Replaces the extra permissions of one employee (on top of what his role already gives). */
    public StaffDto.PermissionsResponse setExtraPermissions(Long id, Set<String> codes, AuthenticatedUser actor) {
        Staff s = getEntity(id);
        assertCanManage(s, actor);

        List<Permission> wanted = codes.isEmpty() ? List.of() : permissionRepository.findByCodeIn(codes);
        if (wanted.size() != codes.size()) {
            throw new FalseInputData("One or more permission codes do not exist");
        }

        List<UserPermission> existing = userPermissionRepository.findByStaffId(id);
        Set<String> existingCodes = new HashSet<>();
        for (UserPermission up : existing) {
            existingCodes.add(up.getPermission().getCode());
            if (!codes.contains(up.getPermission().getCode())) {
                userPermissionRepository.delete(up);
            }
        }
        Staff grantedBy = staffRepository.getReferenceById(actor.staffId());
        for (Permission p : wanted) {
            if (!existingCodes.contains(p.getCode())) {
                UserPermission up = new UserPermission();
                up.setId(new UserPermissionId(s.getId(), p.getId()));
                up.setStaff(s);
                up.setPermission(p);
                up.setGrantedBy(grantedBy);
                userPermissionRepository.save(up);
            }
        }
        userPermissionRepository.flush();
        return getPermissions(id);
    }

    // ------------------------------------------------------------ roles / permissions lists
    @Transactional(readOnly = true)
    public List<StaffDto.RoleResponse> listRoles() {
        return roleRepository.findAllWithPermissions().stream()
                .map(r -> new StaffDto.RoleResponse(r.getId(), r.getCode(), r.getName(),
                        r.getPermissions().stream().map(Permission::getCode).sorted().toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StaffDto.PermissionResponse> listPermissions() {
        return permissionRepository.findAllByOrderByCodeAsc().stream()
                .map(p -> new StaffDto.PermissionResponse(p.getId(), p.getCode(), p.getDescription()))
                .toList();
    }

    // ------------------------------------------------------------ helpers
    private Staff getEntity(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member " + id + " not found"));
    }

    private Role findRole(String code) {
        return roleRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new FalseInputData("Unknown role '" + code + "'"));
    }

    private Branch findBranch(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new FalseInputData("Branch " + id + " does not exist"));
    }

    /** Only an owner can create or promote another owner. */
    private void assertCanAssign(Role role, AuthenticatedUser actor) {
        if (OWNER.equals(role.getCode()) && !OWNER.equals(actor.role())) {
            throw new ForbiddenOperationException("Only the owner can assign the OWNER role");
        }
    }

    /** Only an owner can change another owner. */
    private void assertCanManage(Staff target, AuthenticatedUser actor) {
        if (OWNER.equals(target.getRole().getCode()) && !OWNER.equals(actor.role())) {
            throw new ForbiddenOperationException("Only the owner can change an owner account");
        }
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
