package com.gymsystem.basic.payroll;

import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.exception.FalseInputData;
import com.gymsystem.basic.exception.ResourceNotFoundException;
import com.gymsystem.basic.staff.Staff;
import com.gymsystem.basic.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PayPlanService {
    private final StaffPayPlanRepository payPlanRepository;
    private final StaffRepository staffRepository;

    /**
     * Starts a new pay plan. The previous open plan is closed the day before, so the history stays correct
     * and old payrolls are not affected.
     */
    public PayPlanDto.Response set(PayPlanDto.SetRequest req, AuthenticatedUser actor) {
        Staff staff = staffRepository.findById(req.staffId())
                .orElseThrow(() -> new FalseInputData("Employee " + req.staffId() + " does not exist"));
        BigDecimal hourly = orZero(req.hourlyRate());
        BigDecimal base = orZero(req.baseSalary());
        BigDecimal commission = orZero(req.saleCommissionPercent());
        if (hourly.signum() == 0 && base.signum() == 0 && commission.signum() == 0) {
            throw new FalseInputData("Set an hourly rate, a base salary or a commission");
        }

        payPlanRepository.findFirstByStaffIdAndEffectiveToIsNull(staff.getId()).ifPresent(current -> {
            if (!req.effectiveFrom().isAfter(current.getEffectiveFrom())) {
                throw new FalseInputData("The new plan must start after the current plan's start date ("
                        + current.getEffectiveFrom() + ")");
            }
            current.setEffectiveTo(req.effectiveFrom().minusDays(1));
        });

        StaffPayPlan plan = new StaffPayPlan();
        plan.setStaff(staff);
        plan.setHourlyRate(hourly);
        plan.setBaseSalary(base);
        plan.setSaleCommissionPercent(commission);
        plan.setEffectiveFrom(req.effectiveFrom());
        plan.setCreatedBy(staffRepository.getReferenceById(actor.staffId()));
        return PayPlanDto.Response.from(payPlanRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public List<PayPlanDto.Response> history(Long staffId) {
        return payPlanRepository.findByStaffIdOrderByEffectiveFromDesc(staffId).stream()
                .map(PayPlanDto.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PayPlanDto.Response current(Long staffId) {
        return payPlanRepository.findByStaffIdOrderByEffectiveFromDesc(staffId).stream()
                .filter(p -> p.getEffectiveTo() == null).findFirst()
                .map(PayPlanDto.Response::from)
                .orElseThrow(() -> new ResourceNotFoundException("No pay plan is set for this employee"));
    }

    private BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
