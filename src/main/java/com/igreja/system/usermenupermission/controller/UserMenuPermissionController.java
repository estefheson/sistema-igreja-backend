package com.igreja.system.usermenupermission.controller;

import com.igreja.system.usermenupermission.dto.UserMenuPermissionUpdateRequest;
import com.igreja.system.usermenupermission.dto.UserMenuPermissionsResponse;
import com.igreja.system.usermenupermission.service.UserMenuPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserMenuPermissionController {

    private final UserMenuPermissionService userMenuPermissionService;

    @GetMapping("/{id}/menu-permissions")
    public UserMenuPermissionsResponse findByUserId(@PathVariable Long id) {
        return userMenuPermissionService.findByUserId(id);
    }

    @PutMapping("/{id}/menu-permissions")
    public UserMenuPermissionsResponse updateByUserId(
            @PathVariable Long id,
            @RequestBody UserMenuPermissionUpdateRequest request
    ) {
        return userMenuPermissionService.updateByUserId(id, request);
    }
}
