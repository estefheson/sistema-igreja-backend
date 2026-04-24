package com.igreja.system.room.dto;

public record RoomPhotoResponse(
        Long id,
        String imageUrl,
        Integer displayOrder
) {
}
