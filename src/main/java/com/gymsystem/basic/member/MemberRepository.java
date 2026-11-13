package com.gymsystem.basic.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Override
    @EntityGraph(attributePaths = "branch")
    Optional<Member> findById(Long id);

    @EntityGraph(attributePaths = "branch")
    Optional<Member> findByQrToken(String qrToken);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    /** prefix search on name / phone / email (idx_member_name, uq_member_phone, idx_member_email) */
    @EntityGraph(attributePaths = "branch")
    @Query("select m from Member m where "
            + "(:q is null or m.fullName like concat(:q, '%') or m.phone like concat(:q, '%') or m.email like concat(:q, '%')) "
            + "and (:status is null or m.status = :status) "
            + "and (:branchId is null or m.branch.id = :branchId)")
    Page<Member> search(@Param("q") String q, @Param("status") Member.Status status,
                        @Param("branchId") Long branchId, Pageable pageable);
}
