package com.igreja.system.reservation.dto;

import java.time.LocalDate;

public record ReservationCalendarSummaryResponse(
        LocalDate date,
        long approvedCount,
        long totalCount
) {
}
