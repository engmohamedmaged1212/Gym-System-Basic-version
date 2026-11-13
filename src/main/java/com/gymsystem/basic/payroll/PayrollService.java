package com.gymsystem.basic.payroll;

import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.exception.BusinessRuleException;
import com.gymsystem.basic.exception.FalseInputData;
import com.gymsystem.basic.exception.ResourceNotFoundException;
import com.gymsystem.basic.shift.StaffShiftRepository;
import com.gymsystem.basic.staff.Staff;
import com.gymsystem.basic.staff.StaffRepository;
import com.gymsystem.basic.subscription.MemberSubscription;
import com.gymsystem.basic.subscription.MemberSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * net = hours x hourly rate + base salary + commission % of the subscriptions he sold
 *       + bonuses not paid yet - deductions
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PayrollService {
    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PayrollRepository payrollRepository;
    private final StaffPayPlanRepository payPlanRepository;
    private final StaffBonusRepository bonusRepository;
    private final StaffShiftRepository shiftRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final StaffRepository staffRepository;

    /** Creates (or recalculates, while still DRAFT) the payroll of one employee or of everybody. */
    public PayrollDto.GenerateResponse generate(PayrollDto.GenerateRequest req, AuthenticatedUser actor) {
        YearMonth month = req.month();
        if (month.isAfter(YearMonth.now())) {
            throw new FalseInputData("Cannot generate a payroll for a future month");
        }
        Staff generatedBy = staffRepository.getReferenceById(actor.staffId());
        List<PayrollDto.Response> done = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        if (req.staffId() != null) {
            Staff s = staffRepository.findById(req.staffId())
                    .orElseThrow(() -> new FalseInputData("Employee " + req.staffId() + " does not exist"));
            done.add(PayrollDto.Response.from(generateFor(s, month, generatedBy)));
        } else {
            for (Staff s : staffRepository.findByStatus(Staff.Status.ACTIVE)) {
                try {
                    done.add(PayrollDto.Response.from(generateFor(s, month, generatedBy)));
                } catch (BusinessRuleException e) {
                    skipped.add(s.getFullName() + ": " + e.getMessage());
                }
            }
        }
        return new PayrollDto.GenerateResponse(month, done, skipped);
    }

    public PayrollDto.Response setDeduction(Long id, PayrollDto.DeductionRequest req) {
        Payroll p = getEntity(id);
        requireDraft(p, "changed");
        BigDecimal earnings = earnings(p);
        if (req.amount().compareTo(earnings) > 0) {
            throw new BusinessRuleException("Deduction exceeds the total earnings (" + earnings + ")");
        }
        p.setDeductionAmount(req.amount());
        p.setDeductionNote(req.note());
        p.setNetAmount(earnings.subtract(req.amount()));
        return PayrollDto.Response.from(p);
    }

    public PayrollDto.Response approve(Long id) {
        Payroll p = getEntity(id);
        requireDraft(p, "approved");
        p.setStatus(Payroll.Status.APPROVED);
        return PayrollDto.Response.from(p);
    }

    public PayrollDto.Response pay(Long id) {
        Payroll p = getEntity(id);
        if (p.getStatus() != Payroll.Status.APPROVED) {
            throw new BusinessRuleException("Only an APPROVED payroll can be paid (current status: " + p.getStatus() + ")");
        }
        p.setStatus(Payroll.Status.PAID);
        p.setPaidAt(LocalDateTime.now());
        return PayrollDto.Response.from(p);
    }

    @Transactional(readOnly = true)
    public PayrollDto.Response get(Long id) {
        return PayrollDto.Response.from(getEntity(id));
    }

    /** An employee sees his payroll only after it was approved. */
    @Transactional(readOnly = true)
    public PayrollDto.Response mine(Long staffId, YearMonth month) {
        Payroll p = payrollRepository.findByStaffIdAndPeriodMonth(staffId, month.atDay(1))
                .filter(x -> x.getStatus() != Payroll.Status.DRAFT)
                .orElseThrow(() -> new ResourceNotFoundException("Your payroll for " + month + " is not ready yet"));
        return PayrollDto.Response.from(p);
    }

    @Transactional(readOnly = true)
    public PageResponse<PayrollDto.Response> search(Long staffId, YearMonth month, Payroll.Status status,
                                                    Pageable pageable) {
        LocalDate periodMonth = month == null ? null : month.atDay(1);
        return PageResponse.of(payrollRepository.search(staffId, periodMonth, status, pageable)
                .map(PayrollDto.Response::from));
    }

    // ------------------------------------------------------------ calculation
    private Payroll generateFor(Staff staff, YearMonth month, Staff generatedBy) {
        LocalDate monthStart = month.atDay(1);
        LocalDateTime from = monthStart.atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();

        Payroll payroll = payrollRepository.findByStaffIdAndPeriodMonth(staff.getId(), monthStart).orElse(null);
        if (payroll != null && payroll.getStatus() != Payroll.Status.DRAFT) {
            throw new BusinessRuleException("Payroll of " + month + " is already " + payroll.getStatus());
        }
        StaffPayPlan plan = payPlanRepository.findEffective(staff.getId(), monthStart, month.atEndOfMonth())
                .stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException("No pay plan for " + month));

        if (payroll == null) {
            payroll = new Payroll();
            payroll.setStaff(staff);
            payroll.setPeriodMonth(monthStart);
            payroll.setGeneratedBy(generatedBy);
        } else {
            // recalculating a draft: free the bonuses that were attached to it
            bonusRepository.findByPayrollId(payroll.getId()).forEach(b -> b.setPayroll(null));
        }

        long minutes = shiftRepository.sumMinutes(staff.getId(), from, to);
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(SIXTY, 2, RoundingMode.HALF_UP);
        BigDecimal hourlyAmount = plan.getHourlyRate().multiply(BigDecimal.valueOf(minutes))
                .divide(SIXTY, 2, RoundingMode.HALF_UP);
        BigDecimal sales = subscriptionRepository.sumSales(staff.getId(), MemberSubscription.Status.CANCELLED, from, to);
        BigDecimal commission = sales.multiply(plan.getSaleCommissionPercent())
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        payroll.setHoursWorked(hours);
        payroll.setHourlyAmount(hourlyAmount);
        payroll.setBaseAmount(plan.getBaseSalary());
        payroll.setCommissionAmount(commission);
        payroll.setGeneratedAt(LocalDateTime.now());
        payroll = payrollRepository.save(payroll);

        List<StaffBonus> bonuses = bonusRepository.findUnassigned(staff.getId(), to);
        BigDecimal bonusTotal = BigDecimal.ZERO;
        for (StaffBonus b : bonuses) {
            b.setPayroll(payroll);
            bonusTotal = bonusTotal.add(b.getAmount());
        }
        payroll.setBonusAmount(bonusTotal);

        BigDecimal earnings = earnings(payroll);
        BigDecimal deduction = payroll.getDeductionAmount().min(earnings);
        payroll.setDeductionAmount(deduction);
        payroll.setNetAmount(earnings.subtract(deduction));
        return payroll;
    }

    private BigDecimal earnings(Payroll p) {
        return p.getHourlyAmount().add(p.getBaseAmount()).add(p.getCommissionAmount()).add(p.getBonusAmount());
    }

    private void requireDraft(Payroll p, String action) {
        if (p.getStatus() != Payroll.Status.DRAFT) {
            throw new BusinessRuleException("Only a DRAFT payroll can be " + action + " (current status: " + p.getStatus() + ")");
        }
    }

    private Payroll getEntity(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll " + id + " not found"));
    }
}
