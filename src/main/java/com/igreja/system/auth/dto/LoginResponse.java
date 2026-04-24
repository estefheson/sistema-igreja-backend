package com.igreja.system.auth.dto;

import com.igreja.system.usermenupermission.entity.MenuKey;

import java.util.List;

public record LoginResponse(
        String token,
        String username,
        List<String> roles,
        List<MenuKey> menuPermissions
) {
}
