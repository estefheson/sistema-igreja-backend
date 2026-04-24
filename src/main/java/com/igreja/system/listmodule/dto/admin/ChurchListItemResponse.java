package com.igreja.system.listmodule.dto.admin;

public record ChurchListItemResponse(
        Long id,
        Long listId,
        String name,
        String description,
        String imageUrl,
        Integer totalQuantity
) {
}
