package com.igreja.system.listmodule.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

public record ChurchListManagementResponse(
        Long id,
        String name,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active,
        String status,
        Integer totalItems,
        Integer totalReserved,
        Integer totalAvailable,
        Integer totalParticipants,
        Double fillPercentage,
        List<ChurchListManagementItemResponse> items
) {

    public ChurchListManagementResponse {
        List<ChurchListManagementItemResponse> normalizedItems = items == null ? List.of() : List.copyOf(items);
        int normalizedTotalItems = normalizedItems.size();
        int normalizedTotalReserved = normalizedItems.stream()
                .map(ChurchListManagementItemResponse::reservedQuantity)
                .mapToInt(value -> value != null ? value : 0)
                .sum();
        int normalizedTotalAvailable = normalizedItems.stream()
                .map(ChurchListManagementItemResponse::availableQuantity)
                .mapToInt(value -> value != null ? value : 0)
                .sum();
        int normalizedTotalQuantity = normalizedItems.stream()
                .map(ChurchListManagementItemResponse::totalQuantity)
                .mapToInt(value -> value != null ? value : 0)
                .sum();

        totalItems = normalizedTotalItems;
        totalReserved = normalizedTotalReserved;
        totalAvailable = normalizedTotalAvailable;
        totalParticipants = Math.max(totalParticipants != null ? totalParticipants : 0, 0);
        fillPercentage = calculateFillPercentage(normalizedTotalReserved, normalizedTotalQuantity);
        items = normalizedItems;
    }

    private static double calculateFillPercentage(int reservedQuantity, int totalQuantity) {
        if (totalQuantity <= 0) {
            return 0D;
        }

        double percentage = (reservedQuantity * 100.0) / totalQuantity;
        return Math.round(percentage * 100.0) / 100.0;
    }
}
