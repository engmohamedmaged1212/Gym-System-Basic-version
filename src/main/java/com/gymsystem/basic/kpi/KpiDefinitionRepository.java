package com.gymsystem.basic.kpi;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KpiDefinitionRepository extends JpaRepository<KpiDefinition, Short> {

    boolean existsByCode(String code);

    Optional<KpiDefinition> findByCode(String code);

    List<KpiDefinition> findAllByOrderByIdAsc();

    List<KpiDefinition> findByActiveTrueOrderByIdAsc();

    List<KpiDefinition> findBySourceAndActiveTrue(KpiDefinition.Source source);
}
