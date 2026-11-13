package com.gymsystem.basic.payroll;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StaffPayPlanRepository extends JpaRepository<StaffPayPlan, Long> {

    @EntityGraph(attributePaths = {"staff", "createdBy"})
    List<StaffPayPlan> findByStaffIdOrderByEffectiveFromDesc(Long staffId);

    /** the plan that is still open (effective_to is null) */
    Optional<StaffPayPlan> findFirstByStaffIdAndEffectiveToIsNull(Long staffId);

    /** plans that overlap a month, newest first (idx_payplan_staff) */
    @Query("select p from StaffPayPlan p where p.staff.id = :staffId and p.effectiveFrom <= :monthEnd "
            + "and (p.effectiveTo is null or p.effectiveTo >= :monthStart) order by p.effectiveFrom desc")
    List<StaffPayPlan> findEffective(@Param("staffId") Long staffId, @Param("monthStart") LocalDate monthStart,
                                     @Param("monthEnd") LocalDate monthEnd);
}
