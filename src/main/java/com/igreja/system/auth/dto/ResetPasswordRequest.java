package com.igreja.system.auth.dto;

public record ResetPasswordRequest(
        String email,
        String code,
        String newPassword,
        String confirmPassword
) {
}
