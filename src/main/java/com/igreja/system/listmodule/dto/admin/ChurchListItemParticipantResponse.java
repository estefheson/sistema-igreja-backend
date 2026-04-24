package com.igreja.system.listmodule.dto.admin;

import java.time.LocalDateTime;

public record ChurchListItemParticipantResponse(
        String fullName,
        String phone,
        Integer quantity,
        LocalDateTime createdAt
) {
}
