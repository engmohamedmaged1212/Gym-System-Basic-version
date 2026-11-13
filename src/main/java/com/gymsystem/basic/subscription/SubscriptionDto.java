package com.gymsystem.basic.subscription;

import com.gymsystem.basic.payment.Payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class SubscriptionDto {
    private SubscriptionDto() {
    }

    public record InitialPayment(
            @NotNull(message = "Payment amount is required")
            @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
            @Digits(integer = 8, fraction = 2, message = "Payment amount must have at most 2 decimals")
            BigDecimal amount,

            /** optional, default CASH */
            Payment.Method method) {
    }

    public record SellRequest(
            @NotNull(message = "memberId is required") Long memberId,

            @NotNull(message = "planId is required") Long planId,

            /** optional - default: the branch of the logged-in employee */
            Long branchId,

            /** optional - leave empty to apply the best running offer automatically */
            Long offerId,

            /** optional - default today */
            LocalDate startDate,

            /** optional - the employee who persuaded the client (he earns the sale bonus) */
            Long soldByStaffId,

            @Size(max = 255, message = "Note must be at most 255 characters")
            String note,

            /** optional - money received at the desk right now */
            @Valid InitialPayment initialPayment) {
    }

    public record CancelRequest(
            /** optional - money given back to the member */
            @DecimalMin(value = "0.01", message = "Refund amount must be greater than zero")
            @Digits(integer = 8, fraction = 2, message = "Refund amount must have at most 2 decimals")
            BigDecimal refundAmount,

            @Size(max = 200, message = "Reason must be at most 200 characters")
            String reason) {
    }

    public record Response(Long id, Long memberId, String memberName, Long planId, String planName,
                           Long branchId, String branchName, Long offerId, String offerName,
                           boolean accessAllBranches, LocalDate startDate, LocalDate endDate,
                           BigDecimal listPrice, BigDecimal discountAmount, BigDecimal amountDue,
                           BigDecimal paidAmount, BigDecimal refundedAmount, BigDecimal remainingAmount,
                           MemberSubscription.Status status, Long soldById, String soldByName,
                           Long createdById, LocalDateTime createdAt, LocalDateTime cancelledAt, String note) {

        public static Response from(MemberSubscription ms, BigDecimal paid, BigDecimal refunded) {
            BigDecimal remaining = ms.getAmountDue().subtract(paid).add(refunded);
            return new Response(ms.getId(), ms.getMember().getId(), ms.getMember().getFullName(),
                    ms.getPlan().getId(), ms.getPlan().getName(), ms.getBranch().getId(), ms.getBranch().getName(),
                    ms.getOffer() == null ? null : ms.getOffer().getId(),
                    ms.getOffer() == null ? null : ms.getOffer().getName(),
                    ms.isAccessAllBranches(), ms.getStartDate(), ms.getEndDate(), ms.getListPrice(),
                    ms.getDiscountAmount(), ms.getAmountDue(), paid, refunded, remaining, ms.getStatus(),
                    ms.getSoldBy() == null ? null : ms.getSoldBy().getId(),
                    ms.getSoldBy() == null ? null : ms.getSoldBy().getFullName(),
                    ms.getCreatedBy().getId(), ms.getCreatedAt(), ms.getCancelledAt(), ms.getNote());
        }
    }
}
