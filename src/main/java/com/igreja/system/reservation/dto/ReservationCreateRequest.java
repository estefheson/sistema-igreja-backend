package com.igreja.system.reservation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ReservationCreateRequest(
        Long roomId,
        @JsonAlias("date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate reservationDate,
        LocalTime startTime,
        LocalTime endTime,
        String description,
        Long usingMinistryId,
        List<Long> scheduleDemandMinistryIds
) {
}
