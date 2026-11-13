package com.gymsystem.basic.shift;

import com.gymsystem.basic.branch.Branch;
import com.gymsystem.basic.common.ScanMethod;
import com.gymsystem.basic.staff.Staff;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Clock-in / clock-out shift; hours worked drive hourly pay. clockOutAt == null means still open. */
@Entity
@Table(name = "staff_shift")
@Getter @Setter @NoArgsConstructor
public class StaffShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    /** Branch where the staff member actually worked. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "clock_in_at", nullable = false)
    private LocalDateTime clockInAt;

    @Column(name = "clock_out_at")
    private LocalDateTime clockOutAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "clock_in_method", nullable = false)
    private ScanMethod clockInMethod = ScanMethod.QR;

    @Enumerated(EnumType.STRING)
    @Column(name = "clock_out_method")
    private ScanMethod clockOutMethod;

    /** Generated column computed by MySQL - read only. */
    @Column(name = "minutes_worked", insertable = false, updatable = false)
    private Integer minutesWorked;

    /** Manager who corrected the times, if any. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by")
    private Staff editedBy;

    @Column(length = 255)
    private String note;
}
