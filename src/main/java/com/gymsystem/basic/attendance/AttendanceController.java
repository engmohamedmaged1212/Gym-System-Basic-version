package com.gymsystem.basic.attendance;

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
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;

    /**
     * POST /api/v1/attendance/scan        (permission: ATTENDANCE_RECORD)
     * The member scans his QR code. He gets in only if he has an ACTIVE, not expired subscription
     * that is valid for this branch (a plan with allBranches = true is valid everywhere).
     *
     * Request:
     * { "qrToken": "6d0c1f0e-1b7e-4c7a-8d41-0d1f0b3a2c55", "branchId": 1 }
     *
     * Response 201:
     * {
     *   "attendance": { "id": 880, "memberId": 17, "memberName": "Omar Khaled", "branchId": 1,
     *                   "branchName": "Main Branch", "checkinAt": "2026-11-13T18:05:11",
     *                   "checkoutAt": null, "method": "QR", "recordedById": 3 },
     *   "subscriptionEndsOn": "2026-12-12",
     *   "daysLeft": 29
     * }
     *
     * Response 404: { "error": "Unknown member QR code" }
     * Response 422: { "error": "No active subscription valid for this branch" }
     * Response 422: { "error": "This member was checked in a moment ago" }
     */
    @PostMapping("/scan")
    @PreAuthorize(Authz.ATTENDANCE_RECORD)
    public ResponseEntity<AttendanceDto.ScanResponse> scan(@RequestBody @Valid AttendanceDto.ScanRequest req,
                                                           @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.scan(req, actor));
    }

    /**
     * POST /api/v1/attendance/manual        (permission: ATTENDANCE_RECORD)
     *
     * Request:
     * { "memberId": 17, "branchId": 1, "checkinAt": "2026-11-13T18:05:00" }     (branchId, checkinAt optional)
     *
     * Response 201: same shape as /scan, with "method": "MANUAL"
     * Response 400: { "error": "checkinAt cannot be in the future" }
     */
    @PostMapping("/manual")
    @PreAuthorize(Authz.ATTENDANCE_RECORD)
    public ResponseEntity<AttendanceDto.ScanResponse> manual(@RequestBody @Valid AttendanceDto.ManualRequest req,
                                                             @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.manual(req, actor));
    }

    /**
     * PATCH /api/v1/attendance/880/checkout        (permission: ATTENDANCE_RECORD)   - optional
     *
     * Request: no body
     * Response 200:
     * { "id": 880, "memberId": 17, "memberName": "Omar Khaled", "branchId": 1, "branchName": "Main Branch",
     *   "checkinAt": "2026-11-13T18:05:11", "checkoutAt": "2026-11-13T19:30:02", "method": "QR", "recordedById": 3 }
     *
     * Response 422: { "error": "This visit is already checked out" }
     */
    @PatchMapping("/{id}/checkout")
    @PreAuthorize(Authz.ATTENDANCE_RECORD)
    public AttendanceDto.Response checkOut(@PathVariable Long id) {
        return attendanceService.checkOut(id);
    }

    /**
     * GET /api/v1/attendance?memberId=17&branchId=1&from=2026-11-01&to=2026-11-30&page=0&size=20
     * (permission: ATTENDANCE_RECORD, MEMBER_MANAGE or REPORTS_VIEW)
     *
     * Response 200:
     * { "content": [ { "id": 880, "memberId": 17, "memberName": "Omar Khaled", ... } ],
     *   "page": 0, "size": 20, "totalElements": 14, "totalPages": 1 }
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ATTENDANCE_RECORD','MEMBER_MANAGE','REPORTS_VIEW')")
    public PageResponse<AttendanceDto.Response> search(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "checkinAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return attendanceService.search(memberId, branchId, from, to, pageable);
    }

    /**
     * GET /api/v1/attendance/count?branchId=1&date=2026-11-13        (permission: REPORTS_VIEW or ATTENDANCE_RECORD)
     * branchId is optional (all branches); date is optional (today).
     *
     * Response 200: { "branchId": 1, "date": "2026-11-13", "checkIns": 86 }
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyAuthority('REPORTS_VIEW','ATTENDANCE_RECORD')")
    public AttendanceDto.CountResponse count(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.count(branchId, date != null ? date : LocalDate.now());
    }
}
