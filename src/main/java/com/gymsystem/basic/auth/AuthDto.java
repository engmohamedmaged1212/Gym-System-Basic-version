package com.gymsystem.basic.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AuthDto {
    private AuthDto() {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email is not valid")
            String email,

            @NotBlank(message = "Password is required")
            String password) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Old password is required")
            String oldPassword,

            @NotBlank(message = "New password is required")
            @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
            String newPassword) {
    }

    public record SessionResponse(Long id, Long staffId, String staffName, LocalDateTime loginAt,
                                  LocalDateTime logoutAt, String ipAddress, String deviceInfo) {
        public static SessionResponse from(StaffLoginSession s) {
            return new SessionResponse(s.getId(), s.getStaff().getId(), s.getStaff().getFullName(),
                    s.getLoginAt(), s.getLogoutAt(), s.getIpAddress(), s.getDeviceInfo());
        }
    }
}
