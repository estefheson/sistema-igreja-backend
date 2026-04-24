package com.igreja.system.listmodule.dto.admin;

public record ChurchListItemCreateRequest(
        String name,
        String description,
        String imageUrl,
        Integer totalQuantity
) {
}
