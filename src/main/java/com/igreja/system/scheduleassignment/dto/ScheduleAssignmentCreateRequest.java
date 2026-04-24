package com.igreja.system.scheduleassignment.dto;

import java.util.List;

public record ScheduleAssignmentCreateRequest(
        List<Long> memberIds
) {
}
