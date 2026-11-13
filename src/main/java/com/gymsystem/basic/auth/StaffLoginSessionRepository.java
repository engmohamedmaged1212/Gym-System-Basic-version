package com.gymsystem.basic.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface StaffLoginSessionRepository extends JpaRepository<StaffLoginSession, Long> {

    /** an OPEN session (not logged out) that belongs to this staff member */
    Optional<StaffLoginSession> findByIdAndStaffIdAndLogoutAtIsNull(Long id, Long staffId);

    @EntityGraph(attributePaths = "staff")
    @Query("select s from StaffLoginSession s where (:staffId is null or s.staff.id = :staffId) "
            + "and (:from is null or s.loginAt >= :from) and (:to is null or s.loginAt < :to)")
    Page<StaffLoginSession> search(@Param("staffId") Long staffId, @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to, Pageable pageable);
}
