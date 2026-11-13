package com.gymsystem.basic.kpi;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

public final class KpiDto {
    private KpiDto() {
    }

    // ------------------------------------------------------------ definitions
    /** Custom KPIs are always MANUAL (the owner types the result). AUTO ones are built in. */
    public record DefinitionRequest(
            @NotBlank(message = "Code is required")
            @Pattern(regexp = "^[A-Z0-9_]{2,50}$", message = "Code must be 2-50 characters: A-Z, 0-9 and _")
            String code,

            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must be at most 100 characters")
            String name,

            @Size(max = 255, message = "Description must be at most 255 characters")
            String description,

            @NotBlank(message = "Unit is required")
            @Size(max = 30, message = "Unit must be at most 30 characters")
            String unit) {
    }

    public record ActiveRequest(
            @NotNull(message = "active is required") Boolean active) {
    }

    public record DefinitionResponse(Short id, String code, String name, String description, String unit,
                                     KpiDefinition.Source source, boolean active) {
        public static DefinitionResponse from(KpiDefinition d) {
            return new DefinitionResponse(d.getId(), d.getCode(), d.getName(), d.getDescription(), d.getUnit(),
                    d.getSource(), d.isActive());
        }
    }

    // ------------------------------------------------------------ targets and results
    public record TargetRequest(
            @NotNull(message = "kpiId is required") Short kpiId,
            @NotNull(message = "staffId is required") Long staffId,
            @NotNull(message = "month is required (format yyyy-MM)") YearMonth month,

            @NotNull(message = "Target value is required")
            @DecimalMin(value = "0.00", message = "Target cannot be negative")
            @Digits(integer = 10, fraction = 2, message = "Target must have at most 2 decimals")
            BigDecimal targetValue) {
    }

    public record TargetResponse(Long id, Short kpiId, String kpiCode, Long staffId, YearMonth month,
                                 BigDecimal targetValue) {
        public static TargetResponse from(KpiTarget t) {
            return new TargetResponse(t.getId(), t.getKpi().getId(), t.getKpi().getCode(), t.getStaff().getId(),
                    YearMonth.from(t.getPeriodMonth()), t.getTargetValue());
        }
    }

    public record ManualResultRequest(
            @NotNull(message = "kpiId is required") Short kpiId,
            @NotNull(message = "staffId is required") Long staffId,
            @NotNull(message = "month is required (format yyyy-MM)") YearMonth month,

            @NotNull(message = "Actual value is required")
            @DecimalMin(value = "0.00", message = "Value cannot be negative")
            @Digits(integer = 10, fraction = 2, message = "Value must have at most 2 decimals")
            BigDecimal actualValue) {
    }

    public record ResultResponse(Long id, Short kpiId, String kpiCode, Long staffId, YearMonth month,
                                 BigDecimal actualValue, LocalDateTime computedAt) {
        public static ResultResponse from(KpiResult r) {
            return new ResultResponse(r.getId(), r.getKpi().getId(), r.getKpi().getCode(), r.getStaff().getId(),
                    YearMonth.from(r.getPeriodMonth()), r.getActualValue(), r.getComputedAt());
        }
    }

    // ------------------------------------------------------------ compute and report
    public record ComputeRequest(
            @NotNull(message = "month is required (format yyyy-MM)") YearMonth month,
            /** optional - empty means every active employee */
            Long staffId) {
    }

    public record ComputeResponse(YearMonth month, int employees, int resultsWritten) {
    }

    /** achievementPercent = actual / target x 100 (null when there is no target or no result yet) */
    public record ReportLine(Short kpiId, String kpiCode, String kpiName, String unit, KpiDefinition.Source source,
                             BigDecimal target, BigDecimal actual, BigDecimal achievementPercent) {
    }

    public record ReportResponse(Long staffId, String staffName, YearMonth month, List<ReportLine> lines) {
    }
}
