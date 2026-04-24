package com.igreja.system.listmodule.repository.projection;

import java.time.LocalDateTime;

public record ListItemParticipantProjection(
        Long itemId,
        String fullName,
        String phone,
        Integer quantity,
        LocalDateTime createdAt
) {
}
