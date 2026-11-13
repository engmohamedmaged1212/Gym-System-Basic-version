package com.gymsystem.basic.kpi;

import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.config.security.Authz;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/kpis")
@RequiredArgsConstructor
public class KpiController {
    private final KpiService kpiService;

    /**
     * GET /api/v1/kpis/definitions        (any logged-in staff)
     *
     * Response 200:
     * [
     *   { "id": 1, "code": "HOURS_WORKED", "name": "Hours worked", "description": "From closed staff shifts",
     *     "unit": "hours", "source": "AUTO", "active": true },
     *   { "id": 5, "code": "MANAGER_RATING", "name": "Manager rating", "description": "Monthly rating entered by the owner/manager",
     *     "unit": "score", "source": "MANUAL", "active": true }
     * ]
     */
    @GetMapping("/definitions")
    @PreAuthorize(Authz.AUTHENTICATED)
    public List<KpiDto.DefinitionResponse> definitions() {
        return kpiService.listDefinitions();
    }

    /**
     * POST /api/v1/kpis/definitions        (permission: KPI_MANAGE)   - a new MANUAL KPI
     *
     * Request:
     * { "code": "CLIENT_FEEDBACK", "name": "Client feedback", "description": "Average member rating", "unit": "score" }
     *
     * Response 201:
     * { "id": 6, "code": "CLIENT_FEEDBACK", "name": "Client feedback", "description": "Average member rating",
     *   "unit": "score", "source": "MANUAL", "active": true }
     *
     * Response 409: { "error": "A KPI with this code already exists" }
     */
    @PostMapping("/definitions")
    @PreAuthorize(Authz.KPI_MANAGE)
    public ResponseEntity<KpiDto.DefinitionResponse> createDefinition(@RequestBody @Valid KpiDto.DefinitionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kpiService.createDefinition(req));
    }

    /**
     * PATCH /api/v1/kpis/definitions/6/active        (permission: KPI_MANAGE)
     *
     * Request:  { "active": false }
     * Response 200: the KPI definition (same shape as above)
     */
    @PatchMapping("/definitions/{id}/active")
    @PreAuthorize(Authz.KPI_MANAGE)
    public KpiDto.DefinitionResponse setDefinitionActive(@PathVariable Short id,
                                                         @RequestBody @Valid KpiDto.ActiveRequest req) {
        return kpiService.setDefinitionActive(id, req.active());
    }

    /**
     * PUT /api/v1/kpis/targets        (permission: KPI_MANAGE)   - creates or replaces the monthly target
     *
     * Request:
     * { "kpiId": 2, "staffId": 5, "month": "2026-11", "targetValue": 20 }
     *
     * Response 200:
     * { "id": 14, "kpiId": 2, "kpiCode": "SUBSCRIPTIONS_SOLD", "staffId": 5, "month": "2026-11", "targetValue": 20.00 }
     */
    @PutMapping("/targets")
    @PreAuthorize(Authz.KPI_MANAGE)
    public KpiDto.TargetResponse setTarget(@RequestBody @Valid KpiDto.TargetRequest req,
                                           @AuthenticationPrincipal AuthenticatedUser actor) {
        return kpiService.setTarget(req, actor);
    }

    /**
     * PUT /api/v1/kpis/results/manual        (permission: KPI_MANAGE)   - the value of a MANUAL KPI
     *
     * Request:
     * { "kpiId": 5, "staffId": 5, "month": "2026-11", "actualValue": 8.5 }
     *
     * Response 200:
     * { "id": 77, "kpiId": 5, "kpiCode": "MANAGER_RATING", "staffId": 5, "month": "2026-11",
     *   "actualValue": 8.50, "computedAt": "2026-12-01T10:00:00" }
     *
     * Response 422: { "error": "This KPI is calculated automatically, use /compute" }
     */
    @PutMapping("/results/manual")
    @PreAuthorize(Authz.KPI_MANAGE)
    public KpiDto.ResultResponse setManualResult(@RequestBody @Valid KpiDto.ManualResultRequest req) {
        return kpiService.setManualResult(req);
    }

    /**
     * POST /api/v1/kpis/compute        (permission: KPI_MANAGE)
     * Calculates the AUTO KPIs of the month (hours, subscriptions sold, sales amount, check-ins handled).
     * Safe to call many times: the results of the month are replaced.
     *
     * Request:
     * { "month": "2026-11", "staffId": null }       (staffId optional = every active employee)
     *
     * Response 200:  { "month": "2026-11", "employees": 8, "resultsWritten": 32 }
     */
    @PostMapping("/compute")
    @PreAuthorize(Authz.KPI_MANAGE)
    public KpiDto.ComputeResponse compute(@RequestBody @Valid KpiDto.ComputeRequest req) {
        return kpiService.compute(req);
    }

    /**
     * GET /api/v1/kpis/report?staffId=5&month=2026-11        (permission: KPI_MANAGE or REPORTS_VIEW)
     *
     * Response 200:
     * {
     *   "staffId": 5, "staffName": "Ahmed Hassan", "month": "2026-11",
     *   "lines": [
     *     { "kpiId": 2, "kpiCode": "SUBSCRIPTIONS_SOLD", "kpiName": "Subscriptions sold", "unit": "count",
     *       "source": "AUTO", "target": 20.00, "actual": 14.00, "achievementPercent": 70.00 },
     *     { "kpiId": 5, "kpiCode": "MANAGER_RATING", "kpiName": "Manager rating", "unit": "score",
     *       "source": "MANUAL", "target": null, "actual": 8.50, "achievementPercent": null }
     *   ]
     * }
     */
    @GetMapping("/report")
    @PreAuthorize(Authz.KPI_VIEW)
    public KpiDto.ReportResponse report(@RequestParam Long staffId,
                                        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return kpiService.report(staffId, month);
    }

    /**
     * GET /api/v1/kpis/report/me?month=2026-11        (any logged-in staff)   - the employee sees his own KPIs
     *
     * Response 200: same shape as /report
     */
    @GetMapping("/report/me")
    @PreAuthorize(Authz.AUTHENTICATED)
    public KpiDto.ReportResponse myReport(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                                          @AuthenticationPrincipal AuthenticatedUser actor) {
        return kpiService.report(actor.staffId(), month);
    }
}
