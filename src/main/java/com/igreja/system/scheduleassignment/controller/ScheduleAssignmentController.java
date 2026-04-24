package com.igreja.system.scheduleassignment.controller;

import com.igreja.system.scheduleassignment.dto.ScheduleAssignmentCreateRequest;
import com.igreja.system.scheduleassignment.dto.ScheduleAssignmentResponse;
import com.igreja.system.scheduleassignment.service.ScheduleAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedule-needs")
@RequiredArgsConstructor
public class ScheduleAssignmentController {

    private final ScheduleAssignmentService scheduleAssignmentService;

    @GetMapping("/{id}/assignments")
    public List<ScheduleAssignmentResponse> findByScheduleNeedId(@PathVariable Long id) {
        return scheduleAssignmentService.findByScheduleNeedId(id);
    }

    @PostMapping("/{id}/assignments")
    public List<ScheduleAssignmentResponse> createMany(
            @PathVariable Long id,
            @RequestBody ScheduleAssignmentCreateRequest request
    ) {
        return scheduleAssignmentService.createMany(id, request);
    }

    @PostMapping("/{id}/assignments/{memberId}")
    public ScheduleAssignmentResponse create(@PathVariable Long id, @PathVariable Long memberId) {
        return scheduleAssignmentService.create(id, memberId);
    }

    @DeleteMapping("/{id}/assignments/{memberId}")
    public ScheduleAssignmentResponse delete(@PathVariable Long id, @PathVariable Long memberId) {
        return scheduleAssignmentService.delete(id, memberId);
    }
}
