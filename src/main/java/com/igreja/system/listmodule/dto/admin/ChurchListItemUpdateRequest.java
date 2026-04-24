package com.igreja.system.listmodule.dto.admin;

public record ChurchListItemUpdateRequest(
        String name,
        String description,
        String imageUrl,
        Integer totalQuantity
) {
}
