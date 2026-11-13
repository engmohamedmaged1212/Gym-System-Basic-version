package com.gymsystem.basic.shift;

import com.gymsystem.basic.common.ScanMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;

public final class ShiftDto {
    private ShiftDto() {
    }

    /** body is optional: send {} to use the branch of the logged-in employee */
    public record ClockRequest(Long branchId) {
    }

    public record ScanRequest(
            @NotBlank(message = "qrToken is required") String qrToken,
            Long branchId) {
    }

    public record EditRequest(
            @NotNull(message = "clockInAt is required") LocalDateTime clockInAt,
            LocalDateTime clockOutAt,
            Long branchId,
            @Size(max = 255, message = "Note must be at most 255 characters") String note) {
    }

    public record Response(Long id, Long staffId, String staffName, Long branchId, String branchName,
                           LocalDateTime clockInAt, LocalDateTime clockOutAt, ScanMethod clockInMethod,
                           ScanMethod clockOutMethod, Long minutesWorked, Long editedById, String note) {
        public static Response from(StaffShift s) {
            Long minutes = s.getClockOutAt() == null ? null
                    : Duration.between(s.getClockInAt(), s.getClockOutAt()).toMinutes();
            return new Response(s.getId(), s.getStaff().getId(), s.getStaff().getFullName(),
                    s.getBranch().getId(), s.getBranch().getName(), s.getClockInAt(), s.getClockOutAt(),
                    s.getClockInMethod(), s.getClockOutMethod(), minutes,
                    s.getEditedBy() == null ? null : s.getEditedBy().getId(), s.getNote());
        }
    }

    /** action is CLOCKED_IN or CLOCKED_OUT */
    public record ScanResponse(String action, Response shift) {
    }

    public record HoursSummary(Long staffId, YearMonth month, long totalMinutes, BigDecimal totalHours, long shifts) {
        public static HoursSummary of(Long staffId, YearMonth month, long minutes, long shifts) {
            BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            return new HoursSummary(staffId, month, minutes, hours, shifts);
        }
    }
}
