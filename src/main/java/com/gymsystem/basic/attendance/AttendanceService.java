package com.gymsystem.basic.attendance;

import com.gymsystem.basic.branch.Branch;
import com.gymsystem.basic.branch.BranchRepository;
import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.common.ScanMethod;
import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.exception.BusinessRuleException;
import com.gymsystem.basic.exception.FalseInputData;
import com.gymsystem.basic.exception.ResourceNotFoundException;
import com.gymsystem.basic.member.Member;
import com.gymsystem.basic.member.MemberRepository;
import com.gymsystem.basic.staff.StaffRepository;
import com.gymsystem.basic.subscription.MemberSubscription;
import com.gymsystem.basic.subscription.MemberSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {
    /** a second scan of the same member within this time is treated as a mistake */
    private static final Duration DOUBLE_SCAN_WINDOW = Duration.ofMinutes(2);

    private final MemberAttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final BranchRepository branchRepository;
    private final StaffRepository staffRepository;

    /** The member scans his own QR at the gate / reception. */
    public AttendanceDto.ScanResponse scan(AttendanceDto.ScanRequest req, AuthenticatedUser actor) {
        Member member = memberRepository.findByQrToken(req.qrToken().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Unknown member QR code"));
        return checkIn(member, req.branchId() != null ? req.branchId() : actor.branchId(),
                ScanMethod.QR, LocalDateTime.now(), actor);
    }

    /** The receptionist registers a member by hand (forgot his card). */
    public AttendanceDto.ScanResponse manual(AttendanceDto.ManualRequest req, AuthenticatedUser actor) {
        Member member = memberRepository.findById(req.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member " + req.memberId() + " not found"));
        LocalDateTime at = req.checkinAt() != null ? req.checkinAt() : LocalDateTime.now();
        if (at.isAfter(LocalDateTime.now().plusMinutes(1))) {
            throw new FalseInputData("checkinAt cannot be in the future");
        }
        return checkIn(member, req.branchId() != null ? req.branchId() : actor.branchId(),
                ScanMethod.MANUAL, at, actor);
    }

    public AttendanceDto.Response checkOut(Long id) {
        MemberAttendance a = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record " + id + " not found"));
        if (a.getCheckoutAt() != null) {
            throw new BusinessRuleException("This visit is already checked out");
        }
        a.setCheckoutAt(LocalDateTime.now());
        return AttendanceDto.Response.from(a);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceDto.Response> search(Long memberId, Long branchId, LocalDate from, LocalDate to,
                                                       Pageable pageable) {
        LocalDateTime start = from == null ? null : from.atStartOfDay();
        LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();
        return PageResponse.of(attendanceRepository.search(memberId, branchId, start, end, pageable)
                .map(AttendanceDto.Response::from));
    }

    @Transactional(readOnly = true)
    public AttendanceDto.CountResponse count(Long branchId, LocalDate date) {
        long n = attendanceRepository.countInRange(branchId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        return new AttendanceDto.CountResponse(branchId, date, n);
    }

    // ------------------------------------------------------------ core rule
    private AttendanceDto.ScanResponse checkIn(Member member, Long branchId, ScanMethod method,
                                               LocalDateTime at, AuthenticatedUser actor) {
        if (member.getStatus() != Member.Status.ACTIVE) {
            throw new BusinessRuleException("Member is " + member.getStatus() + " and cannot enter");
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new FalseInputData("Branch " + branchId + " does not exist"));

        // any ACTIVE, not expired subscription that is valid for this branch lets the member in
        LocalDate day = at.toLocalDate();
        List<MemberSubscription> valid = subscriptionRepository
                .findValidForEntry(member.getId(), MemberSubscription.Status.ACTIVE, day, branchId);
        if (valid.isEmpty()) {
            throw new BusinessRuleException("No active subscription valid for this branch");
        }

        attendanceRepository.findFirstByMemberIdOrderByCheckinAtDesc(member.getId())
                .filter(last -> Duration.between(last.getCheckinAt(), at).abs().compareTo(DOUBLE_SCAN_WINDOW) < 0)
                .ifPresent(last -> {
                    throw new BusinessRuleException("This member was checked in a moment ago");
                });

        MemberAttendance a = new MemberAttendance();
        a.setMember(member);
        a.setBranch(branch);
        a.setCheckinAt(at);
        a.setMethod(method);
        a.setRecordedBy(staffRepository.getReferenceById(actor.staffId()));
        attendanceRepository.save(a);

        LocalDate endsOn = valid.stream().map(MemberSubscription::getEndDate)
                .max(Comparator.naturalOrder()).orElse(day);
        return new AttendanceDto.ScanResponse(AttendanceDto.Response.from(a), endsOn,
                ChronoUnit.DAYS.between(day, endsOn));
    }
}
