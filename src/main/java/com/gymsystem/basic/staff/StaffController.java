package com.gymsystem.basic.staff;

import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.config.security.Authz;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {
    private final StaffService staffService;

    /**
     * POST /api/v1/staff        (permission: STAFF_MANAGE)
     *
     * Request:
     * {
     *   "branchId": 1,
     *   "roleCode": "TRAINER",
     *   "fullName": "Ahmed Hassan",
     *   "email": "ahmed@gym.com",
     *   "phone": "01012345678",
     *   "password": "Strong@123",
     *   "hireDate": "2026-11-01"
     * }
     *
     * Response 201:
     * {
     *   "id": 5, "branchId": 1, "branchName": "Main Branch", "roleCode": "TRAINER", "roleName": "Trainer",
     *   "fullName": "Ahmed Hassan", "email": "ahmed@gym.com", "phone": "01012345678",
     *   "hireDate": "2026-11-01", "status": "ACTIVE", "createdAt": "2026-11-13T10:15:30"
     * }
     *
     * Response 409: { "error": "Email is already registered" }
     * Response 400: { "password": "Password must be 8-72 characters" }
     */
    @PostMapping
    @PreAuthorize(Authz.STAFF_MANAGE)
    public ResponseEntity<StaffDto.Response> create(@RequestBody @Valid StaffDto.CreateRequest req,
                                                    @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.create(req, actor));
    }

    /**
     * GET /api/v1/staff?q=ahm&roleCode=TRAINER&status=ACTIVE&branchId=1&page=0&size=20   (permission: STAFF_MANAGE)
     * "q" matches the beginning of the name, phone or email.
     *
     * Response 200:
     * {
     *   "content": [ { "id": 5, "branchId": 1, "branchName": "Main Branch", "roleCode": "TRAINER",
     *                  "roleName": "Trainer", "fullName": "Ahmed Hassan", "email": "ahmed@gym.com",
     *                  "phone": "01012345678", "hireDate": "2026-11-01", "status": "ACTIVE",
     *                  "createdAt": "2026-11-13T10:15:30" } ],
     *   "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
     * }
     */
    @GetMapping
    @PreAuthorize(Authz.STAFF_MANAGE)
    public PageResponse<StaffDto.Response> search(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) String roleCode,
                                                  @RequestParam(required = false) Staff.Status status,
                                                  @RequestParam(required = false) Long branchId,
                                                  @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
        return staffService.search(q, roleCode, status, branchId, pageable);
    }

    /**
     * GET /api/v1/staff/me        (any logged-in staff)
     *
     * Response 200: the logged-in employee (same shape as the create response)
     */
    @GetMapping("/me")
    @PreAuthorize(Authz.AUTHENTICATED)
    public StaffDto.Response me(@AuthenticationPrincipal AuthenticatedUser actor) {
        return staffService.get(actor.staffId());
    }

    /**
     * GET /api/v1/staff/5        (permission: STAFF_MANAGE)
     *
     * Response 200: one employee (same shape as the create response)
     * Response 404: { "error": "Staff member 5 not found" }
     */
    @GetMapping("/{id}")
    @PreAuthorize(Authz.STAFF_MANAGE)
    public StaffDto.Response get(@PathVariable Long id) {
        return staffService.get(id);
    }

    /**
     * PUT /api/v1/staff/5        (permission: STAFF_MANAGE)
     *
     * Request:
     * { "branchId": 1, "roleCode": "RECEPTION", "fullName": "Ahmed H. Hassan", "phone": "01012345678" }
     *
     * Response 200: the updated employee (same shape as the create response)
     * Response 403: { "error": "Only the owner can assign the OWNER role" }
     */
    @PutMapping("/{id}")
    @PreAuthorize(Authz.STAFF_MANAGE)
    public StaffDto.Response update(@PathVariable Long id, @RequestBody @Valid StaffDto.UpdateRequest req,
                                    @AuthenticationPrincipal AuthenticatedUser actor) {
        return staffService.update(id, req, actor);
    }

    /**
     * PATCH /api/v1/staff/5/status        (permission: STAFF_MANAGE)
     *
     * Request:  { "status": "SUSPENDED" }       (ACTIVE | SUSPENDED | TERMINATED)
     * Response 200: the updated employee (same shape as the create response)
     * Response 422: { "error": "You cannot deactivate your own account" }
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize(Authz.STAFF_MANAGE)
    public StaffDto.Response changeStatus(@PathVariable Long id, @RequestBody @Valid StaffDto.StatusRequest req,
                                          @AuthenticationPrincipal AuthenticatedUser actor) {
        return staffService.changeStatus(id, req.status(), actor);
    }

    /**
     * PUT /api/v1/staff/5/password        (permission: STAFF_MANAGE)   - the owner resets a forgotten password
     *
     * Request:  { "newPassword": "NewStrong@123" }
     * Response 204: (empty)
     */
    @PutMapping("/{id}/password")
    @PreAuthorize(Authz.STAFF_MANAGE)
    public ResponseEntity<Void> resetPassword(@PathVariable Long id,
                                              @RequestBody @Valid StaffDto.ResetPasswordRequest req,
                                              @AuthenticationPrincipal AuthenticatedUser actor) {
        staffService.resetPassword(id, req.newPassword(), actor);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/staff/5/permissions        (permission: STAFF_MANAGE)
     *
     * Response 200:
     * {
     *   "staffId": 5, "roleCode": "TRAINER",
     *   "rolePermissions": ["ATTENDANCE_RECORD", "SUBSCRIPTION_SELL"],
     *   "extraPermissions": ["MEMBER_MANAGE"],
     *   "effectivePermissions": ["ATTENDANCE_RECORD", "MEMBER_MANAGE", "SUBSCRIPTION_SELL"]
     * }
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize(Authz.STAFF_MANAGE)
    public StaffDto.PermissionsResponse getPermissions(@PathVariable Long id) {
        return staffService.getPermissions(id);
    }

    /**
     * PUT /api/v1/staff/5/permissions        (permission: PERMISSION_GRANT)
     * Replaces the EXTRA permissions of this employee (what his role already gives is untouched).
     * The employee gets them in his next access token (next login / refresh).
     *
     * Request:  { "permissionCodes": ["MEMBER_MANAGE", "PAYMENT_RECORD"] }
     * Response 200: same shape as GET /permissions
     * Response 400: { "error": "One or more permission codes do not exist" }
     */
    @PutMapping("/{id}/permissions")
    @PreAuthorize(Authz.PERMISSION_GRANT)
    public StaffDto.PermissionsResponse setExtraPermissions(@PathVariable Long id,
                                                            @RequestBody @Valid StaffDto.PermissionsRequest req,
                                                            @AuthenticationPrincipal AuthenticatedUser actor) {
        return staffService.setExtraPermissions(id, req.permissionCodes(), actor);
    }

    /**
     * GET /api/v1/staff/5/qr        (permission: STAFF_MANAGE)
     * The app turns "qrToken" into a QR image that the employee scans to clock in / out.
     *
     * Response 200: { "staffId": 5, "qrToken": "0b6f7c1e-3c52-4f6e-9d57-6f0a5f6a1c11" }
     */
    @GetMapping("/{id}/qr")
    @PreAuthorize(Authz.STAFF_MANAGE)
    public StaffDto.QrResponse getQr(@PathVariable Long id) {
        return staffService.getQr(id);
    }

    /**
     * POST /api/v1/staff/5/qr/regenerate        (permission: STAFF_MANAGE)   - when a card is lost
     *
     * Response 200: { "staffId": 5, "qrToken": "a new uuid" }
     */
    @PostMapping("/{id}/qr/regenerate")
    @PreAuthorize(Authz.STAFF_MANAGE)
    public StaffDto.QrResponse regenerateQr(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser actor) {
        return staffService.regenerateQr(id, actor);
    }
}
