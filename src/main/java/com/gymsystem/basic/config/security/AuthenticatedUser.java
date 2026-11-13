package com.gymsystem.basic.config.security;

/**
 * The logged-in staff member, available in any controller with
 * {@code @AuthenticationPrincipal AuthenticatedUser actor}.
 */
public record AuthenticatedUser(Long staffId, Long branchId, Long sessionId, String role) {
}
