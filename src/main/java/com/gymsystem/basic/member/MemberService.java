package com.gymsystem.basic.member;

import com.gymsystem.basic.branch.Branch;
import com.gymsystem.basic.branch.BranchRepository;
import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.exception.DuplicateResourceException;
import com.gymsystem.basic.exception.FalseInputData;
import com.gymsystem.basic.exception.ResourceNotFoundException;
import com.gymsystem.basic.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {
    private final MemberRepository memberRepository;
    private final BranchRepository branchRepository;
    private final StaffRepository staffRepository;

    public MemberDto.Response create(MemberDto.CreateRequest req, AuthenticatedUser actor) {
        String phone = req.phone().trim();
        if (memberRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("A member with this phone number already exists");
        }
        Member m = new Member();
        m.setBranch(findBranch(req.branchId() != null ? req.branchId() : actor.branchId()));
        m.setFullName(req.fullName().trim());
        m.setPhone(phone);
        m.setEmail(blankToNull(req.email()));
        m.setBirthDate(req.birthDate());
        m.setGender(req.gender());
        m.setNotes(req.notes());
        m.setCreatedBy(staffRepository.getReferenceById(actor.staffId()));
        return MemberDto.Response.from(memberRepository.save(m));
    }

    public MemberDto.Response update(Long id, MemberDto.UpdateRequest req) {
        Member m = getEntity(id);
        String phone = req.phone().trim();
        if (memberRepository.existsByPhoneAndIdNot(phone, id)) {
            throw new DuplicateResourceException("A member with this phone number already exists");
        }
        if (req.branchId() != null && !req.branchId().equals(m.getBranch().getId())) {
            m.setBranch(findBranch(req.branchId()));
        }
        m.setFullName(req.fullName().trim());
        m.setPhone(phone);
        m.setEmail(blankToNull(req.email()));
        m.setBirthDate(req.birthDate());
        m.setGender(req.gender());
        m.setNotes(req.notes());
        return MemberDto.Response.from(m);
    }

    public MemberDto.Response changeStatus(Long id, Member.Status status) {
        Member m = getEntity(id);
        m.setStatus(status);
        return MemberDto.Response.from(m);
    }

    @Transactional(readOnly = true)
    public MemberDto.Response get(Long id) {
        return MemberDto.Response.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberDto.Response> search(String q, Member.Status status, Long branchId, Pageable pageable) {
        return PageResponse.of(memberRepository.search(blankToNull(q), status, branchId, pageable)
                .map(MemberDto.Response::from));
    }

    @Transactional(readOnly = true)
    public MemberDto.QrResponse getQr(Long id) {
        Member m = getEntity(id);
        return new MemberDto.QrResponse(m.getId(), m.getQrToken());
    }

    public MemberDto.QrResponse regenerateQr(Long id) {
        Member m = getEntity(id);
        m.setQrToken(UUID.randomUUID().toString());
        return new MemberDto.QrResponse(m.getId(), m.getQrToken());
    }

    private Member getEntity(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member " + id + " not found"));
    }

    private Branch findBranch(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new FalseInputData("Branch " + id + " does not exist"));
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
