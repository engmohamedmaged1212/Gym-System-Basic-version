package com.gymsystem.basic.payment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = "receivedBy")
    List<Payment> findByMemberSubscriptionIdOrderByPaidAtDesc(Long subscriptionId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p "
            + "where p.memberSubscription.id = :subscriptionId and p.type = :type")
    BigDecimal sumByType(@Param("subscriptionId") Long subscriptionId, @Param("type") Payment.Type type);

    /** money in/out in a period (idx_payment_time), optionally for one branch */
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.type = :type "
            + "and p.paidAt >= :from and p.paidAt < :to "
            + "and (:branchId is null or p.memberSubscription.branch.id = :branchId)")
    BigDecimal sumInRange(@Param("type") Payment.Type type, @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to, @Param("branchId") Long branchId);
}
