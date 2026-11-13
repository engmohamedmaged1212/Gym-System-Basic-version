package com.gymsystem.basic.plan;

import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.exception.DuplicateResourceException;
import com.gymsystem.basic.exception.FalseInputData;
import com.gymsystem.basic.exception.ResourceNotFoundException;
import com.gymsystem.basic.gymclass.GymClass;
import com.gymsystem.basic.gymclass.GymClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class PlanService {
    private final SubscriptionPlanRepository planRepository;
    private final GymClassRepository classRepository;

    public PlanDto.Response create(PlanDto.Request req) {
        String name = req.name().trim();
        if (planRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A plan with this name already exists");
        }
        SubscriptionPlan p = new SubscriptionPlan();
        apply(p, req, name);
        return PlanDto.Response.from(planRepository.save(p));
    }

    /** A new price only affects future sales: sold subscriptions keep the price they were sold at. */
    public PlanDto.Response update(Long id, PlanDto.Request req) {
        SubscriptionPlan p = getEntity(id);
        String name = req.name().trim();
        if (planRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("A plan with this name already exists");
        }
        apply(p, req, name);
        return PlanDto.Response.from(p);
    }

    public PlanDto.Response setActive(Long id, boolean active) {
        SubscriptionPlan p = getEntity(id);
        p.setActive(active);
        return PlanDto.Response.from(p);
    }

    @Transactional(readOnly = true)
    public PlanDto.Response get(Long id) {
        return PlanDto.Response.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<PlanDto.Response> search(String q, Boolean active, Pageable pageable) {
        String query = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(planRepository.search(query, active, pageable).map(PlanDto.Response::from));
    }

    private void apply(SubscriptionPlan p, PlanDto.Request req, String name) {
        boolean includesGym = Boolean.TRUE.equals(req.includesGym());
        Set<Long> classIds = req.classIds() == null ? Set.of() : req.classIds();
        if (!includesGym && classIds.isEmpty()) {
            throw new FalseInputData("A plan must include the gym or at least one class");
        }
        Set<GymClass> classes = new HashSet<>();
        if (!classIds.isEmpty()) {
            List<GymClass> found = classRepository.findByIdIn(classIds);
            if (found.size() != classIds.size()) {
                throw new FalseInputData("One or more classes do not exist");
            }
            if (found.stream().anyMatch(c -> c.getStatus() == GymClass.Status.DISCONTINUED)) {
                throw new FalseInputData("A discontinued class cannot be added to a plan");
            }
            classes.addAll(found);
        }
        p.setName(name);
        p.setDescription(req.description());
        p.setDurationDays(req.durationDays());
        p.setPrice(req.price());
        p.setSaleBonusAmount(req.saleBonusAmount() != null ? req.saleBonusAmount() : BigDecimal.ZERO);
        p.setIncludesGym(includesGym);
        p.setAllBranches(Boolean.TRUE.equals(req.allBranches()));
        p.getClasses().clear();
        p.getClasses().addAll(classes);
    }

    private SubscriptionPlan getEntity(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan " + id + " not found"));
    }
}
