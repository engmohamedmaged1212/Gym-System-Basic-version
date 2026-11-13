package com.gymsystem.basic.gymclass;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class GymClassDto {
    private GymClassDto() {
    }

    public record Request(
            @NotNull(message = "Branch is required") Long branchId,

            /** optional - must be an ACTIVE employee with the TRAINER role */
            Long trainerId,

            @NotBlank(message = "Class name is required")
            @Size(max = 100, message = "Class name must be at most 100 characters")
            String name,

            @Size(max = 500, message = "Description must be at most 500 characters")
            String description) {
    }

    public record Response(Long id, Long branchId, String branchName, Long trainerId, String trainerName,
                           String name, String description, GymClass.Status status,
                           LocalDateTime discontinuedAt, LocalDateTime createdAt) {
        public static Response from(GymClass c) {
            return new Response(c.getId(), c.getBranch().getId(), c.getBranch().getName(),
                    c.getTrainer() == null ? null : c.getTrainer().getId(),
                    c.getTrainer() == null ? null : c.getTrainer().getFullName(),
                    c.getName(), c.getDescription(), c.getStatus(), c.getDiscontinuedAt(), c.getCreatedAt());
        }
    }
}
