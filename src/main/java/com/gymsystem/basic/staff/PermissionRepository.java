package com.gymsystem.basic.staff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Short> {

    List<Permission> findByCodeIn(Collection<String> codes);

    List<Permission> findAllByOrderByCodeAsc();
}
