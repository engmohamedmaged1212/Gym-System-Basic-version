package com.gymsystem.basic.payroll;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class PayPlanDto {
    private PayPlanDto() {
    }

    /** Any mix works: hourly only, base only, base + hourly, base + commission... (empty = 0) */
    public record SetRequest(
            @NotNull(message = "staffId is required") Long staffId,

            @DecimalMin(value = "0.00", message = "Hourly rate cannot be negative")
            @Digits(integer = 8, fraction = 2, message = "Hourly rate must have at most 2 decimals")
            BigDecimal hourlyRate,

            @DecimalMin(value = "0.00", message = "Base salary cannot be negative")
            @Digits(integer = 8, fraction = 2, message = "Base salary must have at most 2 decimals")
            BigDecimal baseSalary,

            @DecimalMin(value = "0.00", message = "Commission cannot be negative")
            @DecimalMax(value = "100.00", message = "Commission cannot be more than 100%")
            @Digits(integer = 3, fraction = 2, message = "Commission must have at most 2 decimals")
            BigDecimal saleCommissionPercent,

            @NotNull(message = "effectiveFrom is required") LocalDate effectiveFrom) {
    }

    public record Response(Long id, Long staffId, String staffName, BigDecimal hourlyRate, BigDecimal baseSalary,
                           BigDecimal saleCommissionPercent, LocalDate effectiveFrom, LocalDate effectiveTo,
                           Long createdById, LocalDateTime createdAt) {
        public static Response from(StaffPayPlan p) {
            return new Response(p.getId(), p.getStaff().getId(), p.getStaff().getFullName(), p.getHourlyRate(),
                    p.getBaseSalary(), p.getSaleCommissionPercent(), p.getEffectiveFrom(), p.getEffectiveTo(),
                    p.getCreatedBy().getId(), p.getCreatedAt());
        }
    }
}
