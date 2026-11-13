package com.gymsystem.basic.payroll;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

public final class PayrollDto {
    private PayrollDto() {
    }

    public record GenerateRequest(
            @NotNull(message = "month is required (format yyyy-MM)") YearMonth month,
            /** optional - empty means every active employee that has a pay plan */
            Long staffId) {
    }

    public record DeductionRequest(
            @NotNull(message = "Deduction amount is required")
            @DecimalMin(value = "0.00", message = "Deduction cannot be negative")
            @Digits(integer = 8, fraction = 2, message = "Deduction must have at most 2 decimals")
            BigDecimal amount,

            @Size(max = 255, message = "Note must be at most 255 characters")
            String note) {
    }

    public record Response(Long id, Long staffId, String staffName, YearMonth month, BigDecimal hoursWorked,
                           BigDecimal hourlyAmount, BigDecimal baseAmount, BigDecimal commissionAmount,
                           BigDecimal bonusAmount, BigDecimal deductionAmount, BigDecimal netAmount,
                           Payroll.Status status, String deductionNote, Long generatedById,
                           LocalDateTime generatedAt, LocalDateTime paidAt) {
        public static Response from(Payroll p) {
            return new Response(p.getId(), p.getStaff().getId(), p.getStaff().getFullName(),
                    YearMonth.from(p.getPeriodMonth()), p.getHoursWorked(), p.getHourlyAmount(),
                    p.getBaseAmount(), p.getCommissionAmount(), p.getBonusAmount(), p.getDeductionAmount(),
                    p.getNetAmount(), p.getStatus(), p.getDeductionNote(), p.getGeneratedBy().getId(),
                    p.getGeneratedAt(), p.getPaidAt());
        }
    }

    public record GenerateResponse(YearMonth month, List<Response> payrolls, List<String> skipped) {
    }
}
