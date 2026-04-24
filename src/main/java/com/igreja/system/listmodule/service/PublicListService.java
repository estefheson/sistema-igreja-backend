package com.igreja.system.listmodule.service;

import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.listmodule.dto.PublicListDetailResponse;
import com.igreja.system.listmodule.dto.PublicListItemResponse;
import com.igreja.system.listmodule.dto.PublicListSubmissionConfirmedItemResponse;
import com.igreja.system.listmodule.dto.PublicListSubmissionItemRequest;
import com.igreja.system.listmodule.dto.PublicListSubmissionRequest;
import com.igreja.system.listmodule.dto.PublicListSubmissionResponse;
import com.igreja.system.listmodule.dto.PublicListSummaryResponse;
import com.igreja.system.listmodule.entity.ChurchList;
import com.igreja.system.listmodule.entity.ChurchListItem;
import com.igreja.system.listmodule.entity.PublicListSubmission;
import com.igreja.system.listmodule.entity.PublicListSubmissionItem;
import com.igreja.system.listmodule.repository.ChurchListItemRepository;
import com.igreja.system.listmodule.repository.ChurchListRepository;
import com.igreja.system.listmodule.repository.PublicListSubmissionItemRepository;
import com.igreja.system.listmodule.repository.PublicListSubmissionRepository;
import com.igreja.system.listmodule.repository.projection.ListItemReservedQuantityProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicListService {

    private final ChurchListRepository churchListRepository;
    private final ChurchListItemRepository churchListItemRepository;
    private final PublicListSubmissionRepository publicListSubmissionRepository;
    private final PublicListSubmissionItemRepository publicListSubmissionItemRepository;

    public List<PublicListSummaryResponse> findAvailableLists() {
        LocalDateTime now = LocalDateTime.now();

        return churchListRepository.findAllPubliclyAvailable(now)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public PublicListDetailResponse findAvailableListById(Long id) {
        ChurchList list = findAvailableList(id);
        Map<Long, Integer> reservedQuantitiesByItemId = loadReservedQuantitiesByItemId(list.getId());

        return toDetailResponse(list, reservedQuantitiesByItemId);
    }

    @Transactional
    public PublicListSubmissionResponse createSubmission(Long listId, PublicListSubmissionRequest request) {
        ChurchList list = findAvailableList(listId);
        String fullName = normalizeRequiredText(request != null ? request.fullName() : null, "Nome completo e obrigatorio");
        String phone = normalizeRequiredText(request != null ? request.phone() : null, "Telefone e obrigatorio");
        LinkedHashMap<Long, Integer> requestedQuantitiesByItemId = normalizeRequestedItems(request != null ? request.items() : null);

        List<Long> requestedItemIds = new ArrayList<>(requestedQuantitiesByItemId.keySet());
        List<ChurchListItem> lockedItems = churchListItemRepository.findAllByListIdAndIdInForUpdate(listId, requestedItemIds);

        if (lockedItems.size() != requestedItemIds.size()) {
            throw new BusinessException("Um ou mais itens informados nao pertencem a lista");
        }

        Map<Long, ChurchListItem> itemsById = lockedItems.stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), LinkedHashMap::putAll);

        Map<Long, Integer> reservedQuantitiesByItemId = loadReservedQuantitiesByItemId(listId, requestedItemIds);

        for (Map.Entry<Long, Integer> entry : requestedQuantitiesByItemId.entrySet()) {
            ChurchListItem item = itemsById.get(entry.getKey());
            int reservedQuantity = reservedQuantitiesByItemId.getOrDefault(item.getId(), 0);
            int availableQuantity = item.getTotalQuantity() - reservedQuantity;

            if (entry.getValue() > availableQuantity) {
                throw new BusinessException("Quantidade solicitada para o item '" + item.getName() + "' excede o saldo disponivel");
            }
        }

        PublicListSubmission submission = PublicListSubmission.builder()
                .list(list)
                .fullName(fullName)
                .phone(phone)
                .build();

        Set<PublicListSubmissionItem> submissionItems = new LinkedHashSet<>();

        for (Map.Entry<Long, Integer> entry : requestedQuantitiesByItemId.entrySet()) {
            ChurchListItem item = itemsById.get(entry.getKey());

            submissionItems.add(PublicListSubmissionItem.builder()
                    .submission(submission)
                    .listItem(item)
                    .quantity(entry.getValue())
                    .build());
        }

        submission.setItems(submissionItems);

        PublicListSubmission savedSubmission = publicListSubmissionRepository.save(submission);
        publicListSubmissionItemRepository.flush();

        return toSubmissionResponse(savedSubmission);
    }

    private ChurchList findAvailableList(Long id) {
        return churchListRepository.findPublicDetailedById(id, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("Lista publica nao encontrada ou fora do periodo de participacao"));
    }

    private LinkedHashMap<Long, Integer> normalizeRequestedItems(List<PublicListSubmissionItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("Informe pelo menos um item para confirmar a participacao");
        }

        LinkedHashMap<Long, Integer> quantitiesByItemId = new LinkedHashMap<>();

        for (PublicListSubmissionItemRequest item : items) {
            if (item == null) {
                throw new BusinessException("Lista de itens nao aceita valores nulos");
            }

            if (item.itemId() == null) {
                throw new BusinessException("Item da lista e obrigatorio");
            }

            if (item.quantity() == null || item.quantity() < 1) {
                throw new BusinessException("Quantidade deve ser maior ou igual a 1");
            }

            if (quantitiesByItemId.putIfAbsent(item.itemId(), item.quantity()) != null) {
                throw new BusinessException("Nao e permitido informar o mesmo item mais de uma vez");
            }
        }

        return quantitiesByItemId;
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        if (value == null) {
            throw new BusinessException(errorMessage);
        }

        String normalizedValue = value.trim();

        if (normalizedValue.isBlank()) {
            throw new BusinessException(errorMessage);
        }

        return normalizedValue;
    }

    private Map<Long, Integer> loadReservedQuantitiesByItemId(Long listId) {
        return toReservedQuantityMap(churchListItemRepository.findReservedQuantitiesByListId(listId));
    }

    private Map<Long, Integer> loadReservedQuantitiesByItemId(Long listId, List<Long> itemIds) {
        return toReservedQuantityMap(churchListItemRepository.findReservedQuantitiesByListIdAndItemIds(listId, itemIds));
    }

    private Map<Long, Integer> toReservedQuantityMap(List<ListItemReservedQuantityProjection> projections) {
        Map<Long, Integer> reservedQuantitiesByItemId = new LinkedHashMap<>();

        for (ListItemReservedQuantityProjection projection : projections) {
            reservedQuantitiesByItemId.put(
                    projection.itemId(),
                    projection.reservedQuantity() != null ? projection.reservedQuantity().intValue() : 0
            );
        }

        return reservedQuantitiesByItemId;
    }

    private PublicListSummaryResponse toSummaryResponse(ChurchList list) {
        return new PublicListSummaryResponse(
                list.getId(),
                list.getName(),
                list.getDescription(),
                list.getStartsAt(),
                list.getEndsAt()
        );
    }

    private PublicListDetailResponse toDetailResponse(ChurchList list, Map<Long, Integer> reservedQuantitiesByItemId) {
        List<PublicListItemResponse> items = list.getItems()
                .stream()
                .sorted(Comparator.comparing(ChurchListItem::getId))
                .map(item -> toItemResponse(item, reservedQuantitiesByItemId))
                .toList();

        return new PublicListDetailResponse(
                list.getId(),
                list.getName(),
                list.getDescription(),
                list.getStartsAt(),
                list.getEndsAt(),
                items
        );
    }

    private PublicListItemResponse toItemResponse(ChurchListItem item, Map<Long, Integer> reservedQuantitiesByItemId) {
        int reservedQuantity = reservedQuantitiesByItemId.getOrDefault(item.getId(), 0);
        int availableQuantity = Math.max(item.getTotalQuantity() - reservedQuantity, 0);

        return new PublicListItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getImageUrl(),
                item.getTotalQuantity(),
                reservedQuantity,
                availableQuantity
        );
    }

    private PublicListSubmissionResponse toSubmissionResponse(PublicListSubmission submission) {
        List<PublicListSubmissionConfirmedItemResponse> items = submission.getItems()
                .stream()
                .sorted(Comparator.comparing(PublicListSubmissionItem::getId))
                .map(item -> new PublicListSubmissionConfirmedItemResponse(
                        item.getListItem().getId(),
                        item.getListItem().getName(),
                        item.getQuantity()
                ))
                .toList();

        return new PublicListSubmissionResponse(
                submission.getId(),
                submission.getList().getId(),
                submission.getList().getName(),
                submission.getFullName(),
                submission.getPhone(),
                submission.getCreatedAt(),
                items
        );
    }
}
