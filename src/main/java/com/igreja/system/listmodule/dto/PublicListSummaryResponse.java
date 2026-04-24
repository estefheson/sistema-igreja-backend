package com.igreja.system.listmodule.dto;

import java.time.LocalDateTime;

public record PublicListSummaryResponse(
        Long id,
        String name,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {
}
