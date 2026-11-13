package com.gymsystem.basic.plan;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    @Override
    @EntityGraph(attributePaths = "classes")
    Optional<SubscriptionPlan> findById(Long id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** prefix search on name (uq_plan_name), filter on idx_plan_active */
    @Query("select p from SubscriptionPlan p where (:q is null or p.name like concat(:q, '%')) "
            + "and (:active is null or p.active = :active)")
    Page<SubscriptionPlan> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);
}
