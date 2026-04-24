package com.igreja.system.scheduleneed.dto;

import com.igreja.system.reservation.entity.ReservationStatus;
import com.igreja.system.scheduleassignment.dto.ScheduleAssignmentResponse;
import com.igreja.system.scheduleneed.entity.ScheduleNeedStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ScheduleNeedResponse(
        Long id,
        Long reservationId,
        Long roomId,
        String roomName,
        String reservationDescription,
        Long ministryId,
        String ministryName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        ScheduleNeedStatus status,
        ReservationStatus reservationStatus,
        Boolean authenticatedMemberAssigned,
        Boolean authenticatedUserAssigned,
        List<ScheduleNeedAssignedMemberResponse> assignedMembers,
        List<ScheduleAssignmentResponse> assignments
) {
}
