package com.gymsystem.basic.payroll;

import com.gymsystem.basic.common.PageResponse;
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

/** Manual bonuses (the owner gives a bonus to any worker). Sale bonuses are created by SubscriptionService. */
@Service
@RequiredArgsConstructor
@Transactional
public class BonusService {
    private final StaffBonusRepository bonusRepository;
    private final StaffRepository staffRepository;

    public BonusDto.Response grant(BonusDto.GrantRequest req, AuthenticatedUser actor) {
        StaffBonus.ReasonType reason = req.reasonType() != null ? req.reasonType() : StaffBonus.ReasonType.OWNER_DISCRETIONARY;
        if (reason == StaffBonus.ReasonType.SUBSCRIPTION_SALE) {
            throw new FalseInputData("Sale bonuses are created automatically when a subscription is sold");
        }
        Staff staff = staffRepository.findById(req.staffId())
                .orElseThrow(() -> new FalseInputData("Employee " + req.staffId() + " does not exist"));
        if (staff.getStatus() != Staff.Status.ACTIVE) {
            throw new BusinessRuleException("The employee is not active");
        }
        StaffBonus b = new StaffBonus();
        b.setStaff(staff);
        b.setAmount(req.amount());
        b.setReasonType(reason);
        b.setGrantedBy(staffRepository.getReferenceById(actor.staffId()));
        b.setNote(req.note());
        return BonusDto.Response.from(bonusRepository.save(b));
    }

    /** Removes a bonus that was not paid yet (granted by mistake). */
    public void delete(Long id) {
        StaffBonus b = bonusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bonus " + id + " not found"));
        if (b.getPayroll() != null) {
            throw new BusinessRuleException("This bonus is already included in a payroll");
        }
        bonusRepository.delete(b);
    }

    @Transactional(readOnly = true)
    public PageResponse<BonusDto.Response> search(Long staffId, StaffBonus.ReasonType reason, Long payrollId,
                                                  boolean unassignedOnly, LocalDate from, LocalDate to,
                                                  Pageable pageable) {
        LocalDateTime start = from == null ? null : from.atStartOfDay();
        LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();
        return PageResponse.of(bonusRepository.search(staffId, reason, payrollId, unassignedOnly, start, end, pageable)
                .map(BonusDto.Response::from));
    }
}
