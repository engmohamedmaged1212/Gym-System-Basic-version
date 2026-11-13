package com.gymsystem.basic.payment;

import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.exception.BusinessRuleException;
import com.gymsystem.basic.exception.FalseInputData;
import com.gymsystem.basic.exception.ResourceNotFoundException;
import com.gymsystem.basic.staff.StaffRepository;
import com.gymsystem.basic.subscription.MemberSubscription;
import com.gymsystem.basic.subscription.MemberSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final StaffRepository staffRepository;

    public PaymentDto.Response create(PaymentDto.CreateRequest req, AuthenticatedUser actor) {
        MemberSubscription ms = subscriptionRepository.findById(req.subscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription " + req.subscriptionId() + " not found"));
        LocalDateTime paidAt = req.paidAt();
        if (paidAt != null && paidAt.isAfter(LocalDateTime.now().plusMinutes(1))) {
            throw new FalseInputData("paidAt cannot be in the future");
        }
        Payment p = record(ms,
                req.type() != null ? req.type() : Payment.Type.PAYMENT,
                req.amount(),
                req.method() != null ? req.method() : Payment.Method.CASH,
                actor.staffId(), req.note(), paidAt);
        return PaymentDto.Response.from(p);
    }

    /**
     * The one place where money is recorded (also used when a subscription is sold or cancelled).
     * PAYMENT can not exceed what is still due; REFUND can not exceed what was paid and not yet refunded.
     */
    public Payment record(MemberSubscription ms, Payment.Type type, BigDecimal amount, Payment.Method method,
                          Long receivedById, String note, LocalDateTime paidAt) {
        BigDecimal paid = paymentRepository.sumByType(ms.getId(), Payment.Type.PAYMENT);
        BigDecimal refunded = paymentRepository.sumByType(ms.getId(), Payment.Type.REFUND);

        if (type == Payment.Type.PAYMENT) {
            if (ms.getStatus() == MemberSubscription.Status.CANCELLED) {
                throw new BusinessRuleException("Cannot add a payment to a cancelled subscription");
            }
            BigDecimal remaining = ms.getAmountDue().subtract(paid).add(refunded);
            if (amount.compareTo(remaining) > 0) {
                throw new BusinessRuleException("Payment exceeds the remaining amount (" + remaining + ")");
            }
        } else {
            BigDecimal refundable = paid.subtract(refunded);
            if (amount.compareTo(refundable) > 0) {
                throw new BusinessRuleException("Refund exceeds the amount paid so far (" + refundable + ")");
            }
        }

        Payment p = new Payment();
        p.setMemberSubscription(ms);
        p.setType(type);
        p.setAmount(amount);
        p.setMethod(method);
        p.setPaidAt(paidAt != null ? paidAt : LocalDateTime.now());
        p.setReceivedBy(staffRepository.getReferenceById(receivedById));
        p.setNote(note);
        return paymentRepository.save(p);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.Response> listForSubscription(Long subscriptionId) {
        return paymentRepository.findByMemberSubscriptionIdOrderByPaidAtDesc(subscriptionId).stream()
                .map(PaymentDto.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PaymentDto.SummaryResponse summary(LocalDate from, LocalDate to, Long branchId) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        BigDecimal in = paymentRepository.sumInRange(Payment.Type.PAYMENT, start, end, branchId);
        BigDecimal out = paymentRepository.sumInRange(Payment.Type.REFUND, start, end, branchId);
        return new PaymentDto.SummaryResponse(from, to, branchId, in, out, in.subtract(out));
    }
}
