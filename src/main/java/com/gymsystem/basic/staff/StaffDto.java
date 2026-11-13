package com.gymsystem.basic.staff;

import com.gymsystem.basic.common.Patterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class StaffDto {
    private StaffDto() {
    }

    // ------------------------------------------------------------ requests
    public record CreateRequest(
            @NotNull(message = "Branch is required") Long branchId,

            @NotBlank(message = "Role is required")
            @Size(max = 30, message = "Role code must be at most 30 characters")
            String roleCode,

            @NotBlank(message = "Full name is required")
            @Size(max = 120, message = "Full name must be at most 120 characters")
            String fullName,

            @NotBlank(message = "Email is required")
            @Email(message = "Email is not valid")
            @Size(max = 150, message = "Email must be at most 150 characters")
            String email,

            @Pattern(regexp = Patterns.PHONE, message = "Phone must be 8-15 digits")
            String phone,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
            String password,

            LocalDate hireDate) {
    }

    public record UpdateRequest(
            @NotNull(message = "Branch is required") Long branchId,

            @NotBlank(message = "Role is required")
            @Size(max = 30, message = "Role code must be at most 30 characters")
            String roleCode,

            @NotBlank(message = "Full name is required")
            @Size(max = 120, message = "Full name must be at most 120 characters")
            String fullName,

            @Pattern(regexp = Patterns.PHONE, message = "Phone must be 8-15 digits")
            String phone) {
    }

    public record StatusRequest(
            @NotNull(message = "Status is required") Staff.Status status) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "New password is required")
            @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
            String newPassword) {
    }

    public record PermissionsRequest(
            @NotNull(message = "permissionCodes is required (send [] to remove all extra permissions)")
            Set<String> permissionCodes) {
    }

    // ------------------------------------------------------------ responses
    public record Response(Long id, Long branchId, String branchName, String roleCode, String roleName,
                           String fullName, String email, String phone, LocalDate hireDate,
                           Staff.Status status, LocalDateTime createdAt) {
        public static Response from(Staff s) {
            return new Response(s.getId(), s.getBranch().getId(), s.getBranch().getName(),
                    s.getRole().getCode(), s.getRole().getName(), s.getFullName(), s.getEmail(),
                    s.getPhone(), s.getHireDate(), s.getStatus(), s.getCreatedAt());
        }
    }

    public record QrResponse(Long staffId, String qrToken) {
    }

    public record PermissionsResponse(Long staffId, String roleCode, List<String> rolePermissions,
                                      List<String> extraPermissions, List<String> effectivePermissions) {
    }

    public record RoleResponse(Short id, String code, String name, List<String> permissions) {
    }

    public record PermissionResponse(Short id, String code, String description) {
    }
}
