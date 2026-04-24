package com.igreja.system.reservation.dto;

import com.igreja.system.reservation.entity.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record ReservationResponse(
        Long id,
        Long roomId,
        String roomName,
        String requesterName,
        Long requestedByUserId,
        String requestedByUsername,
        Long usingMinistryId,
        String usingMinistryName,
        List<Long> scheduleDemandMinistryIds,
        List<String> scheduleDemandMinistryNames,
        LocalDateTime createdAt,
        LocalDate reservationDate,
        LocalTime startTime,
        LocalTime endTime,
        String description,
        String cancelReason,
        ReservationStatus status,
        List<Long> ministryIds
) {
}
