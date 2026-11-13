package com.gymsystem.basic.auth;

import com.gymsystem.basic.exception.NotAuthException;
import com.gymsystem.basic.staff.Staff;
import com.gymsystem.basic.staff.StaffRepository;
import com.gymsystem.basic.staff.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class StaffUserDetailsService implements UserDetailsService {
    private final StaffRepository staffRepository;
    private final UserPermissionRepository userPermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public StaffPrincipal loadUserByUsername(String email) throws UsernameNotFoundException {
        Staff s = staffRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        return toPrincipal(s);
    }

    /** Used by the refresh endpoint to rebuild fresh permissions. */
    @Transactional(readOnly = true)
    public StaffPrincipal loadById(Long staffId) {
        Staff s = staffRepository.findById(staffId)
                .orElseThrow(() -> new NotAuthException("Account no longer exists"));
        return toPrincipal(s);
    }

    private StaffPrincipal toPrincipal(Staff s) {
        // permissions of the role + the extra ones the owner granted to this employee
        Set<String> permissions = new TreeSet<>();
        s.getRole().getPermissions().forEach(p -> permissions.add(p.getCode()));
        permissions.addAll(userPermissionRepository.findPermissionCodesByStaffId(s.getId()));
        return new StaffPrincipal(s.getId(), s.getBranch().getId(), s.getEmail(), s.getPasswordHash(),
                s.getRole().getCode(), s.getStatus() == Staff.Status.ACTIVE, List.copyOf(permissions));
    }
}
