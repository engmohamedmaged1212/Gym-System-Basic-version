package com.gymsystem.basic.payroll;

import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.config.security.Authz;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pay-plans")
@RequiredArgsConstructor
public class PayPlanController {
    private final PayPlanService payPlanService;

    /**
     * POST /api/v1/pay-plans        (permission: PAYROLL_MANAGE)
     * Starts a new pay plan for an employee and closes his previous plan the day before.
     *
     * Request (hourly + commission):
     * {
     *   "staffId": 5,
     *   "hourlyRate": 60.00,
     *   "baseSalary": 0,
     *   "saleCommissionPercent": 2.5,
     *   "effectiveFrom": "2026-12-01"
     * }
     *
     * Response 201:
     * {
     *   "id": 12, "staffId": 5, "staffName": "Ahmed Hassan", "hourlyRate": 60.00, "baseSalary": 0.00,
     *   "saleCommissionPercent": 2.50, "effectiveFrom": "2026-12-01", "effectiveTo": null,
     *   "createdById": 1, "createdAt": "2026-11-13T13:00:00"
     * }
     *
     * Response 400: { "error": "Set an hourly rate, a base salary or a commission" }
     * Response 400: { "error": "The new plan must start after the current plan's start date (2026-06-01)" }
     */
    @PostMapping
    @PreAuthorize(Authz.PAYROLL_MANAGE)
    public ResponseEntity<PayPlanDto.Response> set(@RequestBody @Valid PayPlanDto.SetRequest req,
                                                   @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payPlanService.set(req, actor));
    }

    /**
     * GET /api/v1/pay-plans/staff/5        (permission: PAYROLL_MANAGE or REPORTS_VIEW)
     *
     * Response 200: [ plan, plan, ... ]  newest first (same shape as the create response)
     */
    @GetMapping("/staff/{staffId}")
    @PreAuthorize(Authz.PAYROLL_VIEW)
    public List<PayPlanDto.Response> history(@PathVariable Long staffId) {
        return payPlanService.history(staffId);
    }

    /**
     * GET /api/v1/pay-plans/me        (any logged-in staff)   - the employee sees his own current terms
     *
     * Response 200: the current plan (same shape as the create response)
     * Response 404: { "error": "No pay plan is set for this employee" }
     */
    @GetMapping("/me")
    @PreAuthorize(Authz.AUTHENTICATED)
    public PayPlanDto.Response mine(@AuthenticationPrincipal AuthenticatedUser actor) {
        return payPlanService.current(actor.staffId());
    }
}
