package com.gymsystem.basic.attendance;

import com.gymsystem.basic.branch.Branch;
import com.gymsystem.basic.common.ScanMethod;
import com.gymsystem.basic.member.Member;
import com.gymsystem.basic.staff.Staff;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_attendance")
@Getter @Setter @NoArgsConstructor
public class MemberAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "checkin_at", nullable = false)
    private LocalDateTime checkinAt = LocalDateTime.now();

    /** Optional - check-in is what matters. */
    @Column(name = "checkout_at")
    private LocalDateTime checkoutAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScanMethod method = ScanMethod.QR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_staff_id")
    private Staff recordedBy;
}
