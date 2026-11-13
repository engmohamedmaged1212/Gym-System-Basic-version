package com.gymsystem.basic.payroll;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class BonusDto {
    private BonusDto() {
    }

    public record GrantRequest(
            @NotNull(message = "staffId is required") Long staffId,

            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
            @Digits(integer = 8, fraction = 2, message = "Amount must have at most 2 decimals")
            BigDecimal amount,

            /** optional, default OWNER_DISCRETIONARY (SUBSCRIPTION_SALE is created automatically by sales) */
            StaffBonus.ReasonType reasonType,

            @Size(max = 255, message = "Note must be at most 255 characters")
            String note) {
    }

    public record Response(Long id, Long staffId, String staffName, BigDecimal amount,
                           StaffBonus.ReasonType reasonType, Long subscriptionId, Long grantedById,
                           String grantedByName, LocalDateTime grantedAt, String note, Long payrollId) {
        public static Response from(StaffBonus b) {
            return new Response(b.getId(), b.getStaff().getId(), b.getStaff().getFullName(), b.getAmount(),
                    b.getReasonType(),
                    b.getMemberSubscription() == null ? null : b.getMemberSubscription().getId(),
                    b.getGrantedBy().getId(), b.getGrantedBy().getFullName(), b.getGrantedAt(), b.getNote(),
                    b.getPayroll() == null ? null : b.getPayroll().getId());
        }
    }
}
