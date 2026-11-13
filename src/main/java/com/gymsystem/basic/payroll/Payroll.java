package com.gymsystem.basic.payroll;

import com.gymsystem.basic.staff.Staff;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One row per staff member per month (unique staff_id + period_month). */
@Entity
@Table(name = "payroll")
@Getter @Setter @NoArgsConstructor
public class Payroll {

    public enum Status { DRAFT, APPROVED, PAID }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    /** First day of the month. */
    @Column(name = "period_month", nullable = false)
    private LocalDate periodMonth;

    @Column(name = "hours_worked", nullable = false, precision = 7, scale = 2)
    private BigDecimal hoursWorked = BigDecimal.ZERO;

    @Column(name = "hourly_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyAmount = BigDecimal.ZERO;

    @Column(name = "base_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAmount = BigDecimal.ZERO;

    @Column(name = "commission_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "bonus_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal bonusAmount = BigDecimal.ZERO;

    @Column(name = "deduction_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal deductionAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @Column(name = "deduction_note", length = 255)
    private String deductionNote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by", nullable = false)
    private Staff generatedBy;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
