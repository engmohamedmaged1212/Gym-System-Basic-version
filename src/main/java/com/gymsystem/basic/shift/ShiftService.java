package com.gymsystem.basic.shift;

import com.gymsystem.basic.branch.Branch;
import com.gymsystem.basic.branch.BranchRepository;
import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.common.ScanMethod;
import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.exception.BusinessRuleException;
import com.gymsystem.basic.exception.FalseInputData;
import com.gymsystem.basic.exception.ResourceNotFoundException;
import com.gymsystem.basic.staff.Staff;
import com.gymsystem.basic.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional
public class ShiftService {
    private final StaffShiftRepository shiftRepository;
    private final StaffRepository staffRepository;
    private final BranchRepository branchRepository;

    // ------------------------------------------------------------ self service
    public ShiftDto.Response clockIn(AuthenticatedUser actor, Long branchId) {
        Staff staff = findStaff(actor.staffId());
        return ShiftDto.Response.from(open(staff, branchId != null ? branchId : actor.branchId(), ScanMethod.MANUAL));
    }

    public ShiftDto.Response clockOut(AuthenticatedUser actor) {
        return ShiftDto.Response.from(close(actor.staffId(), ScanMethod.MANUAL));
    }

    // ------------------------------------------------------------ QR station
    /** Scanning the staff QR toggles: no open shift -> clock in, open shift -> clock out. */
    public ShiftDto.ScanResponse scan(String qrToken, Long branchId, AuthenticatedUser actor) {
        Staff staff = staffRepository.findByQrToken(qrToken.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Unknown staff QR code"));
        boolean hasOpenShift = shiftRepository
                .findFirstByStaffIdAndClockOutAtIsNullOrderByClockInAtDesc(staff.getId()).isPresent();
        if (hasOpenShift) {
            return new ShiftDto.ScanResponse("CLOCKED_OUT", ShiftDto.Response.from(close(staff.getId(), ScanMethod.QR)));
        }
        Long branch = branchId != null ? branchId : actor.branchId();
        return new ShiftDto.ScanResponse("CLOCKED_IN", ShiftDto.Response.from(open(staff, branch, ScanMethod.QR)));
    }

    // ------------------------------------------------------------ manager correction
    public ShiftDto.Response edit(Long id, ShiftDto.EditRequest req, AuthenticatedUser actor) {
        StaffShift s = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift " + id + " not found"));
        if (req.clockOutAt() != null && req.clockOutAt().isBefore(req.clockInAt())) {
            throw new FalseInputData("clockOutAt must be after clockInAt");
        }
        s.setClockInAt(req.clockInAt());
        s.setClockOutAt(req.clockOutAt());
        if (req.clockOutAt() != null && s.getClockOutMethod() == null) {
            s.setClockOutMethod(ScanMethod.MANUAL);
        }
        if (req.clockOutAt() == null) {
            s.setClockOutMethod(null);
        }
        if (req.branchId() != null && !req.branchId().equals(s.getBranch().getId())) {
            s.setBranch(findBranch(req.branchId()));
        }
        s.setNote(req.note());
        s.setEditedBy(staffRepository.getReferenceById(actor.staffId()));
        return ShiftDto.Response.from(s);
    }

    // ------------------------------------------------------------ queries
    @Transactional(readOnly = true)
    public PageResponse<ShiftDto.Response> search(Long staffId, Long branchId, LocalDate from, LocalDate to,
                                                  Pageable pageable) {
        LocalDateTime start = from == null ? null : from.atStartOfDay();
        LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();
        return PageResponse.of(shiftRepository.search(staffId, branchId, start, end, pageable)
                .map(ShiftDto.Response::from));
    }

    @Transactional(readOnly = true)
    public ShiftDto.Response current(Long staffId) {
        return shiftRepository.findFirstByStaffIdAndClockOutAtIsNullOrderByClockInAtDesc(staffId)
                .map(ShiftDto.Response::from)
                .orElseThrow(() -> new ResourceNotFoundException("No open shift"));
    }

    @Transactional(readOnly = true)
    public ShiftDto.HoursSummary summary(Long staffId, YearMonth month) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        return ShiftDto.HoursSummary.of(staffId, month,
                shiftRepository.sumMinutes(staffId, from, to), shiftRepository.countClosed(staffId, from, to));
    }

    // ------------------------------------------------------------ helpers
    private StaffShift open(Staff staff, Long branchId, ScanMethod method) {
        if (staff.getStatus() != Staff.Status.ACTIVE) {
            throw new BusinessRuleException("This staff account is not active");
        }
        shiftRepository.findFirstByStaffIdAndClockOutAtIsNullOrderByClockInAtDesc(staff.getId())
                .ifPresent(open -> {
                    throw new BusinessRuleException("Shift already open since " + open.getClockInAt());
                });
        StaffShift s = new StaffShift();
        s.setStaff(staff);
        s.setBranch(findBranch(branchId));
        s.setClockInAt(LocalDateTime.now());
        s.setClockInMethod(method);
        return shiftRepository.save(s);
    }

    private StaffShift close(Long staffId, ScanMethod method) {
        StaffShift s = shiftRepository.findFirstByStaffIdAndClockOutAtIsNullOrderByClockInAtDesc(staffId)
                .orElseThrow(() -> new BusinessRuleException("There is no open shift to close"));
        s.setClockOutAt(LocalDateTime.now());
        s.setClockOutMethod(method);
        return s;
    }

    private Staff findStaff(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member " + id + " not found"));
    }

    private Branch findBranch(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new FalseInputData("Branch " + id + " does not exist"));
    }
}
