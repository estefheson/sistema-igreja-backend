package com.igreja.system.usermenupermission.dto;

import com.igreja.system.usermenupermission.entity.MenuKey;

public record MenuPermissionItemResponse(
        MenuKey menuKey,
        Boolean allowed,
        Boolean explicit
) {
}
