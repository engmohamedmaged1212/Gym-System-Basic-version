package com.gymsystem.basic.payment;

import com.gymsystem.basic.staff.Staff;
import com.gymsystem.basic.subscription.MemberSubscription;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Payment or refund of a subscription. Amount is always positive; type gives the direction. */
@Entity
@Table(name = "payment")
@Getter @Setter @NoArgsConstructor
public class Payment {

    public enum Type { PAYMENT, REFUND }

    public enum Method { CASH, CARD, TRANSFER, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_subscription_id", nullable = false)
    private MemberSubscription memberSubscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type = Type.PAYMENT;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Method method = Method.CASH;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "received_by", nullable = false)
    private Staff receivedBy;

    @Column(length = 255)
    private String note;
}
