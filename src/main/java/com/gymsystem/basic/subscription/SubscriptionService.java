package com.gymsystem.basic.subscription;

import com.gymsystem.basic.branch.Branch;
import com.gymsystem.basic.branch.BranchRepository;
import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.exception.BusinessRuleException;
import com.gymsystem.basic.exception.FalseInputData;
import com.gymsystem.basic.exception.ResourceNotFoundException;
import com.gymsystem.basic.member.Member;
import com.gymsystem.basic.member.MemberRepository;
import com.gymsystem.basic.offer.Offer;
import com.gymsystem.basic.offer.OfferService;
import com.gymsystem.basic.payment.Payment;
import com.gymsystem.basic.payment.PaymentRepository;
import com.gymsystem.basic.payment.PaymentService;
import com.gymsystem.basic.payroll.StaffBonus;
import com.gymsystem.basic.payroll.StaffBonusRepository;
import com.gymsystem.basic.plan.SubscriptionPlan;
import com.gymsystem.basic.plan.SubscriptionPlanRepository;
import com.gymsystem.basic.staff.Staff;
import com.gymsystem.basic.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {
    private final MemberSubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final SubscriptionPlanRepository planRepository;
    private final BranchRepository branchRepository;
    private final StaffRepository staffRepository;
    private final OfferService offerService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final StaffBonusRepository bonusRepository;

    /**
     * Sells a plan to a member.
     * price  = the offer price if an offer applies, otherwise the plan price (both are copied into the row)
     * bonus  = the sale bonus of the offer (or of the plan) goes to the employee who persuaded the client
     */
    public SubscriptionDto.Response sell(SubscriptionDto.SellRequest req, AuthenticatedUser actor) {
        Member member = memberRepository.findById(req.memberId())
                .orElseThrow(() -> new FalseInputData("Member " + req.memberId() + " does not exist"));
        if (member.getStatus() != Member.Status.ACTIVE) {
            throw new BusinessRuleException("Member is " + member.getStatus() + " and cannot subscribe");
        }
        SubscriptionPlan plan = planRepository.findById(req.planId())
                .orElseThrow(() -> new FalseInputData("Plan " + req.planId() + " does not exist"));
        if (!plan.isActive()) {
            throw new BusinessRuleException("This plan is not active");
        }
        Long branchId = req.branchId() != null ? req.branchId() : actor.branchId();
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new FalseInputData("Branch " + branchId + " does not exist"));

        LocalDateTime now = LocalDateTime.now();
        Offer offer = req.offerId() != null
                ? offerService.requireUsableOffer(req.offerId(), plan.getId(), branchId, now)
                : offerService.findBestOffer(plan.getId(), branchId, now).orElse(null);

        BigDecimal listPrice = plan.getPrice();
        BigDecimal discount = offer == null ? BigDecimal.ZERO
                : listPrice.subtract(offer.getOfferPrice()).max(BigDecimal.ZERO);
        BigDecimal amountDue = listPrice.subtract(discount);

        Staff soldBy = null;
        if (req.soldByStaffId() != null) {
            soldBy = staffRepository.findById(req.soldByStaffId())
                    .orElseThrow(() -> new FalseInputData("Employee " + req.soldByStaffId() + " does not exist"));
            if (soldBy.getStatus() != Staff.Status.ACTIVE) {
                throw new FalseInputData("The selling employee is not active");
            }
        }

        LocalDate start = req.startDate() != null ? req.startDate() : LocalDate.now();
        Staff createdBy = staffRepository.getReferenceById(actor.staffId());

        MemberSubscription ms = new MemberSubscription();
        ms.setMember(member);
        ms.setPlan(plan);
        ms.setBranch(branch);
        ms.setOffer(offer);
        ms.setAccessAllBranches(plan.isAllBranches());
        ms.setStartDate(start);
        ms.setEndDate(start.plusDays(plan.getDurationDays() - 1L));
        ms.setListPrice(listPrice);
        ms.setDiscountAmount(discount);
        ms.setAmountDue(amountDue);
        ms.setSoldBy(soldBy);
        ms.setCreatedBy(createdBy);
        ms.setNote(req.note());
        subscriptionRepository.save(ms);

        if (soldBy != null) {
            createSaleBonus(ms, plan, offer, soldBy, createdBy);
        }
        if (req.initialPayment() != null) {
            Payment.Method method = req.initialPayment().method() != null
                    ? req.initialPayment().method() : Payment.Method.CASH;
            paymentService.record(ms, Payment.Type.PAYMENT, req.initialPayment().amount(), method,
                    actor.staffId(), "Initial payment", null);
        }
        return toResponse(ms);
    }

    /** Cancels a subscription, removes the unpaid sale bonus, and optionally gives money back. */
    public SubscriptionDto.Response cancel(Long id, SubscriptionDto.CancelRequest req, AuthenticatedUser actor) {
        MemberSubscription ms = getEntity(id);
        if (ms.getStatus() == MemberSubscription.Status.CANCELLED) {
            throw new BusinessRuleException("This subscription is already cancelled");
        }
        if (ms.getStatus() == MemberSubscription.Status.EXPIRED) {
            throw new BusinessRuleException("An expired subscription cannot be cancelled");
        }
        if (req.refundAmount() != null) {
            // validated first, so nothing is changed when the refund is wrong
            paymentService.record(ms, Payment.Type.REFUND, req.refundAmount(), Payment.Method.CASH,
                    actor.staffId(), "Refund on cancellation", null);
        }
        ms.setStatus(MemberSubscription.Status.CANCELLED);
        ms.setCancelledAt(LocalDateTime.now());
        if (req.reason() != null && !req.reason().isBlank()) {
            ms.setNote(("Cancelled: " + req.reason().trim()));
        }
        bonusRepository.deleteUnassignedSaleBonuses(ms.getId(), StaffBonus.ReasonType.SUBSCRIPTION_SALE);
        return toResponse(ms);
    }

    @Transactional(readOnly = true)
    public SubscriptionDto.Response get(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<SubscriptionDto.Response> search(Long memberId, MemberSubscription.Status status,
                                                         Long branchId, Long soldById, Integer expiringInDays,
                                                         Pageable pageable) {
        LocalDate expiringBefore = expiringInDays == null ? null : LocalDate.now().plusDays(expiringInDays);
        return PageResponse.of(subscriptionRepository
                .search(memberId, status, branchId, soldById, expiringBefore, pageable)
                .map(this::toResponse));
    }

    /** Every night at 00:05: ACTIVE subscriptions past their end date become EXPIRED. */
    @Scheduled(cron = "0 5 0 * * *")
    public void expireDue() {
        int n = subscriptionRepository.expire(MemberSubscription.Status.ACTIVE,
                MemberSubscription.Status.EXPIRED, LocalDate.now());
        if (n > 0) {
            log.info("{} subscription(s) expired", n);
        }
    }

    // ------------------------------------------------------------ helpers
    private void createSaleBonus(MemberSubscription ms, SubscriptionPlan plan, Offer offer, Staff soldBy, Staff grantedBy) {
        BigDecimal amount = offer != null && offer.getSaleBonusAmount() != null
                ? offer.getSaleBonusAmount() : plan.getSaleBonusAmount();
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        StaffBonus bonus = new StaffBonus();
        bonus.setStaff(soldBy);
        bonus.setAmount(amount);
        bonus.setReasonType(StaffBonus.ReasonType.SUBSCRIPTION_SALE);
        bonus.setMemberSubscription(ms);
        bonus.setGrantedBy(grantedBy);
        bonus.setNote("Sale bonus: " + plan.getName() + (offer != null ? " (" + offer.getName() + ")" : ""));
        bonusRepository.save(bonus);
    }

    private SubscriptionDto.Response toResponse(MemberSubscription ms) {
        BigDecimal paid = paymentRepository.sumByType(ms.getId(), Payment.Type.PAYMENT);
        BigDecimal refunded = paymentRepository.sumByType(ms.getId(), Payment.Type.REFUND);
        return SubscriptionDto.Response.from(ms, paid, refunded);
    }

    private MemberSubscription getEntity(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription " + id + " not found"));
    }
}
