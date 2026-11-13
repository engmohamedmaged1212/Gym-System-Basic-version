package com.gymsystem.basic.member;

import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.config.security.Authz;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    /**
     * POST /api/v1/members        (permission: MEMBER_MANAGE)
     *
     * Request:
     * {
     *   "branchId": 1,
     *   "fullName": "Omar Khaled",
     *   "phone": "01234567890",
     *   "email": "omar@mail.com",
     *   "birthDate": "1998-04-12",
     *   "gender": "MALE",
     *   "notes": "Knee injury, avoid heavy squats"
     * }
     * (branchId, email, birthDate, gender and notes are optional)
     *
     * Response 201:
     * {
     *   "id": 17, "branchId": 1, "branchName": "Main Branch", "fullName": "Omar Khaled",
     *   "phone": "01234567890", "email": "omar@mail.com", "birthDate": "1998-04-12", "gender": "MALE",
     *   "qrToken": "6d0c1f0e-1b7e-4c7a-8d41-0d1f0b3a2c55", "joinDate": "2026-11-13", "status": "ACTIVE",
     *   "notes": "Knee injury, avoid heavy squats", "createdAt": "2026-11-13T10:20:00"
     * }
     *
     * Response 409: { "error": "A member with this phone number already exists" }
     * Response 400: { "phone": "Phone must be 8-15 digits" }
     */
    @PostMapping
    @PreAuthorize(Authz.MEMBER_MANAGE)
    public ResponseEntity<MemberDto.Response> create(@RequestBody @Valid MemberDto.CreateRequest req,
                                                     @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.create(req, actor));
    }

    /**
     * GET /api/v1/members?q=omar&status=ACTIVE&branchId=1&page=0&size=20
     * (permission: MEMBER_MANAGE, SUBSCRIPTION_SELL or ATTENDANCE_RECORD)
     * "q" matches the beginning of the name, phone or email.
     *
     * Response 200:
     * {
     *   "content": [ { "id": 17, "fullName": "Omar Khaled", "phone": "01234567890", "status": "ACTIVE", ... } ],
     *   "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
     * }
     */
    @GetMapping
    @PreAuthorize(Authz.MEMBER_READ)
    public PageResponse<MemberDto.Response> search(@RequestParam(required = false) String q,
                                                   @RequestParam(required = false) Member.Status status,
                                                   @RequestParam(required = false) Long branchId,
                                                   @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
        return memberService.search(q, status, branchId, pageable);
    }

    /**
     * GET /api/v1/members/17        (permission: MEMBER_MANAGE, SUBSCRIPTION_SELL or ATTENDANCE_RECORD)
     *
     * Response 200: one member (same shape as the create response)
     * Response 404: { "error": "Member 17 not found" }
     */
    @GetMapping("/{id}")
    @PreAuthorize(Authz.MEMBER_READ)
    public MemberDto.Response get(@PathVariable Long id) {
        return memberService.get(id);
    }

    /**
     * PUT /api/v1/members/17        (permission: MEMBER_MANAGE)
     *
     * Request:
     * {
     *   "branchId": 1, "fullName": "Omar K. Khaled", "phone": "01234567890", "email": "omar@mail.com",
     *   "birthDate": "1998-04-12", "gender": "MALE", "notes": "Knee injury healed"
     * }
     *
     * Response 200: the updated member (same shape as the create response)
     */
    @PutMapping("/{id}")
    @PreAuthorize(Authz.MEMBER_MANAGE)
    public MemberDto.Response update(@PathVariable Long id, @RequestBody @Valid MemberDto.UpdateRequest req) {
        return memberService.update(id, req);
    }

    /**
     * PATCH /api/v1/members/17/status        (permission: MEMBER_MANAGE)
     *
     * Request:  { "status": "BLOCKED" }       (ACTIVE | INACTIVE | BLOCKED)
     * Response 200: the updated member (same shape as the create response)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize(Authz.MEMBER_MANAGE)
    public MemberDto.Response changeStatus(@PathVariable Long id, @RequestBody @Valid MemberDto.StatusRequest req) {
        return memberService.changeStatus(id, req.status());
    }

    /**
     * GET /api/v1/members/17/qr        (permission: MEMBER_MANAGE)
     * The app turns "qrToken" into the QR image printed on the member card.
     *
     * Response 200: { "memberId": 17, "qrToken": "6d0c1f0e-1b7e-4c7a-8d41-0d1f0b3a2c55" }
     */
    @GetMapping("/{id}/qr")
    @PreAuthorize(Authz.MEMBER_MANAGE)
    public MemberDto.QrResponse getQr(@PathVariable Long id) {
        return memberService.getQr(id);
    }

    /**
     * POST /api/v1/members/17/qr/regenerate        (permission: MEMBER_MANAGE)   - when a card is lost
     *
     * Response 200: { "memberId": 17, "qrToken": "a new uuid" }
     */
    @PostMapping("/{id}/qr/regenerate")
    @PreAuthorize(Authz.MEMBER_MANAGE)
    public MemberDto.QrResponse regenerateQr(@PathVariable Long id) {
        return memberService.regenerateQr(id);
    }
}
