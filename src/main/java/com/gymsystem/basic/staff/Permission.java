package com.gymsystem.basic.staff;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permission")
@Getter @Setter @NoArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    /** e.g. MEMBER_MANAGE, BONUS_GRANT, PAYROLL_MANAGE */
    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(length = 255)
    private String description;
}
