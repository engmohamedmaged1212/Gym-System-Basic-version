package com.gymsystem.basic.branch;

import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.exception.DuplicateResourceException;
import com.gymsystem.basic.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchService {
    private final BranchRepository branchRepository;

    public BranchDto.Response create(BranchDto.Request req) {
        String name = req.name().trim();
        if (branchRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A branch with this name already exists");
        }
        Branch b = new Branch();
        apply(b, req, name);
        return BranchDto.Response.from(branchRepository.save(b));
    }

    public BranchDto.Response update(Long id, BranchDto.Request req) {
        Branch b = getEntity(id);
        String name = req.name().trim();
        if (branchRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("A branch with this name already exists");
        }
        apply(b, req, name);
        return BranchDto.Response.from(b);
    }

    public BranchDto.Response setActive(Long id, boolean active) {
        Branch b = getEntity(id);
        b.setActive(active);
        return BranchDto.Response.from(b);
    }

    @Transactional(readOnly = true)
    public BranchDto.Response get(Long id) {
        return BranchDto.Response.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<BranchDto.Response> search(String q, Boolean active, Pageable pageable) {
        return PageResponse.of(branchRepository.search(blankToNull(q), active, pageable).map(BranchDto.Response::from));
    }

    private Branch getEntity(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch " + id + " not found"));
    }

    private void apply(Branch b, BranchDto.Request req, String name) {
        b.setName(name);
        b.setAddress(req.address());
        b.setPhone(req.phone());
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
