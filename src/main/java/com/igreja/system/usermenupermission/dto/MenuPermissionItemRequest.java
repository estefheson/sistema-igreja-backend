package com.igreja.system.usermenupermission.dto;

import com.igreja.system.usermenupermission.entity.MenuKey;

public record MenuPermissionItemRequest(
        MenuKey menuKey,
        Boolean allowed
) {
}
