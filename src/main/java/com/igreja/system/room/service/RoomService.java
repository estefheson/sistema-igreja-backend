package com.igreja.system.room.service;

import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.room.dto.RoomActiveUpdateRequest;
import com.igreja.system.room.dto.RoomCreateRequest;
import com.igreja.system.room.dto.RoomPhotoRequest;
import com.igreja.system.room.dto.RoomPhotoResponse;
import com.igreja.system.room.dto.RoomReservationRuleRequest;
import com.igreja.system.room.dto.RoomReservationRuleResponse;
import com.igreja.system.room.dto.RoomResponse;
import com.igreja.system.room.dto.RoomUpdateRequest;
import com.igreja.system.room.entity.Room;
import com.igreja.system.room.entity.RoomPhoto;
import com.igreja.system.room.entity.RoomReservationRule;
import com.igreja.system.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomPhotoStorageService roomPhotoStorageService;

    @Transactional
    public RoomResponse create(RoomCreateRequest request) {
        validateRequiredName(request.name());
        validateCapacity(request.capacity());
        validateReservationRules(request.reservationRules());

        Room room = Room.builder()
                .name(request.name())
                .description(request.description())
                .active(request.active() != null ? request.active() : true)
                .capacity(request.capacity())
                .usageRules(request.usageRules())
                .build();
        room.replacePhotos(toPhotoEntities(request.photos()));
        room.replaceReservationRules(toReservationRuleEntities(request.reservationRules()));

        Room savedRoom = roomRepository.save(room);

        return toResponse(savedRoom);
    }

    public List<RoomResponse> findAll() {
        return roomRepository.findAllWithDetails()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RoomResponse findById(Long id) {
        return toResponse(findRoomById(id));
    }

    @Transactional
    public RoomResponse update(Long id, RoomUpdateRequest request) {
        Room room = findRoomById(id);
        List<String> previousPhotoUrls = room.getPhotos()
                .stream()
                .map(RoomPhoto::getImageUrl)
                .toList();

        validateRequiredName(request.name());
        validateCapacity(request.capacity());
        validateReservationRules(request.reservationRules());

        room.setName(request.name());
        room.setDescription(request.description());
        room.setActive(request.active() != null ? request.active() : room.getActive());
        room.setCapacity(request.capacity());
        room.setUsageRules(request.usageRules());
        room.replacePhotos(toPhotoEntities(request.photos()));
        syncReservationRules(room, request.reservationRules());

        Room updatedRoom = roomRepository.save(room);
        deleteRemovedManagedPhotos(previousPhotoUrls, updatedRoom.getPhotos());

        return toResponse(updatedRoom);
    }

    @Transactional
    public RoomPhotoResponse uploadPhoto(Long id, MultipartFile file, Integer displayOrder) {
        Room room = findRoomById(id);
        int resolvedDisplayOrder = resolveDisplayOrder(room, displayOrder);
        String imageUrl = roomPhotoStorageService.store(file);

        RoomPhoto photo = RoomPhoto.builder()
                .imageUrl(imageUrl)
                .displayOrder(resolvedDisplayOrder)
                .build();

        room.addPhoto(photo);
        roomRepository.saveAndFlush(room);

        Room reloadedRoom = findRoomById(id);
        RoomPhoto savedPhoto = reloadedRoom.getPhotos()
                .stream()
                .filter(existingPhoto -> imageUrl.equals(existingPhoto.getImageUrl()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Imagem do ambiente nao encontrada apos o upload"));

        return toPhotoResponse(savedPhoto);
    }

    @Transactional
    public RoomResponse updateActive(Long id, RoomActiveUpdateRequest request) {
        Room room = findRoomById(id);

        if (request.active() == null) {
            throw new BusinessException("Active e obrigatorio");
        }

        room.setActive(request.active());

        Room updatedRoom = roomRepository.save(room);

        return toResponse(updatedRoom);
    }

    private void validateRequiredName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Nome e obrigatorio");
        }
    }

    private void validateCapacity(Integer capacity) {
        if (capacity != null && capacity < 0) {
            throw new BusinessException("Capacidade deve ser maior ou igual a zero");
        }
    }

    private int resolveDisplayOrder(Room room, Integer displayOrder) {
        if (displayOrder != null && displayOrder < 0) {
            throw new BusinessException("Ordem de exibicao da foto deve ser maior ou igual a zero");
        }

        if (displayOrder != null) {
            return displayOrder;
        }

        return room.getPhotos()
                .stream()
                .map(RoomPhoto::getDisplayOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(currentMax -> currentMax + 1)
                .orElse(0);
    }

    private void validateReservationRules(List<RoomReservationRuleRequest> reservationRules) {
        if (reservationRules == null) {
            return;
        }

        Set<DayOfWeek> informedDays = EnumSet.noneOf(DayOfWeek.class);

        for (RoomReservationRuleRequest reservationRule : reservationRules) {
            if (reservationRule.dayOfWeek() == null) {
                throw new BusinessException("Dia da semana da regra de reserva e obrigatorio");
            }

            if (!informedDays.add(reservationRule.dayOfWeek())) {
                throw new BusinessException("Nao e permitido informar dias da semana duplicados");
            }

            if (reservationRule.enabled() == null) {
                throw new BusinessException("Campo enabled da regra de reserva e obrigatorio");
            }

            if (Boolean.TRUE.equals(reservationRule.enabled())) {
                if (reservationRule.startTime() == null || reservationRule.endTime() == null) {
                    throw new BusinessException("Horario inicial e final sao obrigatorios quando o dia estiver habilitado");
                }

                if (!reservationRule.endTime().isAfter(reservationRule.startTime())) {
                    throw new BusinessException("Horario final deve ser maior que o horario inicial");
                }
            } else if (reservationRule.startTime() != null || reservationRule.endTime() != null) {
                throw new BusinessException("Dias desabilitados nao devem informar horarios");
            }
        }
    }

    private Room findRoomById(Long id) {
        return roomRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException("Ambiente nao encontrado"));
    }

    private Set<RoomPhoto> toPhotoEntities(List<RoomPhotoRequest> photos) {
        Set<RoomPhoto> photoEntities = new LinkedHashSet<>();

        if (photos == null) {
            return photoEntities;
        }

        for (int index = 0; index < photos.size(); index++) {
            RoomPhotoRequest photo = photos.get(index);

            if (photo.imageUrl() == null || photo.imageUrl().isBlank()) {
                throw new BusinessException("URL da foto e obrigatoria");
            }

            Integer displayOrder = photo.displayOrder() != null ? photo.displayOrder() : index;

            if (displayOrder < 0) {
                throw new BusinessException("Ordem de exibicao da foto deve ser maior ou igual a zero");
            }

            validatePersistedPhotoReference(photo.imageUrl());

            photoEntities.add(RoomPhoto.builder()
                    .imageUrl(normalizePhotoUrl(photo.imageUrl()))
                    .displayOrder(displayOrder)
                    .build());
        }

        return photoEntities;
    }

    private Set<RoomReservationRule> toReservationRuleEntities(List<RoomReservationRuleRequest> reservationRules) {
        Set<RoomReservationRule> ruleEntities = new LinkedHashSet<>();

        if (reservationRules == null) {
            return ruleEntities;
        }

        for (RoomReservationRuleRequest reservationRule : reservationRules) {
            ruleEntities.add(RoomReservationRule.builder()
                    .dayOfWeek(reservationRule.dayOfWeek())
                    .enabled(reservationRule.enabled())
                    .startTime(reservationRule.startTime())
                    .endTime(reservationRule.endTime())
                    .build());
        }

        return ruleEntities;
    }

    private void syncReservationRules(Room room, List<RoomReservationRuleRequest> reservationRules) {
        Map<DayOfWeek, RoomReservationRule> existingRulesByDay = new EnumMap<>(DayOfWeek.class);
        room.getReservationRules().forEach(rule -> existingRulesByDay.put(rule.getDayOfWeek(), rule));

        Set<DayOfWeek> informedDays = EnumSet.noneOf(DayOfWeek.class);

        if (reservationRules != null) {
            for (RoomReservationRuleRequest reservationRule : reservationRules) {
                informedDays.add(reservationRule.dayOfWeek());

                RoomReservationRule existingRule = existingRulesByDay.get(reservationRule.dayOfWeek());

                if (existingRule != null) {
                    existingRule.setEnabled(reservationRule.enabled());
                    existingRule.setStartTime(reservationRule.startTime());
                    existingRule.setEndTime(reservationRule.endTime());
                    continue;
                }

                room.addReservationRule(RoomReservationRule.builder()
                        .dayOfWeek(reservationRule.dayOfWeek())
                        .enabled(reservationRule.enabled())
                        .startTime(reservationRule.startTime())
                        .endTime(reservationRule.endTime())
                        .build());
            }
        }

        room.getReservationRules().removeIf(existingRule -> !informedDays.contains(existingRule.getDayOfWeek()));
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getActive(),
                room.getCapacity(),
                room.getUsageRules(),
                room.getPhotos()
                        .stream()
                        .sorted(Comparator
                                .comparing(RoomPhoto::getDisplayOrder)
                                .thenComparing(RoomPhoto::getId, Comparator.nullsLast(Long::compareTo)))
                        .map(this::toPhotoResponse)
                        .toList(),
                room.getReservationRules()
                        .stream()
                        .sorted(Comparator.comparing(reservationRule -> reservationRule.getDayOfWeek().getValue()))
                        .map(reservationRule -> new RoomReservationRuleResponse(
                                reservationRule.getId(),
                                reservationRule.getDayOfWeek(),
                                reservationRule.getEnabled(),
                                reservationRule.getStartTime(),
                                reservationRule.getEndTime()
                        ))
                        .toList()
        );
    }

    private RoomPhotoResponse toPhotoResponse(RoomPhoto photo) {
        return new RoomPhotoResponse(
                photo.getId(),
                resolvePhotoUrl(photo.getImageUrl()),
                photo.getDisplayOrder()
        );
    }

    private void deleteRemovedManagedPhotos(List<String> previousPhotoUrls, Set<RoomPhoto> currentPhotos) {
        List<String> currentPhotoUrls = currentPhotos.stream()
                .map(RoomPhoto::getImageUrl)
                .toList();

        List<String> removedPhotoUrls = new ArrayList<>(previousPhotoUrls);
        removedPhotoUrls.removeAll(currentPhotoUrls);

        removedPhotoUrls.forEach(roomPhotoStorageService::deleteIfManaged);
    }

    private String normalizePhotoUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        String normalizedImageUrl = imageUrl.trim();

        if (normalizedImageUrl.isBlank()) {
            return normalizedImageUrl;
        }

        if (normalizedImageUrl.startsWith("http://") || normalizedImageUrl.startsWith("https://")) {
            try {
                URI uri = new URI(normalizedImageUrl);

                if (uri.getPath() != null && uri.getPath().startsWith("/uploads/rooms/")) {
                    return uri.getPath();
                }
            } catch (URISyntaxException ignored) {
                return normalizedImageUrl;
            }
        }

        return normalizedImageUrl;
    }

    private void validatePersistedPhotoReference(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        if (looksLikeNonResolvableExternalReference(imageUrl)) {
            throw new BusinessException("Fotos novas devem ser enviadas pelo endpoint de upload do ambiente");
        }
    }

    private String resolvePhotoUrl(String imageUrl) {
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

    private boolean looksLikeNonResolvableExternalReference(String imageUrl) {
        return imageUrl.startsWith("blob:")
                || imageUrl.startsWith("data:")
                || imageUrl.startsWith("file:")
                || imageUrl.contains("\\");
    }
}
