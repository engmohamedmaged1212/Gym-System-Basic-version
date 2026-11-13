package com.gymsystem.basic.gymclass;

import com.gymsystem.basic.branch.BranchRepository;
import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.exception.BusinessRuleException;
import com.gymsystem.basic.exception.FalseInputData;
import com.gymsystem.basic.exception.ResourceNotFoundException;
import com.gymsystem.basic.staff.Staff;
import com.gymsystem.basic.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class GymClassService {
    private final GymClassRepository classRepository;
    private final BranchRepository branchRepository;
    private final StaffRepository staffRepository;

    public GymClassDto.Response create(GymClassDto.Request req) {
        GymClass c = new GymClass();
        apply(c, req);
        return GymClassDto.Response.from(classRepository.save(c));
    }

    public GymClassDto.Response update(Long id, GymClassDto.Request req) {
        GymClass c = getEntity(id);
        if (c.getStatus() == GymClass.Status.DISCONTINUED) {
            throw new BusinessRuleException("A discontinued class cannot be edited");
        }
        apply(c, req);
        return GymClassDto.Response.from(c);
    }

    public GymClassDto.Response discontinue(Long id) {
        GymClass c = getEntity(id);
        if (c.getStatus() == GymClass.Status.DISCONTINUED) {
            throw new BusinessRuleException("This class is already discontinued");
        }
        c.setStatus(GymClass.Status.DISCONTINUED);
        c.setDiscontinuedAt(LocalDateTime.now());
        return GymClassDto.Response.from(c);
    }

    @Transactional(readOnly = true)
    public GymClassDto.Response get(Long id) {
        return GymClassDto.Response.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<GymClassDto.Response> search(String q, Long branchId, Long trainerId,
                                                     GymClass.Status status, Pageable pageable) {
        String query = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(classRepository.search(query, branchId, trainerId, status, pageable)
                .map(GymClassDto.Response::from));
    }

    private void apply(GymClass c, GymClassDto.Request req) {
        c.setBranch(branchRepository.findById(req.branchId())
                .orElseThrow(() -> new FalseInputData("Branch " + req.branchId() + " does not exist")));
        c.setName(req.name().trim());
        c.setDescription(req.description());
        if (req.trainerId() == null) {
            c.setTrainer(null);
            return;
        }
        Staff trainer = staffRepository.findById(req.trainerId())
                .orElseThrow(() -> new FalseInputData("Trainer " + req.trainerId() + " does not exist"));
        if (!"TRAINER".equals(trainer.getRole().getCode()) || trainer.getStatus() != Staff.Status.ACTIVE) {
            throw new FalseInputData("The selected employee is not an active trainer");
        }
        c.setTrainer(trainer);
    }

    private GymClass getEntity(Long id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class " + id + " not found"));
    }
}
