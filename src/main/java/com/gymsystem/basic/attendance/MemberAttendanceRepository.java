package com.gymsystem.basic.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MemberAttendanceRepository extends JpaRepository<MemberAttendance, Long> {

    @Override
    @EntityGraph(attributePaths = {"member", "branch", "recordedBy"})
    Optional<MemberAttendance> findById(Long id);

    /** uses idx_att_member_time */
    Optional<MemberAttendance> findFirstByMemberIdOrderByCheckinAtDesc(Long memberId);

    @EntityGraph(attributePaths = {"member", "branch", "recordedBy"})
    @Query("select a from MemberAttendance a where (:memberId is null or a.member.id = :memberId) "
            + "and (:branchId is null or a.branch.id = :branchId) "
            + "and (:from is null or a.checkinAt >= :from) and (:to is null or a.checkinAt < :to)")
    Page<MemberAttendance> search(@Param("memberId") Long memberId, @Param("branchId") Long branchId,
                                  @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("select count(a) from MemberAttendance a where (:branchId is null or a.branch.id = :branchId) "
            + "and a.checkinAt >= :from and a.checkinAt < :to")
    long countInRange(@Param("branchId") Long branchId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** check-ins entered by one employee (KPI MEMBER_CHECKINS) - idx_att_recorder */
    @Query("select count(a) from MemberAttendance a where a.recordedBy.id = :staffId "
            + "and a.checkinAt >= :from and a.checkinAt < :to")
    long countByRecorder(@Param("staffId") Long staffId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
