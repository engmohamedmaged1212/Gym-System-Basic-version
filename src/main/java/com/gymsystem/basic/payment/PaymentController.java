package com.gymsystem.basic.payment;

import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.config.security.Authz;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    /**
     * POST /api/v1/payments        (permission: PAYMENT_RECORD)
     * Records a payment (or a refund) of a subscription. Amounts are always positive.
     *
     * Request:
     * {
     *   "subscriptionId": 31,
     *   "type": "PAYMENT",
     *   "amount": 500.00,
     *   "method": "CASH",
     *   "note": "Second installment"
     * }
     * (type defaults to PAYMENT, method to CASH, paidAt to now)
     *
     * Response 201:
     * {
     *   "id": 77, "subscriptionId": 31, "type": "PAYMENT", "amount": 500.00, "method": "CASH",
     *   "paidAt": "2026-11-13T12:10:00", "receivedById": 3, "receivedByName": "Sara Ali",
     *   "note": "Second installment"
     * }
     *
     * Response 422: { "error": "Payment exceeds the remaining amount (250.00)" }
     * Response 422: { "error": "Refund exceeds the amount paid so far (0)" }
     */
    @PostMapping
    @PreAuthorize(Authz.PAYMENT_RECORD)
    public ResponseEntity<PaymentDto.Response> create(@RequestBody @Valid PaymentDto.CreateRequest req,
                                                      @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(req, actor));
    }

    /**
     * GET /api/v1/payments/subscription/31        (permission: PAYMENT_RECORD, SUBSCRIPTION_SELL, MEMBER_MANAGE or REPORTS_VIEW)
     *
     * Response 200:
     * [ { "id": 77, "subscriptionId": 31, "type": "PAYMENT", "amount": 500.00, "method": "CASH",
     *     "paidAt": "2026-11-13T12:10:00", "receivedById": 3, "receivedByName": "Sara Ali", "note": null } ]
     */
    @GetMapping("/subscription/{subscriptionId}")
    @PreAuthorize(Authz.SUBSCRIPTION_VIEW)
    public List<PaymentDto.Response> forSubscription(@PathVariable Long subscriptionId) {
        return paymentService.listForSubscription(subscriptionId);
    }

    /**
     * GET /api/v1/payments/summary?from=2026-11-01&to=2026-11-30&branchId=1        (permission: REPORTS_VIEW)
     * branchId is optional (all branches).
     *
     * Response 200:
     * { "from": "2026-11-01", "to": "2026-11-30", "branchId": 1,
     *   "totalPayments": 48500.00, "totalRefunds": 1200.00, "net": 47300.00 }
     */
    @GetMapping("/summary")
    @PreAuthorize(Authz.REPORTS_VIEW)
    public PaymentDto.SummaryResponse summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long branchId) {
        return paymentService.summary(from, to, branchId);
    }
}
