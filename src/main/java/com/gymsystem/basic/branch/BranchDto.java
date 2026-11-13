package com.gymsystem.basic.branch;

import com.gymsystem.basic.common.Patterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class BranchDto {
    private BranchDto() {
    }

    public record Request(
            @NotBlank(message = "Branch name is required")
            @Size(max = 100, message = "Branch name must be at most 100 characters")
            String name,

            @Size(max = 255, message = "Address must be at most 255 characters")
            String address,

            @Pattern(regexp = Patterns.PHONE, message = "Phone must be 8-15 digits")
            String phone) {
    }

    public record ActiveRequest(
            @NotNull(message = "active is required") Boolean active) {
    }

    public record Response(Long id, String name, String address, String phone,
                           boolean active, LocalDateTime createdAt) {
        public static Response from(Branch b) {
            return new Response(b.getId(), b.getName(), b.getAddress(), b.getPhone(), b.isActive(), b.getCreatedAt());
        }
    }
}
