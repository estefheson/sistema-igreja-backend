package com.igreja.system.user.dto;

import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String email,
        Long memberId,
        String memberName,
        String memberCpf,
        Boolean active,
        List<String> roles
) {
}
