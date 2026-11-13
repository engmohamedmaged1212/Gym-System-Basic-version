package com.gymsystem.basic.member;

import com.gymsystem.basic.common.Patterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class MemberDto {
    private MemberDto() {
    }

    public record CreateRequest(
            /** optional - default: the branch of the logged-in employee */
            Long branchId,

            @NotBlank(message = "Full name is required")
            @Size(max = 120, message = "Full name must be at most 120 characters")
            String fullName,

            @NotBlank(message = "Phone is required")
            @Pattern(regexp = Patterns.PHONE, message = "Phone must be 8-15 digits")
            String phone,

            @Email(message = "Email is not valid")
            @Size(max = 150, message = "Email must be at most 150 characters")
            String email,

            @Past(message = "Birth date must be in the past")
            LocalDate birthDate,

            Member.Gender gender,

            @Size(max = 500, message = "Notes must be at most 500 characters")
            String notes) {
    }

    public record UpdateRequest(
            Long branchId,

            @NotBlank(message = "Full name is required")
            @Size(max = 120, message = "Full name must be at most 120 characters")
            String fullName,

            @NotBlank(message = "Phone is required")
            @Pattern(regexp = Patterns.PHONE, message = "Phone must be 8-15 digits")
            String phone,

            @Email(message = "Email is not valid")
            @Size(max = 150, message = "Email must be at most 150 characters")
            String email,

            @Past(message = "Birth date must be in the past")
            LocalDate birthDate,

            Member.Gender gender,

            @Size(max = 500, message = "Notes must be at most 500 characters")
            String notes) {
    }

    public record StatusRequest(
            @NotNull(message = "Status is required") Member.Status status) {
    }

    public record Response(Long id, Long branchId, String branchName, String fullName, String phone,
                           String email, LocalDate birthDate, Member.Gender gender, String qrToken,
                           LocalDate joinDate, Member.Status status, String notes, LocalDateTime createdAt) {
        public static Response from(Member m) {
            return new Response(m.getId(), m.getBranch().getId(), m.getBranch().getName(), m.getFullName(),
                    m.getPhone(), m.getEmail(), m.getBirthDate(), m.getGender(), m.getQrToken(),
                    m.getJoinDate(), m.getStatus(), m.getNotes(), m.getCreatedAt());
        }
    }

    public record QrResponse(Long memberId, String qrToken) {
    }
}
