package com.igreja.system.usermenupermission.dto;

import com.igreja.system.usermenupermission.entity.MenuKey;

import java.util.List;

public record UserMenuPermissionsResponse(
        Long userId,
        String username,
        List<String> roles,
        List<MenuKey> allowedMenus,
        List<MenuPermissionItemResponse> permissions
) {
}
