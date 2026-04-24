package com.igreja.system.listmodule.dto;

import java.util.List;

public record PublicListSubmissionRequest(
        String fullName,
        String phone,
        List<PublicListSubmissionItemRequest> items
) {
}
