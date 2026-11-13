package com.gymsystem.basic.staff;

import com.gymsystem.basic.config.security.Authz;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only lists used by the "add employee" and "permissions" screens. */
@RestController
@RequestMapping("/api/v1/access")
@RequiredArgsConstructor
public class AccessController {
    private final StaffService staffService;

    /**
     * GET /api/v1/access/roles        (permission: STAFF_MANAGE)
     *
     * Response 200:
     * [
     *   { "id": 3, "code": "TRAINER", "name": "Trainer", "permissions": ["ATTENDANCE_RECORD", "SUBSCRIPTION_SELL"] },
     *   { "id": 4, "code": "RECEPTION", "name": "Reception", "permissions": ["MEMBER_MANAGE", "PAYMENT_RECORD"] }
     * ]
     */
    @GetMapping("/roles")
    @PreAuthorize(Authz.STAFF_MANAGE)
    public List<StaffDto.RoleResponse> roles() {
        return staffService.listRoles();
    }

    /**
     * GET /api/v1/access/permissions        (permission: STAFF_MANAGE)
     *
     * Response 200:
     * [ { "id": 4, "code": "ATTENDANCE_RECORD", "description": "Record member attendance manually" }, ... ]
     */
    @GetMapping("/permissions")
    @PreAuthorize(Authz.STAFF_MANAGE)
    public List<StaffDto.PermissionResponse> permissions() {
        return staffService.listPermissions();
    }
}
