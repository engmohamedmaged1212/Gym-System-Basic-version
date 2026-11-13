package com.gymsystem.basic.config.jwt;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
public class Jwt {
    private final Claims claims;
    private final String tokenValue;

    public boolean isValid() {
        return claims.getExpiration().after(new Date());
    }

    public boolean isAccessToken() {
        return "access".equals(claims.get("type", String.class));
    }

    public boolean isRefreshToken() {
        return "refresh".equals(claims.get("type", String.class));
    }

    public Long getStaffId() {
        return Long.valueOf(claims.getSubject());
    }

    public String getRole() {
        return claims.get("role", String.class);
    }

    public Long getBranchId() {
        Number n = claims.get("branch_id", Number.class);
        return n == null ? null : n.longValue();
    }

    /** id of the staff_login_session row this token belongs to */
    public Long getSessionId() {
        Number n = claims.get("sid", Number.class);
        return n == null ? null : n.longValue();
    }

    public List<String> getPermissions() {
        Object value = claims.get("permissions");
        if (value instanceof Collection<?> c) {
            return c.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    @Override
    public String toString() {
        return this.tokenValue;
    }
}
