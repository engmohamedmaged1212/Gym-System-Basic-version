package com.gymsystem.basic.subscription;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, Long> {

    @Override
    @EntityGraph(attributePaths = {"member", "plan", "branch", "offer", "soldBy", "createdBy"})
    Optional<MemberSubscription> findById(Long id);

    /** subscriptions that let a member enter a branch today (idx_ms_member) */
    @Query("select ms from MemberSubscription ms where ms.member.id = :memberId and ms.status = :status "
            + "and ms.startDate <= :today and ms.endDate >= :today "
            + "and (ms.accessAllBranches = true or ms.branch.id = :branchId)")
    List<MemberSubscription> findValidForEntry(@Param("memberId") Long memberId,
                                               @Param("status") MemberSubscription.Status status,
                                               @Param("today") LocalDate today, @Param("branchId") Long branchId);

    @EntityGraph(attributePaths = {"member", "plan", "branch", "offer", "soldBy", "createdBy"})
    @Query("select ms from MemberSubscription ms where (:memberId is null or ms.member.id = :memberId) "
            + "and (:status is null or ms.status = :status) "
            + "and (:branchId is null or ms.branch.id = :branchId) "
            + "and (:soldById is null or ms.soldBy.id = :soldById) "
            + "and (:expiringBefore is null or ms.endDate <= :expiringBefore)")
    Page<MemberSubscription> search(@Param("memberId") Long memberId,
                                    @Param("status") MemberSubscription.Status status,
                                    @Param("branchId") Long branchId, @Param("soldById") Long soldById,
                                    @Param("expiringBefore") LocalDate expiringBefore, Pageable pageable);

    /** nightly job: ACTIVE subscriptions whose end date passed become EXPIRED (idx_ms_status_end) */
    @Modifying(flushAutomatically = true)
    @Query("update MemberSubscription ms set ms.status = :newStatus "
            + "where ms.status = :oldStatus and ms.endDate < :today")
    int expire(@Param("oldStatus") MemberSubscription.Status oldStatus,
               @Param("newStatus") MemberSubscription.Status newStatus, @Param("today") LocalDate today);

    // ---- numbers used by payroll and KPIs (idx_ms_sold_by)
    @Query("select count(ms) from MemberSubscription ms where ms.soldBy.id = :staffId "
            + "and ms.status <> :excluded and ms.createdAt >= :from and ms.createdAt < :to")
    long countSold(@Param("staffId") Long staffId, @Param("excluded") MemberSubscription.Status excluded,
                   @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(ms.amountDue), 0) from MemberSubscription ms where ms.soldBy.id = :staffId "
            + "and ms.status <> :excluded and ms.createdAt >= :from and ms.createdAt < :to")
    BigDecimal sumSales(@Param("staffId") Long staffId, @Param("excluded") MemberSubscription.Status excluded,
                        @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
