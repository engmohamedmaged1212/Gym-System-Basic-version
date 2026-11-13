package com.gymsystem.basic.kpi;

import com.gymsystem.basic.staff.Staff;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "kpi_target",
        uniqueConstraints = @UniqueConstraint(name = "uq_kpi_target",
                columnNames = {"kpi_id", "staff_id", "period_month"}))
@Getter @Setter @NoArgsConstructor
public class KpiTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kpi_id", nullable = false)
    private KpiDefinition kpi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    /** First day of the month. */
    @Column(name = "period_month", nullable = false)
    private LocalDate periodMonth;

    @Column(name = "target_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal targetValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_by", nullable = false)
    private Staff setBy;
}
