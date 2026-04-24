package com.igreja.system.usermenupermission.controller;

import com.igreja.system.usermenupermission.dto.UserMenuPermissionsResponse;
import com.igreja.system.usermenupermission.service.UserMenuPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyMenuPermissionController {

    private final UserMenuPermissionService userMenuPermissionService;

    @GetMapping("/menu-permissions")
    public UserMenuPermissionsResponse findMyPermissions() {
        return userMenuPermissionService.findMyMenuPermissions();
    }
}
