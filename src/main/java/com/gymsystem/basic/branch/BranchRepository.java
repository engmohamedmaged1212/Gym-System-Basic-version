package com.gymsystem.basic.branch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** prefix search on name (uses the unique index on branch.name) */
    @Query("select b from Branch b where (:q is null or b.name like concat(:q, '%')) "
            + "and (:active is null or b.active = :active)")
    Page<Branch> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);
}
