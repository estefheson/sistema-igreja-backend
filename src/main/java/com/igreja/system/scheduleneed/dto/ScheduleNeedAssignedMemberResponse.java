package com.igreja.system.scheduleneed.dto;

import java.time.LocalDateTime;

public record ScheduleNeedAssignedMemberResponse(
        Long assignmentId,
        Long memberId,
        String fullName,
        String cpf,
        LocalDateTime assignedAt,
        Long assignedByUserId,
        String assignedByUsername,
        String assignedByName
) {
}
