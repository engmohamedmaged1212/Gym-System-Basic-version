package com.gymsystem.basic.offer;

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

import java.util.List;

@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
public class OfferController {
    private final OfferService offerService;

    /**
     * POST /api/v1/offers        (permission: OFFER_MANAGE)
     * A fixed price for ONE plan during a time window. Create one offer per plan.
     *
     * Request:
     * {
     *   "name": "Friday flash sale",
     *   "planId": 3,
     *   "branchId": null,
     *   "offerPrice": 1500.00,
     *   "saleBonusAmount": 100.00,
     *   "validFrom": "2026-11-13T00:00:00",
     *   "validTo": "2026-11-13T23:59:59"
     * }
     * (branchId null = every branch; saleBonusAmount optional = use the plan's bonus)
     *
     * Response 201:
     * {
     *   "id": 9, "name": "Friday flash sale", "planId": 3, "planName": "Half-year", "planPrice": 2000.00,
     *   "branchId": null, "branchName": null, "offerPrice": 1500.00, "saleBonusAmount": 100.00,
     *   "validFrom": "2026-11-13T00:00:00", "validTo": "2026-11-13T23:59:59", "active": true,
     *   "runningNow": true, "createdById": 1, "createdAt": "2026-11-13T09:00:00"
     * }
     *
     * Response 400: { "error": "Offer price must be lower than the regular plan price (2000.00)" }
     * Response 400: { "error": "validTo must be after validFrom" }
     */
    @PostMapping
    @PreAuthorize(Authz.OFFER_MANAGE)
    public ResponseEntity<OfferDto.Response> create(@RequestBody @Valid OfferDto.Request req,
                                                    @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(offerService.create(req, actor));
    }

    /**
     * GET /api/v1/offers?q=fri&planId=3&branchId=1&active=true&runningNow=true&page=0&size=20   (any logged-in staff)
     *
     * Response 200: { "content": [ offer, ... ], "page": 0, "size": 20, "totalElements": 2, "totalPages": 1 }
     */
    @GetMapping
    @PreAuthorize(Authz.AUTHENTICATED)
    public PageResponse<OfferDto.Response> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "false") boolean runningNow,
            @PageableDefault(size = 20, sort = "validFrom", direction = Sort.Direction.DESC) Pageable pageable) {
        return offerService.search(q, planId, branchId, active, runningNow, pageable);
    }

    /**
     * GET /api/v1/offers/applicable?planId=3&branchId=1        (any logged-in staff)
     * The offers the receptionist can use right now for this plan in this branch, cheapest first.
     *
     * Response 200: [ { "id": 9, "name": "Friday flash sale", "offerPrice": 1500.00, "runningNow": true, ... } ]
     */
    @GetMapping("/applicable")
    @PreAuthorize(Authz.AUTHENTICATED)
    public List<OfferDto.Response> applicable(@RequestParam Long planId, @RequestParam Long branchId) {
        return offerService.applicable(planId, branchId);
    }

    /**
     * GET /api/v1/offers/9        (any logged-in staff)
     *
     * Response 200: one offer (same shape as the create response)
     * Response 404: { "error": "Offer 9 not found" }
     */
    @GetMapping("/{id}")
    @PreAuthorize(Authz.AUTHENTICATED)
    public OfferDto.Response get(@PathVariable Long id) {
        return offerService.get(id);
    }

    /**
     * PUT /api/v1/offers/9        (permission: OFFER_MANAGE)
     *
     * Request:  same body as the create request
     * Response 200: the updated offer (same shape as the create response)
     */
    @PutMapping("/{id}")
    @PreAuthorize(Authz.OFFER_MANAGE)
    public OfferDto.Response update(@PathVariable Long id, @RequestBody @Valid OfferDto.Request req) {
        return offerService.update(id, req);
    }

    /**
     * PATCH /api/v1/offers/9/active        (permission: OFFER_MANAGE)   - stop an offer early
     *
     * Request:  { "active": false }
     * Response 200: the updated offer (same shape as the create response)
     */
    @PatchMapping("/{id}/active")
    @PreAuthorize(Authz.OFFER_MANAGE)
    public OfferDto.Response setActive(@PathVariable Long id, @RequestBody @Valid OfferDto.ActiveRequest req) {
        return offerService.setActive(id, req.active());
    }
}
