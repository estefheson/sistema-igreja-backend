package com.igreja.system.scheduleassignment.controller;

import com.igreja.system.scheduleneed.dto.ScheduleNeedResponse;
import com.igreja.system.scheduleneed.service.ScheduleNeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MyServiceScaleController {

    private final ScheduleNeedService scheduleNeedService;

    @GetMapping({"/api/my-service-scales", "/api/schedule-assignments/me", "/api/my-service-agenda"})
    public List<ScheduleNeedResponse> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return scheduleNeedService.findMyServiceAgenda(startDate, endDate);
    }
}
