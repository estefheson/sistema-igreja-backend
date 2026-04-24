package com.igreja.system.scheduleneed.controller;

import com.igreja.system.scheduleneed.dto.ScheduleNeedResponse;
import com.igreja.system.scheduleneed.entity.ScheduleNeedStatus;
import com.igreja.system.scheduleneed.service.ScheduleNeedService;
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
@RequestMapping("/api/schedule-needs")
@RequiredArgsConstructor
public class ScheduleNeedController {

    private final ScheduleNeedService scheduleNeedService;

    @GetMapping
    public List<ScheduleNeedResponse> findAll(
            @RequestParam(required = false) ScheduleNeedStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return scheduleNeedService.findAll(status, date);
    }

    @GetMapping("/{id}")
    public ScheduleNeedResponse findById(@PathVariable Long id) {
        return scheduleNeedService.findById(id);
    }
}
