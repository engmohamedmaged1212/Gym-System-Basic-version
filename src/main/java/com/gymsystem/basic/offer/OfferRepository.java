package com.gymsystem.basic.offer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    @Override
    @EntityGraph(attributePaths = {"plan", "branch", "createdBy"})
    Optional<Offer> findById(Long id);

    /** offers running right now for a plan in a branch, cheapest first (idx_offer_lookup) */
    @EntityGraph(attributePaths = {"plan", "branch", "createdBy"})
    @Query("select o from Offer o where o.plan.id = :planId and o.active = true "
            + "and o.validFrom <= :now and o.validTo >= :now "
            + "and (o.branch is null or o.branch.id = :branchId) order by o.offerPrice asc")
    List<Offer> findApplicable(@Param("planId") Long planId, @Param("branchId") Long branchId,
                               @Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"plan", "branch", "createdBy"})
    @Query("select o from Offer o where (:q is null or o.name like concat(:q, '%')) "
            + "and (:planId is null or o.plan.id = :planId) "
            + "and (:branchId is null or o.branch.id = :branchId) "
            + "and (:active is null or o.active = :active) "
            + "and (:runningAt is null or (o.validFrom <= :runningAt and o.validTo >= :runningAt))")
    Page<Offer> search(@Param("q") String q, @Param("planId") Long planId, @Param("branchId") Long branchId,
                       @Param("active") Boolean active, @Param("runningAt") LocalDateTime runningAt,
                       Pageable pageable);
}
