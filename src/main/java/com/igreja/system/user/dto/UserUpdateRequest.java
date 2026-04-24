package com.igreja.system.user.dto;

import java.util.List;

public record UserUpdateRequest(
        String username,
        String email,
        Long memberId,
        Boolean active,
        List<String> roles
) {
}
