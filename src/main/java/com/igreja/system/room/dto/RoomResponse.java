package com.igreja.system.room.dto;

import java.util.List;

public record RoomResponse(
        Long id,
        String name,
        String description,
        Boolean active,
        Integer capacity,
        String usageRules,
        List<RoomPhotoResponse> photos,
        List<RoomReservationRuleResponse> reservationRules
) {
}
