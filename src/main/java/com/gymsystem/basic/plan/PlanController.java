package com.gymsystem.basic.plan;

import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.config.security.Authz;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {
    private final PlanService planService;

    /**
     * POST /api/v1/plans        (permission: PLAN_MANAGE)
     * allBranches = true: the member may enter ANY branch of the gym (e.g. the 10,000 yearly plan).
     * allBranches = false: the member may enter only the branch where he subscribed.
     *
     * Request:
     * {
     *   "name": "Yearly - all branches",
     *   "description": "Unlimited gym access in every branch",
     *   "durationDays": 365,
     *   "price": 10000.00,
     *   "saleBonusAmount": 300.00,
     *   "includesGym": true,
     *   "allBranches": true,
     *   "classIds": [4, 7]
     * }
     *
     * Response 201:
     * {
     *   "id": 5, "name": "Yearly - all branches", "description": "Unlimited gym access in every branch",
     *   "durationDays": 365, "price": 10000.00, "saleBonusAmount": 300.00, "includesGym": true,
     *   "allBranches": true, "active": true,
     *   "classes": [ { "id": 4, "name": "Boxing" }, { "id": 7, "name": "Yoga" } ],
     *   "createdAt": "2026-11-13T11:30:00"
     * }
     *
     * Response 409: { "error": "A plan with this name already exists" }
     * Response 400: { "error": "A plan must include the gym or at least one class" }
     */
    @PostMapping
    @PreAuthorize(Authz.PLAN_MANAGE)
    public ResponseEntity<PlanDto.Response> create(@RequestBody @Valid PlanDto.Request req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.create(req));
    }

    /**
     * GET /api/v1/plans?q=month&active=true&page=0&size=20        (any logged-in staff)
     *
     * Response 200: { "content": [ plan, ... ], "page": 0, "size": 20, "totalElements": 4, "totalPages": 1 }
     */
    @GetMapping
    @PreAuthorize(Authz.AUTHENTICATED)
    public PageResponse<PlanDto.Response> search(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) Boolean active,
                                                 @PageableDefault(size = 20, sort = "durationDays") Pageable pageable) {
        return planService.search(q, active, pageable);
    }

    /**
     * GET /api/v1/plans/5        (any logged-in staff)
     *
     * Response 200: one plan (same shape as the create response)
     * Response 404: { "error": "Plan 5 not found" }
     */
    @GetMapping("/{id}")
    @PreAuthorize(Authz.AUTHENTICATED)
    public PlanDto.Response get(@PathVariable Long id) {
        return planService.get(id);
    }

    /**
     * PUT /api/v1/plans/5        (permission: PLAN_MANAGE)
     * A new price only affects future sales; sold subscriptions keep their price.
     *
     * Request:  same body as the create request
     * Response 200: the updated plan (same shape as the create response)
     */
    @PutMapping("/{id}")
    @PreAuthorize(Authz.PLAN_MANAGE)
    public PlanDto.Response update(@PathVariable Long id, @RequestBody @Valid PlanDto.Request req) {
        return planService.update(id, req);
    }

    /**
     * PATCH /api/v1/plans/5/active        (permission: PLAN_MANAGE)
     *
     * Request:  { "active": false }
     * Response 200: the updated plan (same shape as the create response)
     */
    @PatchMapping("/{id}/active")
    @PreAuthorize(Authz.PLAN_MANAGE)
    public PlanDto.Response setActive(@PathVariable Long id, @RequestBody @Valid PlanDto.ActiveRequest req) {
        return planService.setActive(id, req.active());
    }
}
