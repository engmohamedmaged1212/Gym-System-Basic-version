package com.gymsystem.basic.shift;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface StaffShiftRepository extends JpaRepository<StaffShift, Long> {

    @Override
    @EntityGraph(attributePaths = {"staff", "branch"})
    Optional<StaffShift> findById(Long id);

    /** the shift that is still open (idx_shift_open) */
    @EntityGraph(attributePaths = {"staff", "branch"})
    Optional<StaffShift> findFirstByStaffIdAndClockOutAtIsNullOrderByClockInAtDesc(Long staffId);

    @EntityGraph(attributePaths = {"staff", "branch"})
    @Query("select s from StaffShift s where (:staffId is null or s.staff.id = :staffId) "
            + "and (:branchId is null or s.branch.id = :branchId) "
            + "and (:from is null or s.clockInAt >= :from) and (:to is null or s.clockInAt < :to)")
    Page<StaffShift> search(@Param("staffId") Long staffId, @Param("branchId") Long branchId,
                            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    /** minutes worked in closed shifts that STARTED in [from, to) */
    @Query("select coalesce(sum(s.minutesWorked), 0) from StaffShift s where s.staff.id = :staffId "
            + "and s.clockOutAt is not null and s.clockInAt >= :from and s.clockInAt < :to")
    Long sumMinutes(@Param("staffId") Long staffId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(s) from StaffShift s where s.staff.id = :staffId "
            + "and s.clockOutAt is not null and s.clockInAt >= :from and s.clockInAt < :to")
    long countClosed(@Param("staffId") Long staffId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
