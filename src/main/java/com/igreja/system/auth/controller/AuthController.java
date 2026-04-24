package com.igreja.system.auth.controller;

import com.igreja.system.auth.dto.LoginRequest;
import com.igreja.system.auth.dto.LoginResponse;
import com.igreja.system.auth.dto.ChangePasswordRequest;
import com.igreja.system.auth.dto.ForgotPasswordRequest;
import com.igreja.system.auth.dto.ResetPasswordRequest;
import com.igreja.system.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@RequestBody ForgotPasswordRequest request) {
        service.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody ResetPasswordRequest request) {
        service.resetPassword(request);
    }

    @PatchMapping("/change-password")
    public void changePassword(@RequestBody ChangePasswordRequest request) {
        service.changePassword(request);
    }
}
