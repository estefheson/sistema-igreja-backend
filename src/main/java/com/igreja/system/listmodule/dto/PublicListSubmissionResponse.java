package com.igreja.system.listmodule.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PublicListSubmissionResponse(
        Long submissionId,
        Long listId,
        String listName,
        String fullName,
        String phone,
        LocalDateTime createdAt,
        List<PublicListSubmissionConfirmedItemResponse> items
) {
}
