package com.igreja.system.listmodule.controller;

import com.igreja.system.listmodule.dto.admin.ChurchListCreateRequest;
import com.igreja.system.listmodule.dto.admin.ChurchListItemCreateRequest;
import com.igreja.system.listmodule.dto.admin.ChurchListManagementResponse;
import com.igreja.system.listmodule.dto.admin.ChurchListResponse;
import com.igreja.system.listmodule.dto.admin.ChurchListUpdateRequest;
import com.igreja.system.listmodule.service.ChurchListAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lists")
@RequiredArgsConstructor
public class ChurchListController {

    private final ChurchListAdminService churchListAdminService;

    @GetMapping
    public List<ChurchListResponse> findAll() {
        return churchListAdminService.findAll();
    }

    @GetMapping("/{id}")
    public ChurchListResponse findById(@PathVariable Long id) {
        return churchListAdminService.findById(id);
    }

    @GetMapping("/{id}/management")
    public ChurchListManagementResponse findManagementById(@PathVariable Long id) {
        return churchListAdminService.findManagementById(id);
    }

    @PostMapping
    public ChurchListResponse create(@RequestBody ChurchListCreateRequest request) {
        return churchListAdminService.create(request);
    }

    @PutMapping("/{id}")
    public ChurchListResponse update(@PathVariable Long id, @RequestBody ChurchListUpdateRequest request) {
        return churchListAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        churchListAdminService.delete(id);
    }

    @PostMapping("/{id}/items")
    public ChurchListResponse addItem(@PathVariable Long id, @RequestBody ChurchListItemCreateRequest request) {
        return churchListAdminService.addItem(id, request);
    }
}
