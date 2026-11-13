package com.gymsystem.basic.subscription;

import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.config.security.Authz;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    /**
     * POST /api/v1/subscriptions        (permission: SUBSCRIPTION_SELL)
     * Sells a plan to a member. If offerId is empty, the cheapest offer running now for this plan and
     * branch is applied automatically. The sale bonus goes to soldByStaffId (the trainer who persuaded the client).
     *
     * Request:
     * {
     *   "memberId": 17,
     *   "planId": 3,
     *   "branchId": 1,
     *   "offerId": null,
     *   "startDate": "2026-11-13",
     *   "soldByStaffId": 5,
     *   "note": "Came with his brother",
     *   "initialPayment": { "amount": 1000.00, "method": "CASH" }
     * }
     * (only memberId and planId are required)
     *
     * Response 201:
     * {
     *   "id": 31, "memberId": 17, "memberName": "Omar Khaled", "planId": 3, "planName": "Half-year",
     *   "branchId": 1, "branchName": "Main Branch", "offerId": 9, "offerName": "Friday flash sale",
     *   "accessAllBranches": false, "startDate": "2026-11-13", "endDate": "2027-05-11",
     *   "listPrice": 2000.00, "discountAmount": 500.00, "amountDue": 1500.00,
     *   "paidAmount": 1000.00, "refundedAmount": 0, "remainingAmount": 500.00,
     *   "status": "ACTIVE", "soldById": 5, "soldByName": "Ahmed Hassan", "createdById": 3,
     *   "createdAt": "2026-11-13T12:00:00", "cancelledAt": null, "note": "Came with his brother"
     * }
     *
     * Response 422: { "error": "This offer is not running right now" }
     * Response 422: { "error": "Payment exceeds the remaining amount (1500.00)" }
     * Response 400: { "error": "Plan 99 does not exist" }
     */
    @PostMapping
    @PreAuthorize(Authz.SUBSCRIPTION_SELL)
    public ResponseEntity<SubscriptionDto.Response> sell(@RequestBody @Valid SubscriptionDto.SellRequest req,
                                                         @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.sell(req, actor));
    }

    /**
     * GET /api/v1/subscriptions?memberId=17&status=ACTIVE&branchId=1&soldById=5&expiringInDays=7&page=0&size=20
     * (permission: SUBSCRIPTION_SELL, PAYMENT_RECORD, MEMBER_MANAGE or REPORTS_VIEW)
     * expiringInDays=7 returns subscriptions that end within 7 days (use it with status=ACTIVE).
     *
     * Response 200: { "content": [ subscription, ... ], "page": 0, "size": 20, "totalElements": 5, "totalPages": 1 }
     */
    @GetMapping
    @PreAuthorize(Authz.SUBSCRIPTION_VIEW)
    public PageResponse<SubscriptionDto.Response> search(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) MemberSubscription.Status status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long soldById,
            @RequestParam(required = false) Integer expiringInDays,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return subscriptionService.search(memberId, status, branchId, soldById, expiringInDays, pageable);
    }

    /**
     * GET /api/v1/subscriptions/31        (permission: SUBSCRIPTION_SELL, PAYMENT_RECORD, MEMBER_MANAGE or REPORTS_VIEW)
     *
     * Response 200: one subscription (same shape as the sell response)
     * Response 404: { "error": "Subscription 31 not found" }
     */
    @GetMapping("/{id}")
    @PreAuthorize(Authz.SUBSCRIPTION_VIEW)
    public SubscriptionDto.Response get(@PathVariable Long id) {
        return subscriptionService.get(id);
    }

    /**
     * POST /api/v1/subscriptions/31/cancel        (permission: PAYMENT_RECORD, because it can give money back)
     * Cancels the subscription, removes its unpaid sale bonus, and records the refund if refundAmount is sent.
     *
     * Request:
     * { "refundAmount": 500.00, "reason": "Moved to another city" }      (both optional)
     *
     * Response 200: the subscription with "status": "CANCELLED", "cancelledAt" filled and
     *               "refundedAmount": 500.00
     *
     * Response 422: { "error": "This subscription is already cancelled" }
     * Response 422: { "error": "Refund exceeds the amount paid so far (1000.00)" }
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize(Authz.PAYMENT_RECORD)
    public SubscriptionDto.Response cancel(@PathVariable Long id,
                                           @RequestBody(required = false) @Valid SubscriptionDto.CancelRequest req,
                                           @AuthenticationPrincipal AuthenticatedUser actor) {
        var body = req != null ? req : new SubscriptionDto.CancelRequest(null, null);
        return subscriptionService.cancel(id, body, actor);
    }
}
