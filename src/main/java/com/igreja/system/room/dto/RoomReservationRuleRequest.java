package com.igreja.system.room.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record RoomReservationRuleRequest(
        DayOfWeek dayOfWeek,
        Boolean enabled,
        LocalTime startTime,
        LocalTime endTime
) {
}
