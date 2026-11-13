package com.gymsystem.basic.payroll;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    @Override
    @EntityGraph(attributePaths = {"staff", "generatedBy"})
    Optional<Payroll> findById(Long id);

    /** unique (staff_id, period_month) */
    @EntityGraph(attributePaths = {"staff", "generatedBy"})
    Optional<Payroll> findByStaffIdAndPeriodMonth(Long staffId, LocalDate periodMonth);

    @EntityGraph(attributePaths = {"staff", "generatedBy"})
    @Query("select p from Payroll p where (:staffId is null or p.staff.id = :staffId) "
            + "and (:month is null or p.periodMonth = :month) "
            + "and (:status is null or p.status = :status)")
    Page<Payroll> search(@Param("staffId") Long staffId, @Param("month") LocalDate month,
                         @Param("status") Payroll.Status status, Pageable pageable);
}
