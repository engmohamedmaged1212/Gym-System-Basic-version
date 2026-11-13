package com.gymsystem.basic.offer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class OfferDto {
    private OfferDto() {
    }

    public record Request(
            @NotBlank(message = "Offer name is required")
            @Size(max = 100, message = "Offer name must be at most 100 characters")
            String name,

            @NotNull(message = "Plan is required") Long planId,

            /** optional - empty means every branch */
            Long branchId,

            @NotNull(message = "Offer price is required")
            @DecimalMin(value = "0.00", message = "Offer price cannot be negative")
            @Digits(integer = 8, fraction = 2, message = "Offer price must have at most 2 decimals")
            BigDecimal offerPrice,

            /** optional - replaces the plan's sale bonus while the offer is used */
            @DecimalMin(value = "0.00", message = "Sale bonus cannot be negative")
            @Digits(integer = 8, fraction = 2, message = "Sale bonus must have at most 2 decimals")
            BigDecimal saleBonusAmount,

            @NotNull(message = "validFrom is required") LocalDateTime validFrom,

            @NotNull(message = "validTo is required") LocalDateTime validTo) {
    }

    public record ActiveRequest(
            @NotNull(message = "active is required") Boolean active) {
    }

    public record Response(Long id, String name, Long planId, String planName, BigDecimal planPrice,
                           Long branchId, String branchName, BigDecimal offerPrice, BigDecimal saleBonusAmount,
                           LocalDateTime validFrom, LocalDateTime validTo, boolean active, boolean runningNow,
                           Long createdById, LocalDateTime createdAt) {
        public static Response from(Offer o) {
            LocalDateTime now = LocalDateTime.now();
            boolean running = o.isActive() && !now.isBefore(o.getValidFrom()) && !now.isAfter(o.getValidTo());
            return new Response(o.getId(), o.getName(), o.getPlan().getId(), o.getPlan().getName(),
                    o.getPlan().getPrice(),
                    o.getBranch() == null ? null : o.getBranch().getId(),
                    o.getBranch() == null ? null : o.getBranch().getName(),
                    o.getOfferPrice(), o.getSaleBonusAmount(), o.getValidFrom(), o.getValidTo(),
                    o.isActive(), running, o.getCreatedBy().getId(), o.getCreatedAt());
        }
    }
}
