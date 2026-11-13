package com.gymsystem.basic.kpi;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface KpiTargetRepository extends JpaRepository<KpiTarget, Long> {

    /** unique (kpi_id, staff_id, period_month) */
    Optional<KpiTarget> findByKpiIdAndStaffIdAndPeriodMonth(Short kpiId, Long staffId, LocalDate periodMonth);
}
