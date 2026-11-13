package com.gymsystem.basic.shift;

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
import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/shifts")
@RequiredArgsConstructor
public class ShiftController {
    private final ShiftService shiftService;

    /**
     * POST /api/v1/shifts/clock-in        (any logged-in staff)   - the employee starts his own shift
     *
     * Request (optional body):  { "branchId": 1 }     (default: the employee's own branch)
     *
     * Response 201:
     * {
     *   "id": 120, "staffId": 5, "staffName": "Ahmed Hassan", "branchId": 1, "branchName": "Main Branch",
     *   "clockInAt": "2026-11-13T08:00:12", "clockOutAt": null, "clockInMethod": "MANUAL",
     *   "clockOutMethod": null, "minutesWorked": null, "editedById": null, "note": null
     * }
     *
     * Response 422: { "error": "Shift already open since 2026-11-13T08:00:12" }
     */
    @PostMapping("/clock-in")
    @PreAuthorize(Authz.AUTHENTICATED)
    public ResponseEntity<ShiftDto.Response> clockIn(@RequestBody(required = false) ShiftDto.ClockRequest req,
                                                     @AuthenticationPrincipal AuthenticatedUser actor) {
        Long branchId = req == null ? null : req.branchId();
        return ResponseEntity.status(HttpStatus.CREATED).body(shiftService.clockIn(actor, branchId));
    }

    /**
     * POST /api/v1/shifts/clock-out        (any logged-in staff)   - the employee ends his own shift
     *
     * Request: no body
     * Response 200: the closed shift, e.g. "clockOutAt": "2026-11-13T16:05:40", "minutesWorked": 485
     * Response 422: { "error": "There is no open shift to close" }
     */
    @PostMapping("/clock-out")
    @PreAuthorize(Authz.AUTHENTICATED)
    public ShiftDto.Response clockOut(@AuthenticationPrincipal AuthenticatedUser actor) {
        return shiftService.clockOut(actor);
    }

    /**
     * POST /api/v1/shifts/scan        (permission: ATTENDANCE_RECORD)
     * The reception scans the employee QR: no open shift -> clock in, open shift -> clock out.
     *
     * Request:
     * { "qrToken": "0b6f7c1e-3c52-4f6e-9d57-6f0a5f6a1c11", "branchId": 1 }
     *
     * Response 200:
     * { "action": "CLOCKED_IN", "shift": { "id": 121, "staffId": 5, "staffName": "Ahmed Hassan", ... } }
     *
     * Response 404: { "error": "Unknown staff QR code" }
     */
    @PostMapping("/scan")
    @PreAuthorize(Authz.ATTENDANCE_RECORD)
    public ShiftDto.ScanResponse scan(@RequestBody @Valid ShiftDto.ScanRequest req,
                                      @AuthenticationPrincipal AuthenticatedUser actor) {
        return shiftService.scan(req.qrToken(), req.branchId(), actor);
    }

    /**
     * GET /api/v1/shifts/me/current        (any logged-in staff)
     *
     * Response 200: the open shift (same shape as clock-in)
     * Response 404: { "error": "No open shift" }
     */
    @GetMapping("/me/current")
    @PreAuthorize(Authz.AUTHENTICATED)
    public ShiftDto.Response current(@AuthenticationPrincipal AuthenticatedUser actor) {
        return shiftService.current(actor.staffId());
    }

    /**
     * GET /api/v1/shifts/me?from=2026-11-01&to=2026-11-30&page=0&size=20        (any logged-in staff)
     *
     * Response 200: { "content": [ shift, shift, ... ], "page": 0, "size": 20, "totalElements": 12, "totalPages": 1 }
     */
    @GetMapping("/me")
    @PreAuthorize(Authz.AUTHENTICATED)
    public PageResponse<ShiftDto.Response> mine(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "clockInAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return shiftService.search(actor.staffId(), null, from, to, pageable);
    }

    /**
     * GET /api/v1/shifts/me/summary?month=2026-11        (any logged-in staff)
     *
     * Response 200:
     * { "staffId": 5, "month": "2026-11", "totalMinutes": 7350, "totalHours": 122.50, "shifts": 16 }
     */
    @GetMapping("/me/summary")
    @PreAuthorize(Authz.AUTHENTICATED)
    public ShiftDto.HoursSummary mySummary(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                                           @AuthenticationPrincipal AuthenticatedUser actor) {
        return shiftService.summary(actor.staffId(), month);
    }

    /**
     * GET /api/v1/shifts?staffId=5&branchId=1&from=2026-11-01&to=2026-11-30&page=0&size=20
     * (permission: SHIFT_EDIT or REPORTS_VIEW)
     *
     * Response 200: { "content": [ shift, ... ], "page": 0, "size": 20, "totalElements": 40, "totalPages": 2 }
     */
    @GetMapping
    @PreAuthorize(Authz.SHIFT_VIEW)
    public PageResponse<ShiftDto.Response> search(
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "clockInAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return shiftService.search(staffId, branchId, from, to, pageable);
    }

    /**
     * GET /api/v1/shifts/summary?staffId=5&month=2026-11        (permission: SHIFT_EDIT or REPORTS_VIEW)
     *
     * Response 200:
     * { "staffId": 5, "month": "2026-11", "totalMinutes": 7350, "totalHours": 122.50, "shifts": 16 }
     */
    @GetMapping("/summary")
    @PreAuthorize(Authz.SHIFT_VIEW)
    public ShiftDto.HoursSummary summary(@RequestParam Long staffId,
                                         @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return shiftService.summary(staffId, month);
    }

    /**
     * PUT /api/v1/shifts/121        (permission: SHIFT_EDIT)   - manager corrects a forgotten clock-out
     *
     * Request:
     * {
     *   "clockInAt": "2026-11-13T08:00:00",
     *   "clockOutAt": "2026-11-13T16:00:00",
     *   "branchId": 1,
     *   "note": "Forgot to clock out"
     * }
     *
     * Response 200: the corrected shift (editedById = the manager)
     * Response 400: { "error": "clockOutAt must be after clockInAt" }
     */
    @PutMapping("/{id}")
    @PreAuthorize(Authz.SHIFT_EDIT)
    public ShiftDto.Response edit(@PathVariable Long id, @RequestBody @Valid ShiftDto.EditRequest req,
                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        return shiftService.edit(id, req, actor);
    }
}
