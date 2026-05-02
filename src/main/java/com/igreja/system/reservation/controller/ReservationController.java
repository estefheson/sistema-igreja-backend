package com.igreja.system.reservation.controller;

import com.igreja.system.reservation.dto.ReservationCreateRequest;
import com.igreja.system.reservation.dto.ReservationCancelRequest;
import com.igreja.system.reservation.dto.ReservationCalendarSummaryResponse;
import com.igreja.system.reservation.dto.ReservationResponse;
import com.igreja.system.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ReservationResponse create(@RequestBody ReservationCreateRequest request) {
        return reservationService.create(request);
    }

    @GetMapping
    public List<ReservationResponse> findAll(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return reservationService.findAll(roomId, startDate, endDate);
    }

    @GetMapping("/calendar-summary")
    public List<ReservationCalendarSummaryResponse> findCalendarSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return reservationService.findCalendarSummary(startDate, endDate);
    }

    @GetMapping("/pending")
    public List<ReservationResponse> findPending() {
        return reservationService.findPending();
    }

    @GetMapping("/{id}")
    public ReservationResponse findById(@PathVariable Long id) {
        return reservationService.findById(id);
    }

    @PatchMapping("/{id}/approve")
    public ReservationResponse approve(@PathVariable Long id) {
        return reservationService.approve(id);
    }

    @PatchMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable Long id, @RequestBody ReservationCancelRequest request) {
        return reservationService.cancel(id, request);
    }
}
