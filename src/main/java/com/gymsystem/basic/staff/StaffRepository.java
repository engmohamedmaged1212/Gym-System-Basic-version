package com.gymsystem.basic.staff;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    @Override
    @EntityGraph(attributePaths = {"role", "branch"})
    Optional<Staff> findById(Long id);

    @EntityGraph(attributePaths = {"role", "branch"})
    Optional<Staff> findByEmail(String email);

    @EntityGraph(attributePaths = {"role", "branch"})
    Optional<Staff> findByQrToken(String qrToken);

    boolean existsByEmail(String email);

    boolean existsByRole_Code(String roleCode);

    /** prefix search on name / phone / email (indexes idx_staff_name, idx_staff_phone, uq_staff_email) */
    @EntityGraph(attributePaths = {"role", "branch"})
    @Query("select s from Staff s where "
            + "(:q is null or s.fullName like concat(:q, '%') or s.phone like concat(:q, '%') or s.email like concat(:q, '%')) "
            + "and (:roleCode is null or s.role.code = :roleCode) "
            + "and (:status is null or s.status = :status) "
            + "and (:branchId is null or s.branch.id = :branchId)")
    Page<Staff> search(@Param("q") String q, @Param("roleCode") String roleCode,
                       @Param("status") Staff.Status status, @Param("branchId") Long branchId,
                       Pageable pageable);

    List<Staff> findByStatus(Staff.Status status);
}
