package com.gymsystem.basic.branch;

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
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {
    private final BranchService branchService;

    /**
     * POST /api/v1/branches        (permission: BRANCH_MANAGE)
     *
     * Request:
     * {
     *   "name": "Nasr City",
     *   "address": "12 Abbas El Akkad St, Cairo",
     *   "phone": "01012345678"
     * }
     *
     * Response 201:
     * {
     *   "id": 2, "name": "Nasr City", "address": "12 Abbas El Akkad St, Cairo",
     *   "phone": "01012345678", "active": true, "createdAt": "2026-11-13T10:15:30"
     * }
     */
    @PostMapping
    @PreAuthorize(Authz.BRANCH_MANAGE)
    public ResponseEntity<BranchDto.Response> create(@RequestBody @Valid BranchDto.Request req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branchService.create(req));
    }

    /**
     * GET /api/v1/branches?q=Nas&active=true&page=0&size=20     (any logged-in staff)
     *
     * Response 200:
     * {
     *   "content": [ { "id": 2, "name": "Nasr City", "address": "...", "phone": "01012345678",
     *                  "active": true, "createdAt": "2026-11-13T10:15:30" } ],
     *   "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
     * }
     */
    @GetMapping
    @PreAuthorize(Authz.AUTHENTICATED)
    public PageResponse<BranchDto.Response> search(@RequestParam(required = false) String q,
                                                   @RequestParam(required = false) Boolean active,
                                                   @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return branchService.search(q, active, pageable);
    }

    /**
     * GET /api/v1/branches/2     (any logged-in staff)
     *
     * Response 200:
     * { "id": 2, "name": "Nasr City", "address": "...", "phone": "01012345678",
     *   "active": true, "createdAt": "2026-11-13T10:15:30" }
     *
     * Response 404:  { "error": "Branch 2 not found" }
     */
    @GetMapping("/{id}")
    @PreAuthorize(Authz.AUTHENTICATED)
    public BranchDto.Response get(@PathVariable Long id) {
        return branchService.get(id);
    }

    /**
     * PUT /api/v1/branches/2        (permission: BRANCH_MANAGE)
     *
     * Request:
     * { "name": "Nasr City 2", "address": "New address", "phone": "01099999999" }
     *
     * Response 200: the updated branch (same shape as the create response)
     * Response 409: { "error": "A branch with this name already exists" }
     */
    @PutMapping("/{id}")
    @PreAuthorize(Authz.BRANCH_MANAGE)
    public BranchDto.Response update(@PathVariable Long id, @RequestBody @Valid BranchDto.Request req) {
        return branchService.update(id, req);
    }

    /**
     * PATCH /api/v1/branches/2/active        (permission: BRANCH_MANAGE)
     *
     * Request:  { "active": false }
     * Response 200: the updated branch (same shape as the create response)
     */
    @PatchMapping("/{id}/active")
    @PreAuthorize(Authz.BRANCH_MANAGE)
    public BranchDto.Response setActive(@PathVariable Long id, @RequestBody @Valid BranchDto.ActiveRequest req) {
        return branchService.setActive(id, req.active());
    }
}
