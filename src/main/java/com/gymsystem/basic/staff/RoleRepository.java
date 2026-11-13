package com.gymsystem.basic.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Short> {

    Optional<Role> findByCode(String code);

    @Query("select distinct r from Role r left join fetch r.permissions order by r.id")
    List<Role> findAllWithPermissions();
}
