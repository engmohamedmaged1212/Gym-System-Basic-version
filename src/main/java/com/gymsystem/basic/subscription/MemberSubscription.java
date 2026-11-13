package com.gymsystem.basic.subscription;

import com.gymsystem.basic.branch.Branch;
import com.gymsystem.basic.member.Member;
import com.gymsystem.basic.offer.Offer;
import com.gymsystem.basic.plan.SubscriptionPlan;
import com.gymsystem.basic.staff.Staff;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_subscription")
@Getter @Setter @NoArgsConstructor
public class MemberSubscription {

    public enum Status { ACTIVE, EXPIRED, CANCELLED, FROZEN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    /** Branch where it was sold. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id")
    private Offer offer;

    /** Copied from the plan at sale time: true = member may enter any branch. */
    @Column(name = "access_all_branches", nullable = false)
    private boolean accessAllBranches = false;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Regular plan price at the time of sale. */
    @Column(name = "list_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal listPrice;

    /** listPrice - offerPrice (zero when no offer). */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** What the member must pay (the offer price when an offer applies). */
    @Column(name = "amount_due", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountDue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    /** Staff member who persuaded the client (commission / bonus). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_by_staff_id")
    private Staff soldBy;

    /** Staff member who entered it in the system. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Staff createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(length = 255)
    private String note;
}
