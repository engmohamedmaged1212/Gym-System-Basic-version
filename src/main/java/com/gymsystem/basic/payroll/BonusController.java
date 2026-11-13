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

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/bonuses")
@RequiredArgsConstructor
public class BonusController {
    private final BonusService bonusService;

    /**
     * POST /api/v1/bonuses        (permission: BONUS_GRANT)   - the owner gives a bonus to any worker
     *
     * Request:
     * { "staffId": 5, "amount": 250.00, "reasonType": "OWNER_DISCRETIONARY", "note": "Great month" }
     * (reasonType optional: OWNER_DISCRETIONARY | KPI | OTHER)
     *
     * Response 201:
     * {
     *   "id": 41, "staffId": 5, "staffName": "Ahmed Hassan", "amount": 250.00,
     *   "reasonType": "OWNER_DISCRETIONARY", "subscriptionId": null, "grantedById": 1,
     *   "grantedByName": "Owner", "grantedAt": "2026-11-13T14:00:00", "note": "Great month", "payrollId": null
     * }
     *
     * Response 400: { "error": "Sale bonuses are created automatically when a subscription is sold" }
     */
    @PostMapping
    @PreAuthorize(Authz.BONUS_GRANT)
    public ResponseEntity<BonusDto.Response> grant(@RequestBody @Valid BonusDto.GrantRequest req,
                                                   @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bonusService.grant(req, actor));
    }

    /**
     * GET /api/v1/bonuses?staffId=5&reasonType=SUBSCRIPTION_SALE&payrollId=8&unassignedOnly=true&from=2026-11-01&to=2026-11-30
     * (permission: BONUS_GRANT, PAYROLL_MANAGE or REPORTS_VIEW)
     *
     * Response 200: { "content": [ bonus, ... ], "page": 0, "size": 20, "totalElements": 6, "totalPages": 1 }
     * (a "SUBSCRIPTION_SALE" bonus has "subscriptionId" filled)
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('BONUS_GRANT','PAYROLL_MANAGE','REPORTS_VIEW')")
    public PageResponse<BonusDto.Response> search(
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) StaffBonus.ReasonType reasonType,
            @RequestParam(required = false) Long payrollId,
            @RequestParam(defaultValue = "false") boolean unassignedOnly,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "grantedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return bonusService.search(staffId, reasonType, payrollId, unassignedOnly, from, to, pageable);
    }

    /**
     * GET /api/v1/bonuses/me?from=2026-11-01&to=2026-11-30        (any logged-in staff)
     * The employee sees his own bonuses (including the sale bonuses he earned).
     *
     * Response 200: { "content": [ bonus, ... ], "page": 0, "size": 20, "totalElements": 3, "totalPages": 1 }
     */
    @GetMapping("/me")
    @PreAuthorize(Authz.AUTHENTICATED)
    public PageResponse<BonusDto.Response> mine(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "grantedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return bonusService.search(actor.staffId(), null, null, false, from, to, pageable);
    }

    /**
     * DELETE /api/v1/bonuses/41        (permission: BONUS_GRANT)   - only a bonus not yet included in a payroll
     *
     * Response 204: (empty)
     * Response 422: { "error": "This bonus is already included in a payroll" }
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(Authz.BONUS_GRANT)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bonusService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
