package com.igreja.system.room.controller;

import com.igreja.system.room.dto.RoomActiveUpdateRequest;
import com.igreja.system.room.dto.RoomCreateRequest;
import com.igreja.system.room.dto.RoomPhotoResponse;
import com.igreja.system.room.dto.RoomResponse;
import com.igreja.system.room.dto.RoomUpdateRequest;
import com.igreja.system.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public RoomResponse create(@RequestBody RoomCreateRequest request) {
        return roomService.create(request);
    }

    @PostMapping(value = "/{id}/photos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RoomPhotoResponse uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Integer displayOrder
    ) {
        return roomService.uploadPhoto(id, file, displayOrder);
    }

    @GetMapping
    public List<RoomResponse> findAll() {
        return roomService.findAll();
    }

    @GetMapping("/{id}")
    public RoomResponse findById(@PathVariable Long id) {
        return roomService.findById(id);
    }

    @PutMapping("/{id}")
    public RoomResponse update(@PathVariable Long id, @RequestBody RoomUpdateRequest request) {
        return roomService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public RoomResponse updateActive(@PathVariable Long id, @RequestBody RoomActiveUpdateRequest request) {
        return roomService.updateActive(id, request);
    }
}
