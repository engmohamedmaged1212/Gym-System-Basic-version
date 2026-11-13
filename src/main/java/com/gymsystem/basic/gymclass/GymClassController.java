package com.gymsystem.basic.gymclass;

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
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class GymClassController {
    private final GymClassService classService;

    /**
     * POST /api/v1/classes        (permission: PLAN_MANAGE)
     *
     * Request:
     * { "branchId": 1, "trainerId": 5, "name": "Boxing", "description": "Beginner boxing, gloves provided" }
     *
     * Response 201:
     * {
     *   "id": 4, "branchId": 1, "branchName": "Main Branch", "trainerId": 5, "trainerName": "Ahmed Hassan",
     *   "name": "Boxing", "description": "Beginner boxing, gloves provided", "status": "ACTIVE",
     *   "discontinuedAt": null, "createdAt": "2026-11-13T11:00:00"
     * }
     *
     * Response 400: { "error": "The selected employee is not an active trainer" }
     */
    @PostMapping
    @PreAuthorize(Authz.PLAN_MANAGE)
    public ResponseEntity<GymClassDto.Response> create(@RequestBody @Valid GymClassDto.Request req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classService.create(req));
    }

    /**
     * GET /api/v1/classes?q=box&branchId=1&trainerId=5&status=ACTIVE&page=0&size=20     (any logged-in staff)
     *
     * Response 200: { "content": [ class, ... ], "page": 0, "size": 20, "totalElements": 3, "totalPages": 1 }
     */
    @GetMapping
    @PreAuthorize(Authz.AUTHENTICATED)
    public PageResponse<GymClassDto.Response> search(@RequestParam(required = false) String q,
                                                     @RequestParam(required = false) Long branchId,
                                                     @RequestParam(required = false) Long trainerId,
                                                     @RequestParam(required = false) GymClass.Status status,
                                                     @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return classService.search(q, branchId, trainerId, status, pageable);
    }

    /**
     * GET /api/v1/classes/4        (any logged-in staff)
     *
     * Response 200: one class (same shape as the create response)
     * Response 404: { "error": "Class 4 not found" }
     */
    @GetMapping("/{id}")
    @PreAuthorize(Authz.AUTHENTICATED)
    public GymClassDto.Response get(@PathVariable Long id) {
        return classService.get(id);
    }

    /**
     * PUT /api/v1/classes/4        (permission: PLAN_MANAGE)
     *
     * Request:
     * { "branchId": 1, "trainerId": 6, "name": "Boxing Pro", "description": "Advanced group" }
     *
     * Response 200: the updated class (same shape as the create response)
     * Response 422: { "error": "A discontinued class cannot be edited" }
     */
    @PutMapping("/{id}")
    @PreAuthorize(Authz.PLAN_MANAGE)
    public GymClassDto.Response update(@PathVariable Long id, @RequestBody @Valid GymClassDto.Request req) {
        return classService.update(id, req);
    }

    /**
     * POST /api/v1/classes/4/discontinue        (permission: PLAN_MANAGE)
     * The class has no schedule, it exists until the owner stops it.
     *
     * Request: no body
     * Response 200: the class with "status": "DISCONTINUED" and "discontinuedAt" filled
     */
    @PostMapping("/{id}/discontinue")
    @PreAuthorize(Authz.PLAN_MANAGE)
    public GymClassDto.Response discontinue(@PathVariable Long id) {
        return classService.discontinue(id);
    }
}
