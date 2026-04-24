package com.igreja.system.listmodule.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PublicListDetailResponse(
        Long id,
        String name,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        List<PublicListItemResponse> items
) {
}
