package com.igreja.system.room.dto;

import java.util.List;

public record RoomCreateRequest(
        String name,
        String description,
        Boolean active,
        Integer capacity,
        String usageRules,
        List<RoomPhotoRequest> photos,
        List<RoomReservationRuleRequest> reservationRules
) {
}
