package com.igreja.system.auth.dto;

public record LoginRequest(
        String username,
        String password
) {
}