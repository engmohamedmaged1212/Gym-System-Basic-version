package com.gymsystem.basic.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserPermissionRepository extends JpaRepository<UserPermission, UserPermissionId> {

    @Query("select up from UserPermission up join fetch up.permission where up.staff.id = :staffId")
    List<UserPermission> findByStaffId(@Param("staffId") Long staffId);

    @Query("select up.permission.code from UserPermission up where up.staff.id = :staffId")
    List<String> findPermissionCodesByStaffId(@Param("staffId") Long staffId);
}
