package com.igreja.system.listmodule.dto;

public record PublicListSubmissionConfirmedItemResponse(
        Long itemId,
        String itemName,
        Integer quantity
) {
}
