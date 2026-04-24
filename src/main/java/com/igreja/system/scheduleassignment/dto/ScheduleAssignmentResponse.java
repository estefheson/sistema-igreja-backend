package com.igreja.system.scheduleassignment.dto;

import com.igreja.system.reservation.entity.ReservationStatus;
import com.igreja.system.scheduleneed.entity.ScheduleNeedStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ScheduleAssignmentResponse(
        Long id,
        Long scheduleNeedId,
        Long reservationId,
        Long roomId,
        String roomName,
        Long ministryId,
        String ministryName,
        Long memberId,
        String memberName,
        String memberCpf,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String reservationDescription,
        ScheduleNeedStatus scheduleNeedStatus,
        ReservationStatus reservationStatus,
        LocalDateTime assignedAt,
        Long assignedByUserId,
        String assignedByUsername,
        String assignedByName
) {
}
