package com.gymsystem.basic.gymclass;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GymClassRepository extends JpaRepository<GymClass, Long> {

    @Override
    @EntityGraph(attributePaths = {"branch", "trainer"})
    Optional<GymClass> findById(Long id);

    List<GymClass> findByIdIn(Collection<Long> ids);

    /** prefix search on name (idx_class_name) */
    @EntityGraph(attributePaths = {"branch", "trainer"})
    @Query("select c from GymClass c where (:q is null or c.name like concat(:q, '%')) "
            + "and (:branchId is null or c.branch.id = :branchId) "
            + "and (:trainerId is null or c.trainer.id = :trainerId) "
            + "and (:status is null or c.status = :status)")
    Page<GymClass> search(@Param("q") String q, @Param("branchId") Long branchId, @Param("trainerId") Long trainerId,
                          @Param("status") GymClass.Status status, Pageable pageable);
}
