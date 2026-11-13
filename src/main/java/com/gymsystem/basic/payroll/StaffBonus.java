package com.gymsystem.basic.payroll;

import com.gymsystem.basic.staff.Staff;
import com.gymsystem.basic.subscription.MemberSubscription;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Owner bonus, or bonus earned by persuading a client to subscribe. */
@Entity
@Table(name = "staff_bonus")
@Getter @Setter @NoArgsConstructor
public class StaffBonus {

    public enum ReasonType { OWNER_DISCRETIONARY, SUBSCRIPTION_SALE, KPI, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false)
    private ReasonType reasonType;

    /** Set for SUBSCRIPTION_SALE bonuses. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_subscription_id")
    private MemberSubscription memberSubscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "granted_by", nullable = false)
    private Staff grantedBy;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt = LocalDateTime.now();

    @Column(length = 255)
    private String note;

    /** Filled once the bonus is included in a payroll. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_id")
    private Payroll payroll;
}
