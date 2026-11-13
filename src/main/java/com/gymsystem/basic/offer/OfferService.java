package com.gymsystem.basic.offer;

import com.gymsystem.basic.branch.Branch;
import com.gymsystem.basic.branch.BranchRepository;
import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.exception.BusinessRuleException;
import com.gymsystem.basic.exception.FalseInputData;
import com.gymsystem.basic.exception.ResourceNotFoundException;
import com.gymsystem.basic.plan.SubscriptionPlan;
import com.gymsystem.basic.plan.SubscriptionPlanRepository;
import com.gymsystem.basic.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * An offer is a fixed price for ONE plan during a date-time window
 * (e.g. the half-year plan costs 1500 instead of 2000 today only).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OfferService {
    private final OfferRepository offerRepository;
    private final SubscriptionPlanRepository planRepository;
    private final BranchRepository branchRepository;
    private final StaffRepository staffRepository;

    public OfferDto.Response create(OfferDto.Request req, AuthenticatedUser actor) {
        Offer o = new Offer();
        apply(o, req);
        o.setCreatedBy(staffRepository.getReferenceById(actor.staffId()));
        return OfferDto.Response.from(offerRepository.save(o));
    }

    public OfferDto.Response update(Long id, OfferDto.Request req) {
        Offer o = getEntity(id);
        apply(o, req);
        return OfferDto.Response.from(o);
    }

    public OfferDto.Response setActive(Long id, boolean active) {
        Offer o = getEntity(id);
        o.setActive(active);
        return OfferDto.Response.from(o);
    }

    @Transactional(readOnly = true)
    public OfferDto.Response get(Long id) {
        return OfferDto.Response.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<OfferDto.Response> search(String q, Long planId, Long branchId, Boolean active,
                                                  boolean runningNow, Pageable pageable) {
        String query = q == null || q.isBlank() ? null : q.trim();
        LocalDateTime runningAt = runningNow ? LocalDateTime.now() : null;
        return PageResponse.of(offerRepository.search(query, planId, branchId, active, runningAt, pageable)
                .map(OfferDto.Response::from));
    }

    /** Offers a receptionist can use right now for this plan in this branch, cheapest first. */
    @Transactional(readOnly = true)
    public List<OfferDto.Response> applicable(Long planId, Long branchId) {
        return offerRepository.findApplicable(planId, branchId, LocalDateTime.now()).stream()
                .map(OfferDto.Response::from).toList();
    }

    // ------------------------------------------------------------ used by SubscriptionService
    @Transactional(readOnly = true)
    public Optional<Offer> findBestOffer(Long planId, Long branchId, LocalDateTime now) {
        return offerRepository.findApplicable(planId, branchId, now).stream().findFirst();
    }

    /** The receptionist picked a specific offer: it must really be usable for this sale. */
    @Transactional(readOnly = true)
    public Offer requireUsableOffer(Long offerId, Long planId, Long branchId, LocalDateTime now) {
        Offer o = getEntity(offerId);
        boolean running = o.isActive() && !now.isBefore(o.getValidFrom()) && !now.isAfter(o.getValidTo());
        if (!running) {
            throw new BusinessRuleException("This offer is not running right now");
        }
        if (!o.getPlan().getId().equals(planId)) {
            throw new BusinessRuleException("This offer does not belong to the selected plan");
        }
        if (o.getBranch() != null && !o.getBranch().getId().equals(branchId)) {
            throw new BusinessRuleException("This offer is not valid in this branch");
        }
        return o;
    }

    // ------------------------------------------------------------ helpers
    private void apply(Offer o, OfferDto.Request req) {
        if (!req.validTo().isAfter(req.validFrom())) {
            throw new FalseInputData("validTo must be after validFrom");
        }
        SubscriptionPlan plan = planRepository.findById(req.planId())
                .orElseThrow(() -> new FalseInputData("Plan " + req.planId() + " does not exist"));
        if (!plan.isActive()) {
            throw new FalseInputData("The plan is not active");
        }
        if (req.offerPrice().compareTo(plan.getPrice()) >= 0) {
            throw new FalseInputData("Offer price must be lower than the regular plan price (" + plan.getPrice() + ")");
        }
        Branch branch = null;
        if (req.branchId() != null) {
            branch = branchRepository.findById(req.branchId())
                    .orElseThrow(() -> new FalseInputData("Branch " + req.branchId() + " does not exist"));
        }
        o.setName(req.name().trim());
        o.setPlan(plan);
        o.setBranch(branch);
        o.setOfferPrice(req.offerPrice());
        o.setSaleBonusAmount(req.saleBonusAmount());
        o.setValidFrom(req.validFrom());
        o.setValidTo(req.validTo());
    }

    private Offer getEntity(Long id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer " + id + " not found"));
    }
}
