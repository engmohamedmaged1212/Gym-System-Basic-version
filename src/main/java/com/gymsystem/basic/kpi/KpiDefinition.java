package com.gymsystem.basic.kpi;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kpi_definition")
@Getter @Setter @NoArgsConstructor
public class KpiDefinition {

    /** AUTO = computed from data, MANUAL = entered by owner/manager. */
    public enum Source { AUTO, MANUAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    /** count, EGP, hours, %, score... */
    @Column(nullable = false, length = 30)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source = Source.AUTO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
