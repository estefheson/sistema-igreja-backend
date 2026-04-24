package com.igreja.system.listmodule.controller;

import com.igreja.system.listmodule.dto.admin.ChurchListItemParticipantResponse;
import com.igreja.system.listmodule.dto.admin.ChurchListItemResponse;
import com.igreja.system.listmodule.dto.admin.ChurchListItemUpdateRequest;
import com.igreja.system.listmodule.service.ChurchListAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/list-items")
@RequiredArgsConstructor
public class ChurchListItemController {

    private final ChurchListAdminService churchListAdminService;

    @PutMapping("/{itemId}")
    public ChurchListItemResponse update(@PathVariable Long itemId, @RequestBody ChurchListItemUpdateRequest request) {
        return churchListAdminService.updateItem(itemId, request);
    }

    @GetMapping("/{itemId}/participants")
    public List<ChurchListItemParticipantResponse> findParticipants(@PathVariable Long itemId) {
        return churchListAdminService.findParticipantsByItemId(itemId);
    }

    @DeleteMapping("/{itemId}")
    public void delete(@PathVariable Long itemId) {
        churchListAdminService.deleteItem(itemId);
    }

    @PostMapping(value = "/{itemId}/image/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChurchListItemResponse uploadImage(
            @PathVariable Long itemId,
            @RequestParam("file") MultipartFile file
    ) {
        return churchListAdminService.uploadItemImage(itemId, file);
    }
}
