package com.gymsystem.basic.kpi;

import com.gymsystem.basic.attendance.MemberAttendanceRepository;
import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.exception.BusinessRuleException;
import com.gymsystem.basic.exception.DuplicateResourceException;
import com.gymsystem.basic.exception.FalseInputData;
import com.gymsystem.basic.exception.ResourceNotFoundException;
import com.gymsystem.basic.shift.StaffShiftRepository;
import com.gymsystem.basic.staff.Staff;
import com.gymsystem.basic.staff.StaffRepository;
import com.gymsystem.basic.subscription.MemberSubscription;
import com.gymsystem.basic.subscription.MemberSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class KpiService {
    private final KpiDefinitionRepository definitionRepository;
    private final KpiTargetRepository targetRepository;
    private final KpiResultRepository resultRepository;
    private final StaffRepository staffRepository;
    private final StaffShiftRepository shiftRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final MemberAttendanceRepository attendanceRepository;

    // ------------------------------------------------------------ definitions
    @Transactional(readOnly = true)
    public List<KpiDto.DefinitionResponse> listDefinitions() {
        return definitionRepository.findAllByOrderByIdAsc().stream().map(KpiDto.DefinitionResponse::from).toList();
    }

    public KpiDto.DefinitionResponse createDefinition(KpiDto.DefinitionRequest req) {
        if (definitionRepository.existsByCode(req.code())) {
            throw new DuplicateResourceException("A KPI with this code already exists");
        }
        KpiDefinition d = new KpiDefinition();
        d.setCode(req.code());
        d.setName(req.name().trim());
        d.setDescription(req.description());
        d.setUnit(req.unit().trim());
        d.setSource(KpiDefinition.Source.MANUAL);
        return KpiDto.DefinitionResponse.from(definitionRepository.save(d));
    }

    public KpiDto.DefinitionResponse setDefinitionActive(Short id, boolean active) {
        KpiDefinition d = definitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KPI " + id + " not found"));
        d.setActive(active);
        return KpiDto.DefinitionResponse.from(d);
    }

    // ------------------------------------------------------------ targets
    public KpiDto.TargetResponse setTarget(KpiDto.TargetRequest req, AuthenticatedUser actor) {
        KpiDefinition kpi = findKpi(req.kpiId());
        Staff staff = findStaff(req.staffId());
        LocalDate month = req.month().atDay(1);
        KpiTarget t = targetRepository.findByKpiIdAndStaffIdAndPeriodMonth(kpi.getId(), staff.getId(), month)
                .orElseGet(KpiTarget::new);
        t.setKpi(kpi);
        t.setStaff(staff);
        t.setPeriodMonth(month);
        t.setTargetValue(req.targetValue());
        t.setSetBy(staffRepository.getReferenceById(actor.staffId()));
        return KpiDto.TargetResponse.from(targetRepository.save(t));
    }

    // ------------------------------------------------------------ results
    /** The owner types the value of a MANUAL KPI (for example a manager rating). */
    public KpiDto.ResultResponse setManualResult(KpiDto.ManualResultRequest req) {
        KpiDefinition kpi = findKpi(req.kpiId());
        if (kpi.getSource() != KpiDefinition.Source.MANUAL) {
            throw new BusinessRuleException("This KPI is calculated automatically, use /compute");
        }
        return KpiDto.ResultResponse.from(upsertResult(kpi, findStaff(req.staffId()), req.month().atDay(1), req.actualValue()));
    }

    /** Calculates every active AUTO KPI of the month from shifts, sales and check-ins. */
    public KpiDto.ComputeResponse compute(KpiDto.ComputeRequest req) {
        YearMonth month = req.month();
        if (month.isAfter(YearMonth.now())) {
            throw new FalseInputData("Cannot compute KPIs for a future month");
        }
        LocalDate monthStart = month.atDay(1);
        LocalDateTime from = monthStart.atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();

        List<KpiDefinition> definitions = definitionRepository.findBySourceAndActiveTrue(KpiDefinition.Source.AUTO);
        List<Staff> employees = req.staffId() != null
                ? List.of(findStaff(req.staffId()))
                : staffRepository.findByStatus(Staff.Status.ACTIVE);

        int written = 0;
        for (Staff s : employees) {
            for (KpiDefinition d : definitions) {
                BigDecimal value = autoValue(d.getCode(), s.getId(), from, to);
                if (value == null) {
                    continue; // an AUTO code that this version does not know how to calculate
                }
                upsertResult(d, s, monthStart, value);
                written++;
            }
        }
        return new KpiDto.ComputeResponse(month, employees.size(), written);
    }

    // ------------------------------------------------------------ report
    @Transactional(readOnly = true)
    public KpiDto.ReportResponse report(Long staffId, YearMonth month) {
        Staff staff = findStaff(staffId);
        LocalDate monthStart = month.atDay(1);
        List<KpiDto.ReportLine> lines = new ArrayList<>();
        for (KpiDefinition d : definitionRepository.findByActiveTrueOrderByIdAsc()) {
            BigDecimal target = targetRepository.findByKpiIdAndStaffIdAndPeriodMonth(d.getId(), staffId, monthStart)
                    .map(KpiTarget::getTargetValue).orElse(null);
            BigDecimal actual = resultRepository.findByKpiIdAndStaffIdAndPeriodMonth(d.getId(), staffId, monthStart)
                    .map(KpiResult::getActualValue).orElse(null);
            BigDecimal achievement = null;
            if (target != null && actual != null && target.signum() > 0) {
                achievement = actual.multiply(BigDecimal.valueOf(100)).divide(target, 2, RoundingMode.HALF_UP);
            }
            lines.add(new KpiDto.ReportLine(d.getId(), d.getCode(), d.getName(), d.getUnit(), d.getSource(),
                    target, actual, achievement));
        }
        return new KpiDto.ReportResponse(staff.getId(), staff.getFullName(), month, lines);
    }

    // ------------------------------------------------------------ helpers
    private BigDecimal autoValue(String code, Long staffId, LocalDateTime from, LocalDateTime to) {
        return switch (code) {
            case "HOURS_WORKED" -> BigDecimal.valueOf(shiftRepository.sumMinutes(staffId, from, to))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            case "SUBSCRIPTIONS_SOLD" -> BigDecimal.valueOf(
                    subscriptionRepository.countSold(staffId, MemberSubscription.Status.CANCELLED, from, to));
            case "SALES_AMOUNT" ->
                    subscriptionRepository.sumSales(staffId, MemberSubscription.Status.CANCELLED, from, to);
            case "MEMBER_CHECKINS" -> BigDecimal.valueOf(attendanceRepository.countByRecorder(staffId, from, to));
            default -> null;
        };
    }

    private KpiResult upsertResult(KpiDefinition kpi, Staff staff, LocalDate month, BigDecimal value) {
        KpiResult r = resultRepository.findByKpiIdAndStaffIdAndPeriodMonth(kpi.getId(), staff.getId(), month)
                .orElseGet(KpiResult::new);
        r.setKpi(kpi);
        r.setStaff(staff);
        r.setPeriodMonth(month);
        r.setActualValue(value);
        r.setComputedAt(LocalDateTime.now());
        return resultRepository.save(r);
    }

    private KpiDefinition findKpi(Short id) {
        return definitionRepository.findById(id)
                .orElseThrow(() -> new FalseInputData("KPI " + id + " does not exist"));
    }

    private Staff findStaff(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new FalseInputData("Employee " + id + " does not exist"));
    }
}
