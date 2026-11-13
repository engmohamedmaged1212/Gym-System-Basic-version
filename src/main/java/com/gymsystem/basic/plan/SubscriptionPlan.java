package com.gymsystem.basic.plan;

import com.gymsystem.basic.gymclass.GymClass;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/** Gym only, gym + classes, or classes only (includesGym + classes). */
@Entity
@Table(name = "subscription_plan")
@Getter @Setter @NoArgsConstructor
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Fixed bonus paid to the staff member who sells this plan (0 = none). */
    @Column(name = "sale_bonus_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal saleBonusAmount = BigDecimal.ZERO;

    @Column(name = "includes_gym", nullable = false)
    private boolean includesGym = true;

    /** Usable in every branch? */
    @Column(name = "all_branches", nullable = false)
    private boolean allBranches = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "subscription_plan_class",
            joinColumns = @JoinColumn(name = "plan_id"),
            inverseJoinColumns = @JoinColumn(name = "class_id"))
    private Set<GymClass> classes = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
