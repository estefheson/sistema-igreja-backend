package com.igreja.system.usermenupermission.dto;

import java.util.List;

public record UserMenuPermissionUpdateRequest(
        List<MenuPermissionItemRequest> permissions
) {
}
