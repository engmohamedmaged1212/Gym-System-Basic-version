package com.gymsystem.basic.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class PaymentDto {
    private PaymentDto() {
    }

    public record CreateRequest(
            @NotNull(message = "subscriptionId is required") Long subscriptionId,

            /** optional, default PAYMENT */
            Payment.Type type,

            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
            @Digits(integer = 8, fraction = 2, message = "Amount must have at most 2 decimals")
            BigDecimal amount,

            /** optional, default CASH */
            Payment.Method method,

            @Size(max = 255, message = "Note must be at most 255 characters")
            String note,

            /** optional, default now */
            LocalDateTime paidAt) {
    }

    public record Response(Long id, Long subscriptionId, Payment.Type type, BigDecimal amount,
                           Payment.Method method, LocalDateTime paidAt, Long receivedById,
                           String receivedByName, String note) {
        public static Response from(Payment p) {
            return new Response(p.getId(), p.getMemberSubscription().getId(), p.getType(), p.getAmount(),
                    p.getMethod(), p.getPaidAt(), p.getReceivedBy().getId(), p.getReceivedBy().getFullName(),
                    p.getNote());
        }
    }

    public record SummaryResponse(LocalDate from, LocalDate to, Long branchId, BigDecimal totalPayments,
                                  BigDecimal totalRefunds, BigDecimal net) {
    }
}
