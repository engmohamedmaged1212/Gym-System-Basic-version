package com.gymsystem.basic.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Spring Security view of a staff member (built from the DB at login / refresh). */
public class StaffPrincipal implements UserDetails {
    private final Long staffId;
    private final Long branchId;
    private final String email;
    private final String passwordHash;
    private final String roleCode;
    private final boolean enabled;
    private final List<String> permissions;

    public StaffPrincipal(Long staffId, Long branchId, String email, String passwordHash,
                          String roleCode, boolean enabled, List<String> permissions) {
        this.staffId = staffId;
        this.branchId = branchId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roleCode = roleCode;
        this.enabled = enabled;
        this.permissions = permissions;
    }

    public Long getStaffId() { return staffId; }

    public Long getBranchId() { return branchId; }

    public String getEmail() { return email; }

    public String getRoleCode() { return roleCode; }

    public List<String> getPermissions() { return permissions; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        var authorities = new ArrayList<GrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
        permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        return authorities;
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isEnabled() { return enabled; }
}
