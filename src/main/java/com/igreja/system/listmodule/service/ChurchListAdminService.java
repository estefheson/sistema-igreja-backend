package com.igreja.system.listmodule.service;

import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.listmodule.dto.admin.ChurchListCreateRequest;
import com.igreja.system.listmodule.dto.admin.ChurchListItemCreateRequest;
import com.igreja.system.listmodule.dto.admin.ChurchListItemParticipantResponse;
import com.igreja.system.listmodule.dto.admin.ChurchListItemResponse;
import com.igreja.system.listmodule.dto.admin.ChurchListManagementItemResponse;
import com.igreja.system.listmodule.dto.admin.ChurchListManagementResponse;
import com.igreja.system.listmodule.dto.admin.ChurchListItemUpdateRequest;
import com.igreja.system.listmodule.dto.admin.ChurchListResponse;
import com.igreja.system.listmodule.dto.admin.ChurchListUpdateRequest;
import com.igreja.system.listmodule.entity.ChurchList;
import com.igreja.system.listmodule.entity.ChurchListItem;
import com.igreja.system.listmodule.repository.ChurchListItemRepository;
import com.igreja.system.listmodule.repository.ChurchListRepository;
import com.igreja.system.listmodule.repository.PublicListSubmissionItemRepository;
import com.igreja.system.listmodule.repository.PublicListSubmissionRepository;
import com.igreja.system.listmodule.repository.projection.ListItemParticipantProjection;
import com.igreja.system.listmodule.repository.projection.ListItemReservedQuantityProjection;
import com.igreja.system.usermenupermission.entity.MenuKey;
import com.igreja.system.usermenupermission.service.UserMenuPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChurchListAdminService {

    private static final String STATUS_ACTIVE = "Ativa";
    private static final String STATUS_SCHEDULED = "Agendada";
    private static final String STATUS_ENDED = "Encerrada";
    private static final String STATUS_INACTIVE = "Inativa";

    private final ChurchListRepository churchListRepository;
    private final ChurchListItemRepository churchListItemRepository;
    private final PublicListSubmissionRepository publicListSubmissionRepository;
    private final PublicListSubmissionItemRepository publicListSubmissionItemRepository;
    private final ListItemImageStorageService listItemImageStorageService;
    private final UserMenuPermissionService userMenuPermissionService;

    @Transactional
    public ChurchListResponse create(ChurchListCreateRequest request) {
        validateListModuleAccess();
        validateListRequest(request.name(), request.startsAt(), request.endsAt());

        ChurchList savedList = churchListRepository.save(ChurchList.builder()
                .name(normalizeRequiredText(request.name(), "Nome da lista e obrigatorio"))
                .description(normalizeOptionalText(request.description()))
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .active(request.active() != null ? request.active() : true)
                .build());

        return toResponse(savedList);
    }

    @Transactional
    public ChurchListResponse update(Long id, ChurchListUpdateRequest request) {
        validateListModuleAccess();
        validateListRequest(request.name(), request.startsAt(), request.endsAt());

        ChurchList list = findListById(id);
        list.setName(normalizeRequiredText(request.name(), "Nome da lista e obrigatorio"));
        list.setDescription(normalizeOptionalText(request.description()));
        list.setStartsAt(request.startsAt());
        list.setEndsAt(request.endsAt());
        list.setActive(request.active() != null ? request.active() : list.getActive());

        return toResponse(churchListRepository.save(list));
    }

    public List<ChurchListResponse> findAll() {
        validateListModuleAccess();

        return churchListRepository.findAllWithItems()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ChurchListResponse findById(Long id) {
        validateListModuleAccess();
        return toResponse(findListById(id));
    }

    public ChurchListManagementResponse findManagementById(Long id) {
        validateListModuleAccess();

        ChurchList list = findListById(id);
        Map<Long, Integer> reservedQuantitiesByItemId = loadReservedQuantitiesByItemId(id);
        Map<Long, List<ChurchListItemParticipantResponse>> participantsByItemId = loadParticipantsByItemId(id);
        int totalReservedQuantity = 0;
        int totalAvailableQuantity = 0;
        int totalQuantity = 0;
        List<ChurchListManagementItemResponse> items = new ArrayList<>();

        for (ChurchListItem item : list.getItems().stream().sorted(Comparator.comparing(ChurchListItem::getId)).toList()) {
            ItemMetrics itemMetrics = calculateItemMetrics(item.getTotalQuantity(), reservedQuantitiesByItemId.getOrDefault(item.getId(), 0));
            totalReservedQuantity += itemMetrics.reservedQuantity();
            totalAvailableQuantity += itemMetrics.availableQuantity();
            totalQuantity += item.getTotalQuantity();

            items.add(new ChurchListManagementItemResponse(
                    item.getId(),
                    list.getId(),
                    item.getName(),
                    item.getDescription(),
                    resolveImageUrl(item.getImageUrl()),
                    item.getTotalQuantity(),
                    itemMetrics.reservedQuantity(),
                    itemMetrics.availableQuantity(),
                    itemMetrics.fillPercentage(),
                    participantsByItemId.getOrDefault(item.getId(), List.of())
            ));
        }

        double overallFillPercentage = calculateFillPercentage(totalReservedQuantity, totalQuantity);
        int totalParticipants = Math.toIntExact(publicListSubmissionRepository.countByListId(id));

        return new ChurchListManagementResponse(
                list.getId(),
                list.getName(),
                list.getDescription(),
                list.getStartsAt(),
                list.getEndsAt(),
                list.getActive(),
                calculateStatus(list, LocalDateTime.now()),
                items.size(),
                totalReservedQuantity,
                totalAvailableQuantity,
                totalParticipants,
                overallFillPercentage,
                items
        );
    }

    public List<ChurchListItemParticipantResponse> findParticipantsByItemId(Long itemId) {
        validateListModuleAccess();

        ChurchListItem item = findItemById(itemId);
        return publicListSubmissionItemRepository.findParticipantsByItemId(item.getId()).stream()
                .map(this::toParticipantResponse)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        validateListModuleAccess();

        ChurchList list = findListById(id);
        List<String> imageUrls = list.getItems().stream()
                .map(ChurchListItem::getImageUrl)
                .filter(imageUrl -> imageUrl != null && !imageUrl.isBlank())
                .toList();

        churchListRepository.delete(list);
        imageUrls.forEach(listItemImageStorageService::deleteIfManaged);
    }

    @Transactional
    public ChurchListResponse addItem(Long listId, ChurchListItemCreateRequest request) {
        validateListModuleAccess();
        validateItemRequest(request.name(), request.totalQuantity());

        ChurchList list = findListById(listId);
        ChurchListItem item = ChurchListItem.builder()
                .name(normalizeRequiredText(request.name(), "Nome do item e obrigatorio"))
                .description(normalizeOptionalText(request.description()))
                .imageUrl(normalizeImageUrlForPersistence(request.imageUrl()))
                .totalQuantity(request.totalQuantity())
                .build();

        list.addItem(item);
        ChurchList savedList = churchListRepository.save(list);

        return toResponse(savedList);
    }

    @Transactional
    public ChurchListItemResponse updateItem(Long itemId, ChurchListItemUpdateRequest request) {
        validateListModuleAccess();
        validateItemRequest(request.name(), request.totalQuantity());

        ChurchListItem item = findItemById(itemId);
        String previousImageUrl = item.getImageUrl();

        item.setName(normalizeRequiredText(request.name(), "Nome do item e obrigatorio"));
        item.setDescription(normalizeOptionalText(request.description()));
        item.setTotalQuantity(request.totalQuantity());

        if (request.imageUrl() != null) {
            item.setImageUrl(normalizeImageUrlForPersistence(request.imageUrl()));
        }

        ChurchListItem updatedItem = churchListItemRepository.save(item);

        if (request.imageUrl() != null && imageWasRemoved(previousImageUrl, updatedItem.getImageUrl())) {
            listItemImageStorageService.deleteIfManaged(previousImageUrl);
        }

        return toItemResponse(updatedItem);
    }

    @Transactional
    public void deleteItem(Long itemId) {
        validateListModuleAccess();

        ChurchListItem item = findItemById(itemId);

        if (!item.getSubmissionItems().isEmpty()) {
            throw new BusinessException("Nao e possivel remover item que ja possui participacoes publicas");
        }

        String previousImageUrl = item.getImageUrl();
        ChurchList list = item.getList();
        list.removeItem(item);
        churchListRepository.save(list);
        listItemImageStorageService.deleteIfManaged(previousImageUrl);
    }

    @Transactional
    public ChurchListItemResponse uploadItemImage(Long itemId, MultipartFile file) {
        validateListModuleAccess();

        ChurchListItem item = findItemById(itemId);
        String previousImageUrl = item.getImageUrl();
        String imageUrl = listItemImageStorageService.store(file);

        item.setImageUrl(imageUrl);
        ChurchListItem savedItem = churchListItemRepository.saveAndFlush(item);

        if (imageWasRemoved(previousImageUrl, imageUrl)) {
            listItemImageStorageService.deleteIfManaged(previousImageUrl);
        }

        return toItemResponse(savedItem);
    }

    private ChurchList findListById(Long id) {
        return churchListRepository.findByIdWithItems(id)
                .orElseThrow(() -> new BusinessException("Lista nao encontrada"));
    }

    private ChurchListItem findItemById(Long itemId) {
        return churchListItemRepository.findByIdWithList(itemId)
                .orElseThrow(() -> new BusinessException("Item da lista nao encontrado"));
    }

    private Map<Long, Integer> loadReservedQuantitiesByItemId(Long listId) {
        Map<Long, Integer> reservedQuantitiesByItemId = new LinkedHashMap<>();

        for (ListItemReservedQuantityProjection projection : churchListItemRepository.findReservedQuantitiesByListId(listId)) {
            reservedQuantitiesByItemId.put(
                    projection.itemId(),
                    projection.reservedQuantity() != null ? projection.reservedQuantity().intValue() : 0
            );
        }

        return reservedQuantitiesByItemId;
    }

    private Map<Long, List<ChurchListItemParticipantResponse>> loadParticipantsByItemId(Long listId) {
        Map<Long, List<ChurchListItemParticipantResponse>> participantsByItemId = new LinkedHashMap<>();

        for (ListItemParticipantProjection projection : publicListSubmissionItemRepository.findParticipantsByListId(listId)) {
            participantsByItemId.computeIfAbsent(projection.itemId(), ignored -> new ArrayList<>())
                    .add(toParticipantResponse(projection));
        }

        return participantsByItemId;
    }

    private void validateListModuleAccess() {
        userMenuPermissionService.validateAuthenticatedUserHasMenuAccess(MenuKey.LISTS);
    }

    private void validateListRequest(String name, LocalDateTime startsAt, LocalDateTime endsAt) {
        normalizeRequiredText(name, "Nome da lista e obrigatorio");

        if (startsAt == null) {
            throw new BusinessException("Data/hora inicial da lista e obrigatoria");
        }

        if (endsAt == null) {
            throw new BusinessException("Data/hora final da lista e obrigatoria");
        }

        if (endsAt.isBefore(startsAt)) {
            throw new BusinessException("Data/hora final deve ser maior ou igual a data/hora inicial");
        }
    }

    private void validateItemRequest(String name, Integer totalQuantity) {
        normalizeRequiredText(name, "Nome do item e obrigatorio");

        if (totalQuantity == null || totalQuantity < 1) {
            throw new BusinessException("Quantidade total do item deve ser maior ou igual a 1");
        }
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(errorMessage);
        }

        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isBlank() ? null : normalizedValue;
    }

    private String normalizeImageUrlForPersistence(String imageUrl) {
        String normalizedImageUrl = normalizeOptionalText(imageUrl);

        if (normalizedImageUrl == null) {
            return null;
        }

        validatePersistedImageReference(normalizedImageUrl);

        if (normalizedImageUrl.startsWith("http://") || normalizedImageUrl.startsWith("https://")) {
            try {
                URI uri = new URI(normalizedImageUrl);

                if (uri.getPath() != null && uri.getPath().startsWith("/uploads/")) {
                    return uri.getPath();
                }
            } catch (URISyntaxException ignored) {
                return normalizedImageUrl;
            }
        }

        return normalizedImageUrl;
    }

    private void validatePersistedImageReference(String imageUrl) {
        if (looksLikeNonResolvableExternalReference(imageUrl)) {
            throw new BusinessException("Imagens novas devem ser enviadas pelo endpoint de upload do item da lista");
        }
    }

    private boolean looksLikeNonResolvableExternalReference(String imageUrl) {
        return imageUrl.startsWith("blob:")
                || imageUrl.startsWith("data:")
                || imageUrl.startsWith("file:")
                || imageUrl.contains("\\");
    }

    private boolean imageWasRemoved(String previousImageUrl, String currentImageUrl) {
        return previousImageUrl != null
                && !previousImageUrl.isBlank()
                && (currentImageUrl == null || !previousImageUrl.equals(currentImageUrl));
    }

    private ChurchListResponse toResponse(ChurchList list) {
        return new ChurchListResponse(
                list.getId(),
                list.getName(),
                list.getDescription(),
                list.getStartsAt(),
                list.getEndsAt(),
                list.getActive(),
                calculateStatus(list, LocalDateTime.now()),
                list.getItems().stream()
                        .sorted(Comparator.comparing(ChurchListItem::getId))
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    private ChurchListItemParticipantResponse toParticipantResponse(ListItemParticipantProjection projection) {
        return new ChurchListItemParticipantResponse(
                projection.fullName(),
                projection.phone(),
                projection.quantity(),
                projection.createdAt()
        );
    }

    private ItemMetrics calculateItemMetrics(int totalQuantity, int reservedQuantity) {
        int effectiveTotalQuantity = Math.max(totalQuantity, 0);
        int effectiveReservedQuantity = Math.min(Math.max(reservedQuantity, 0), effectiveTotalQuantity);
        int availableQuantity = Math.max(effectiveTotalQuantity - effectiveReservedQuantity, 0);

        return new ItemMetrics(
                effectiveReservedQuantity,
                availableQuantity,
                calculateFillPercentage(effectiveReservedQuantity, effectiveTotalQuantity)
        );
    }

    private double calculateFillPercentage(int reservedQuantity, int totalQuantity) {
        if (totalQuantity <= 0) {
            return 0D;
        }

        double percentage = (Math.min(Math.max(reservedQuantity, 0), totalQuantity) * 100.0) / totalQuantity;
        return Math.round(percentage * 100.0) / 100.0;
    }

    private String calculateStatus(ChurchList list, LocalDateTime referenceDateTime) {
        if (!Boolean.TRUE.equals(list.getActive())) {
            return STATUS_INACTIVE;
        }

        if (referenceDateTime.isBefore(list.getStartsAt())) {
            return STATUS_SCHEDULED;
        }

        if (referenceDateTime.isAfter(list.getEndsAt())) {
            return STATUS_ENDED;
        }

        return STATUS_ACTIVE;
    }

    private ChurchListItemResponse toItemResponse(ChurchListItem item) {
        return new ChurchListItemResponse(
                item.getId(),
                item.getList().getId(),
                item.getName(),
                item.getDescription(),
                resolveImageUrl(item.getImageUrl()),
                item.getTotalQuantity()
        );
    }

    private String resolveImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return imageUrl;
        }

        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }

        if (looksLikeNonResolvableExternalReference(imageUrl)) {
            return imageUrl;
        }

        String normalizedPath = imageUrl.startsWith("/") ? imageUrl : "/" + imageUrl;

        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(normalizedPath)
                    .toUriString();
        } catch (RuntimeException ex) {
            return imageUrl;
        }
    }

    private record ItemMetrics(
            int reservedQuantity,
            int availableQuantity,
            double fillPercentage
    ) {
    }
}
