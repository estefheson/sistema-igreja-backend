package com.igreja.system.room.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.config.UploadProperties;
import com.igreja.system.room.dto.RoomCreateRequest;
import com.igreja.system.room.dto.RoomPhotoRequest;
import com.igreja.system.room.dto.RoomPhotoResponse;
import com.igreja.system.room.dto.RoomReservationRuleRequest;
import com.igreja.system.room.dto.RoomUpdateRequest;
import com.igreja.system.room.dto.RoomResponse;
import com.igreja.system.room.repository.RoomRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.uploads.root-dir=./target/test-uploads")
class RoomServiceIntegrationTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UploadProperties uploadProperties;

    @AfterEach
    void cleanUploadDirectory() throws IOException {
        Path rootDir = uploadProperties.getResolvedRootDir();

        if (!Files.exists(rootDir)) {
            return;
        }

        try (Stream<Path> pathStream = Files.walk(rootDir)) {
            pathStream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    @Test
    void shouldPersistAndReturnReservationRulesOnCreateAndUpdate() throws Exception {
        String roomName = "Sala Teste Regras " + System.currentTimeMillis();

        RoomResponse createdRoom = roomService.create(new RoomCreateRequest(
                roomName,
                "Ambiente para teste",
                true,
                20,
                "Regras",
                List.of(),
                List.of(
                        new RoomReservationRuleRequest(DayOfWeek.MONDAY, true, LocalTime.of(8, 0), LocalTime.of(18, 0)),
                        new RoomReservationRuleRequest(DayOfWeek.TUESDAY, false, null, null)
                )
        ));

        try {
            assertThat(createdRoom.reservationRules()).hasSize(2);

            RoomResponse loadedRoom = roomService.findById(createdRoom.id());
            assertThat(loadedRoom.reservationRules()).hasSize(2);
            assertThat(loadedRoom.reservationRules())
                    .extracting(rule -> rule.dayOfWeek().name())
                    .containsExactly("MONDAY", "TUESDAY");
            assertThat(objectMapper.writeValueAsString(loadedRoom))
                    .contains("\"startTime\":\"08:00\"")
                    .contains("\"endTime\":\"18:00\"");

            RoomResponse updatedRoom = roomService.update(createdRoom.id(), new RoomUpdateRequest(
                    roomName + " Atualizada",
                    "Ambiente atualizado",
                    true,
                    30,
                    "Regras atualizadas",
                    List.of(),
                    List.of(
                            new RoomReservationRuleRequest(DayOfWeek.WEDNESDAY, true, LocalTime.of(9, 0), LocalTime.of(17, 0))
                    )
            ));

            assertThat(updatedRoom.reservationRules()).hasSize(1);
            assertThat(updatedRoom.reservationRules().getFirst().dayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);

            RoomResponse reloadedUpdatedRoom = roomService.findById(createdRoom.id());
            assertThat(reloadedUpdatedRoom.reservationRules()).hasSize(1);
            assertThat(reloadedUpdatedRoom.reservationRules().getFirst().dayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
        } finally {
            roomRepository.deleteById(createdRoom.id());
        }
    }

    @Test
    void shouldUploadRoomPhotoAndExposeItInRoomResponse() {
        String roomName = "Sala Teste Upload " + System.currentTimeMillis();

        RoomResponse createdRoom = roomService.create(new RoomCreateRequest(
                roomName,
                "Ambiente para upload",
                true,
                15,
                "Regras",
                List.of(),
                List.of()
        ));

        try {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "ambiente.jpg",
                    "image/jpeg",
                    "fake-image-content".getBytes()
            );

            RoomPhotoResponse uploadedPhoto = roomService.uploadPhoto(createdRoom.id(), file, null);

            assertThat(uploadedPhoto.id()).isNotNull();
            assertThat(uploadedPhoto.imageUrl()).contains("/uploads/rooms/");

            RoomResponse loadedRoom = roomService.findById(createdRoom.id());
            assertThat(loadedRoom.photos()).hasSize(1);
            assertThat(loadedRoom.photos().getFirst().imageUrl()).isEqualTo(uploadedPhoto.imageUrl());

            String storedRelativePath = uploadedPhoto.imageUrl().replaceFirst("^https?://[^/]+/uploads/", "");
            if (storedRelativePath.equals(uploadedPhoto.imageUrl())) {
                storedRelativePath = uploadedPhoto.imageUrl().replaceFirst("^/uploads/", "");
            }
            Path storedFile = uploadProperties.getResolvedRootDir().resolve(storedRelativePath);
            assertThat(Files.exists(storedFile)).isTrue();
        } finally {
            roomRepository.deleteById(createdRoom.id());
        }
    }

    @Test
    void shouldLoadRoomWithPhotosAndReservationRulesTogether() {
        String roomName = "Sala Teste Foto Regras " + System.currentTimeMillis();

        RoomResponse createdRoom = roomService.create(new RoomCreateRequest(
                roomName,
                "Ambiente com foto e regras",
                true,
                10,
                "Regras",
                List.of(),
                List.of(
                        new RoomReservationRuleRequest(DayOfWeek.MONDAY, true, LocalTime.of(8, 0), LocalTime.of(18, 0)),
                        new RoomReservationRuleRequest(DayOfWeek.TUESDAY, false, null, null)
                )
        ));

        try {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "ambiente.jpg",
                    "image/jpeg",
                    "fake-image-content".getBytes()
            );

            roomService.uploadPhoto(createdRoom.id(), file, null);

            RoomResponse loadedRoom = roomService.findById(createdRoom.id());

            assertThat(loadedRoom.photos()).hasSize(1);
            assertThat(loadedRoom.reservationRules()).hasSize(2);
        } finally {
            roomRepository.deleteById(createdRoom.id());
        }
    }

    @Test
    void shouldNormalizeManagedAbsolutePhotoUrlsOnUpdate() {
        String roomName = "Sala Teste Url Absoluta " + System.currentTimeMillis();

        RoomResponse createdRoom = roomService.create(new RoomCreateRequest(
                roomName,
                "Ambiente com URL absoluta",
                true,
                10,
                "Regras",
                List.of(),
                List.of()
        ));

        try {
            roomService.update(createdRoom.id(), new RoomUpdateRequest(
                    roomName,
                    "Ambiente com URL absoluta",
                    true,
                    10,
                    "Regras",
                    List.of(new com.igreja.system.room.dto.RoomPhotoRequest(
                            "http://localhost:8080/uploads/rooms/foto.jpg",
                            0
                    )),
                    List.of()
            ));

            com.igreja.system.room.entity.Room reloadedRoom = roomRepository.findByIdWithDetails(createdRoom.id()).orElseThrow();
            assertThat(reloadedRoom.getPhotos()).hasSize(1);
            assertThat(reloadedRoom.getPhotos().iterator().next().getImageUrl()).isEqualTo("/uploads/rooms/foto.jpg");
        } finally {
            roomRepository.deleteById(createdRoom.id());
        }
    }

    @Test
    void shouldRejectLocalPreviewPhotoUrlsOnUpdate() {
        String roomName = "Sala Teste Blob " + System.currentTimeMillis();

        RoomResponse createdRoom = roomService.create(new RoomCreateRequest(
                roomName,
                "Ambiente com preview local",
                true,
                10,
                "Regras",
                List.of(),
                List.of()
        ));

        try {
            assertThatThrownBy(() -> roomService.update(createdRoom.id(), new RoomUpdateRequest(
                    roomName,
                    "Ambiente com preview local",
                    true,
                    10,
                    "Regras",
                    List.of(new RoomPhotoRequest("blob:http://localhost:5173/foto-temporaria", 0)),
                    List.of()
            )))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Fotos novas devem ser enviadas pelo endpoint de upload do ambiente");
        } finally {
            roomRepository.deleteById(createdRoom.id());
        }
    }

    @Test
    void shouldUpdateReservationRulesWithoutDuplicatingExistingDayOfWeek() {
        String roomName = "Sala Teste Update Monday " + System.currentTimeMillis();

        RoomResponse createdRoom = roomService.create(new RoomCreateRequest(
                roomName,
                "Ambiente para editar regra repetida",
                true,
                25,
                "Regras",
                List.of(),
                List.of(
                        new RoomReservationRuleRequest(DayOfWeek.MONDAY, true, LocalTime.of(8, 0), LocalTime.of(18, 0)),
                        new RoomReservationRuleRequest(DayOfWeek.TUESDAY, false, null, null)
                )
        ));

        try {
            RoomResponse updatedRoom = roomService.update(createdRoom.id(), new RoomUpdateRequest(
                    roomName,
                    "Ambiente com monday atualizado",
                    true,
                    25,
                    "Regras",
                    List.of(),
                    List.of(
                            new RoomReservationRuleRequest(DayOfWeek.MONDAY, true, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                            new RoomReservationRuleRequest(DayOfWeek.TUESDAY, true, LocalTime.of(10, 0), LocalTime.of(16, 0))
                    )
            ));

            assertThat(updatedRoom.reservationRules()).hasSize(2);
            assertThat(updatedRoom.reservationRules())
                    .filteredOn(rule -> rule.dayOfWeek() == DayOfWeek.MONDAY)
                    .singleElement()
                    .satisfies(rule -> {
                        assertThat(rule.startTime()).isEqualTo(LocalTime.of(9, 0));
                        assertThat(rule.endTime()).isEqualTo(LocalTime.of(17, 0));
                    });

            com.igreja.system.room.entity.Room reloadedRoom = roomRepository.findByIdWithDetails(createdRoom.id()).orElseThrow();
            assertThat(reloadedRoom.getReservationRules()).hasSize(2);
            assertThat(reloadedRoom.getReservationRules())
                    .extracting(com.igreja.system.room.entity.RoomReservationRule::getDayOfWeek)
                    .containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.TUESDAY);
        } finally {
            roomRepository.deleteById(createdRoom.id());
        }
    }
}
