package com.igreja.system.listmodule.dto.admin;

import java.util.List;

public record ChurchListManagementItemResponse(
        Long id,
        Long listId,
        String name,
        String description,
        String imageUrl,
        Integer totalQuantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        Double fillPercentage,
        List<ChurchListItemParticipantResponse> participants
) {

    public ChurchListManagementItemResponse {
        int normalizedTotalQuantity = Math.max(totalQuantity != null ? totalQuantity : 0, 0);
        int normalizedReservedQuantity = Math.max(reservedQuantity != null ? reservedQuantity : 0, 0);

        if (normalizedReservedQuantity > normalizedTotalQuantity) {
            normalizedReservedQuantity = normalizedTotalQuantity;
        }

        totalQuantity = normalizedTotalQuantity;
        reservedQuantity = normalizedReservedQuantity;
        availableQuantity = Math.max(normalizedTotalQuantity - normalizedReservedQuantity, 0);
        fillPercentage = calculateFillPercentage(normalizedReservedQuantity, normalizedTotalQuantity);
        participants = participants == null ? List.of() : List.copyOf(participants);
    }

    private static double calculateFillPercentage(int reservedQuantity, int totalQuantity) {
        if (totalQuantity <= 0) {
            return 0D;
        }

        double percentage = (reservedQuantity * 100.0) / totalQuantity;
        return Math.round(percentage * 100.0) / 100.0;
    }
}
