package com.gymsystem.basic.staff;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class UserPermissionId implements Serializable {

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "permission_id")
    private Short permissionId;
}
