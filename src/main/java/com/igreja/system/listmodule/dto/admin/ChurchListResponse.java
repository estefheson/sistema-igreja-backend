package com.igreja.system.listmodule.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

public record ChurchListResponse(
        Long id,
        String name,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active,
        String status,
        List<ChurchListItemResponse> items
) {
}
