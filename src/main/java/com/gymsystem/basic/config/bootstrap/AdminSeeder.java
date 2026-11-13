package com.gymsystem.basic.config.bootstrap;

import com.gymsystem.basic.branch.Branch;
import com.gymsystem.basic.branch.BranchRepository;
import com.gymsystem.basic.staff.Role;
import com.gymsystem.basic.staff.RoleRepository;
import com.gymsystem.basic.staff.Staff;
import com.gymsystem.basic.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Creates the first OWNER account (and a first branch) when the system starts empty. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder {
    private final StaffRepository staffRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminConfig adminConfig;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void createOwner() {
        // in case there is already an owner
        if (staffRepository.existsByRole_Code("OWNER")) {
            return;
        }
        if (isBlank(adminConfig.getEmail()) || isBlank(adminConfig.getPassword())) {
            log.warn("No owner exists and app.admin.email / app.admin.password are not set - skipping seeding");
            return;
        }
        Role ownerRole = roleRepository.findByCode("OWNER")
                .orElseThrow(() -> new IllegalStateException("Roles are missing, run gym_schema.sql first"));

        Branch branch = branchRepository.findAll(PageRequest.of(0, 1)).stream().findFirst()
                .orElseGet(() -> {
                    Branch b = new Branch();
                    b.setName("Main Branch");
                    return branchRepository.save(b);
                });

        Staff owner = new Staff();
        owner.setBranch(branch);
        owner.setRole(ownerRole);
        owner.setFullName(adminConfig.getName());
        owner.setEmail(adminConfig.getEmail().trim().toLowerCase());
        owner.setPasswordHash(passwordEncoder.encode(adminConfig.getPassword()));
        owner.setHireDate(LocalDate.now());
        staffRepository.save(owner);
        log.info("Owner account created: {}", owner.getEmail());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
