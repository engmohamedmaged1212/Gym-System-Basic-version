package com.gymsystem.basic.attendance;

import com.gymsystem.basic.common.ScanMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AttendanceDto {
    private AttendanceDto() {
    }

    public record ScanRequest(
            @NotBlank(message = "qrToken is required") String qrToken,
            /** optional - default: the branch of the logged-in employee */
            Long branchId) {
    }

    public record ManualRequest(
            @NotNull(message = "memberId is required") Long memberId,
            Long branchId,
            /** optional - default: now */
            LocalDateTime checkinAt) {
    }

    public record Response(Long id, Long memberId, String memberName, Long branchId, String branchName,
                           LocalDateTime checkinAt, LocalDateTime checkoutAt, ScanMethod method,
                           Long recordedById) {
        public static Response from(MemberAttendance a) {
            return new Response(a.getId(), a.getMember().getId(), a.getMember().getFullName(),
                    a.getBranch().getId(), a.getBranch().getName(), a.getCheckinAt(), a.getCheckoutAt(),
                    a.getMethod(), a.getRecordedBy() == null ? null : a.getRecordedBy().getId());
        }
    }

    /** what the reception screen shows after a scan */
    public record ScanResponse(Response attendance, LocalDate subscriptionEndsOn, long daysLeft) {
    }

    public record CountResponse(Long branchId, LocalDate date, long checkIns) {
    }
}
