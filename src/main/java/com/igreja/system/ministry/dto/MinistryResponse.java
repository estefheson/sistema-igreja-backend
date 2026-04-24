package com.igreja.system.ministry.dto;

public record MinistryResponse(
        Long id,
        String name,
        String description,
        Boolean active
) {
}
