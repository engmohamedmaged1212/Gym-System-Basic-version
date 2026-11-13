package com.gymsystem.basic.payroll;

import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.config.security.Authz;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {
    private final PayrollService payrollService;

    /**
     * POST /api/v1/payroll/generate        (permission: PAYROLL_MANAGE)
     * Calculates the payroll of one employee (or of everybody) for a month from his shifts, pay plan,
     * sales and unpaid bonuses. Calling it again recalculates the payroll while it is still DRAFT.
     *
     * Request:
     * { "month": "2026-11", "staffId": 5 }        (staffId optional = every active employee with a pay plan)
     *
     * Response 201:
     * {
     *   "month": "2026-11",
     *   "payrolls": [ {
     *     "id": 8, "staffId": 5, "staffName": "Ahmed Hassan", "month": "2026-11",
     *     "hoursWorked": 122.50, "hourlyAmount": 7350.00, "baseAmount": 0.00,
     *     "commissionAmount": 112.50, "bonusAmount": 350.00, "deductionAmount": 0.00,
     *     "netAmount": 7812.50, "status": "DRAFT", "deductionNote": null, "generatedById": 1,
     *     "generatedAt": "2026-12-01T09:00:00", "paidAt": null } ],
     *   "skipped": [ "Sara Ali: No pay plan for 2026-11" ]
     * }
     *
     * Response 422: { "error": "Payroll of 2026-11 is already APPROVED" }      (single employee mode)
     * Response 400: { "error": "Cannot generate a payroll for a future month" }
     */
    @PostMapping("/generate")
    @PreAuthorize(Authz.PAYROLL_MANAGE)
    public ResponseEntity<PayrollDto.GenerateResponse> generate(@RequestBody @Valid PayrollDto.GenerateRequest req,
                                                                @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.generate(req, actor));
    }

    /**
     * PUT /api/v1/payroll/8/deduction        (permission: PAYROLL_MANAGE)   - DRAFT payrolls only
     *
     * Request:  { "amount": 200.00, "note": "Advance taken on 2026-11-10" }
     * Response 200: the payroll with "deductionAmount": 200.00 and the new "netAmount"
     * Response 422: { "error": "Deduction exceeds the total earnings (7812.50)" }
     */
    @PutMapping("/{id}/deduction")
    @PreAuthorize(Authz.PAYROLL_MANAGE)
    public PayrollDto.Response setDeduction(@PathVariable Long id, @RequestBody @Valid PayrollDto.DeductionRequest req) {
        return payrollService.setDeduction(id, req);
    }

    /**
     * POST /api/v1/payroll/8/approve        (permission: PAYROLL_MANAGE)   - DRAFT -> APPROVED
     *
     * Request: no body
     * Response 200: the payroll with "status": "APPROVED"
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize(Authz.PAYROLL_MANAGE)
    public PayrollDto.Response approve(@PathVariable Long id) {
        return payrollService.approve(id);
    }

    /**
     * POST /api/v1/payroll/8/pay        (permission: PAYROLL_MANAGE)   - APPROVED -> PAID
     *
     * Request: no body
     * Response 200: the payroll with "status": "PAID" and "paidAt" filled
     * Response 422: { "error": "Only an APPROVED payroll can be paid (current status: DRAFT)" }
     */
    @PostMapping("/{id}/pay")
    @PreAuthorize(Authz.PAYROLL_MANAGE)
    public PayrollDto.Response pay(@PathVariable Long id) {
        return payrollService.pay(id);
    }

    /**
     * GET /api/v1/payroll?staffId=5&month=2026-11&status=APPROVED&page=0&size=20
     * (permission: PAYROLL_MANAGE or REPORTS_VIEW)
     *
     * Response 200: { "content": [ payroll, ... ], "page": 0, "size": 20, "totalElements": 12, "totalPages": 1 }
     */
    @GetMapping
    @PreAuthorize(Authz.PAYROLL_VIEW)
    public PageResponse<PayrollDto.Response> search(
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) Payroll.Status status,
            @PageableDefault(size = 20, sort = "periodMonth", direction = Sort.Direction.DESC) Pageable pageable) {
        return payrollService.search(staffId, month, status, pageable);
    }

    /**
     * GET /api/v1/payroll/me?month=2026-11        (any logged-in staff)
     * The employee sees his own payroll once it is APPROVED or PAID.
     *
     * Response 200: the payroll (same shape as above)
     * Response 404: { "error": "Your payroll for 2026-11 is not ready yet" }
     */
    @GetMapping("/me")
    @PreAuthorize(Authz.AUTHENTICATED)
    public PayrollDto.Response mine(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                                    @AuthenticationPrincipal AuthenticatedUser actor) {
        return payrollService.mine(actor.staffId(), month);
    }

    /**
     * GET /api/v1/payroll/8        (permission: PAYROLL_MANAGE or REPORTS_VIEW)
     *
     * Response 200: one payroll (same shape as above)
     * Response 404: { "error": "Payroll 8 not found" }
     */
    @GetMapping("/{id}")
    @PreAuthorize(Authz.PAYROLL_VIEW)
    public PayrollDto.Response get(@PathVariable Long id) {
        return payrollService.get(id);
    }
}
