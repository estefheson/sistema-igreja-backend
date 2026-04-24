package com.igreja.system.scheduleassignment.controller;

import com.igreja.system.scheduleassignment.dto.ScheduleAssignmentResponse;
import com.igreja.system.scheduleassignment.service.ScheduleAssignmentService;
import com.igreja.system.scheduleneed.entity.ScheduleNeedStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberScheduleAssignmentController {

    private final ScheduleAssignmentService scheduleAssignmentService;

    @GetMapping("/{id}/assignments")
    public List<ScheduleAssignmentResponse> findByMemberId(
            @PathVariable Long id,
            @RequestParam(required = false) ScheduleNeedStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return scheduleAssignmentService.findByMemberId(id, status, date);
    }
}
