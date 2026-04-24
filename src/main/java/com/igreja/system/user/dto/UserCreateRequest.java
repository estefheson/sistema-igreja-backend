package com.igreja.system.user.dto;

import java.util.List;

public record UserCreateRequest(
        String username,
        String email,
        String password,
        Long memberId,
        Boolean active,
        List<String> roles
) {
}
