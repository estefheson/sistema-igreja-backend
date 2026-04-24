package com.igreja.system.ministry.dto;

public record MinistryUpdateRequest(
        String name,
        String description,
        Boolean active
) {
}
