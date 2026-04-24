package com.igreja.system.ministry.dto;

public record MinistryCreateRequest(
        String name,
        String description,
        Boolean active
) {
}
