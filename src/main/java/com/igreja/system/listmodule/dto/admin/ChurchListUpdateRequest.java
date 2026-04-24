package com.igreja.system.listmodule.dto.admin;

import java.time.LocalDateTime;

public record ChurchListUpdateRequest(
        String name,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active
) {
}
