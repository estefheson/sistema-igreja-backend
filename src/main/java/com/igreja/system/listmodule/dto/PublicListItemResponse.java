package com.igreja.system.listmodule.dto;

public record PublicListItemResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        Integer totalQuantity,
        Integer reservedQuantity,
        Integer availableQuantity
) {
}
