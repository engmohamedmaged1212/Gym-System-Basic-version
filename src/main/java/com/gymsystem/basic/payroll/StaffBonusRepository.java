package com.gymsystem.basic.payroll;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StaffBonusRepository extends JpaRepository<StaffBonus, Long> {

    @Override
    @EntityGraph(attributePaths = {"staff", "grantedBy", "memberSubscription", "payroll"})
    Optional<StaffBonus> findById(Long id);

    @EntityGraph(attributePaths = {"staff", "grantedBy", "memberSubscription", "payroll"})
    @Query("select b from StaffBonus b where (:staffId is null or b.staff.id = :staffId) "
            + "and (:reason is null or b.reasonType = :reason) "
            + "and (:payrollId is null or b.payroll.id = :payrollId) "
            + "and (:unassignedOnly = false or b.payroll is null) "
            + "and (:from is null or b.grantedAt >= :from) and (:to is null or b.grantedAt < :to)")
    Page<StaffBonus> search(@Param("staffId") Long staffId, @Param("reason") StaffBonus.ReasonType reason,
                            @Param("payrollId") Long payrollId, @Param("unassignedOnly") boolean unassignedOnly,
                            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    /** bonuses not yet paid in any payroll, granted before the given moment (idx_bonus_unpaid) */
    @Query("select b from StaffBonus b where b.staff.id = :staffId and b.payroll is null and b.grantedAt < :before")
    List<StaffBonus> findUnassigned(@Param("staffId") Long staffId, @Param("before") LocalDateTime before);

    List<StaffBonus> findByPayrollId(Long payrollId);

    /** used when a subscription is cancelled: its sale bonus disappears if it was not paid yet */
    @Modifying(flushAutomatically = true)
    @Query("delete from StaffBonus b where b.memberSubscription.id = :subscriptionId "
            + "and b.payroll is null and b.reasonType = :reason")
    int deleteUnassignedSaleBonuses(@Param("subscriptionId") Long subscriptionId,
                                    @Param("reason") StaffBonus.ReasonType reason);
}
