package com.gymsystem.basic.plan;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class PlanDto {
    private PlanDto() {
    }

    public record Request(
            @NotBlank(message = "Plan name is required")
            @Size(max = 100, message = "Plan name must be at most 100 characters")
            String name,

            @Size(max = 500, message = "Description must be at most 500 characters")
            String description,

            @NotNull(message = "Duration in days is required")
            @Min(value = 1, message = "Duration must be at least 1 day")
            @Max(value = 3650, message = "Duration must be at most 3650 days")
            Integer durationDays,

            @NotNull(message = "Price is required")
            @DecimalMin(value = "0.00", message = "Price cannot be negative")
            @Digits(integer = 8, fraction = 2, message = "Price must have at most 2 decimals")
            BigDecimal price,

            /** optional, default 0 - fixed bonus for the employee who sells this plan */
            @DecimalMin(value = "0.00", message = "Sale bonus cannot be negative")
            @Digits(integer = 8, fraction = 2, message = "Sale bonus must have at most 2 decimals")
            BigDecimal saleBonusAmount,

            @NotNull(message = "includesGym is required") Boolean includesGym,

            @NotNull(message = "allBranches is required") Boolean allBranches,

            /** optional - the classes included in the plan */
            Set<Long> classIds) {
    }

    public record ActiveRequest(
            @NotNull(message = "active is required") Boolean active) {
    }

    public record ClassSummary(Long id, String name) {
    }

    public record Response(Long id, String name, String description, Integer durationDays, BigDecimal price,
                           BigDecimal saleBonusAmount, boolean includesGym, boolean allBranches, boolean active,
                           List<ClassSummary> classes, LocalDateTime createdAt) {
        public static Response from(SubscriptionPlan p) {
            List<ClassSummary> classes = p.getClasses().stream()
                    .sorted(Comparator.comparing(c -> c.getName()))
                    .map(c -> new ClassSummary(c.getId(), c.getName()))
                    .toList();
            return new Response(p.getId(), p.getName(), p.getDescription(), p.getDurationDays(), p.getPrice(),
                    p.getSaleBonusAmount(), p.isIncludesGym(), p.isAllBranches(), p.isActive(), classes,
                    p.getCreatedAt());
        }
    }
}
