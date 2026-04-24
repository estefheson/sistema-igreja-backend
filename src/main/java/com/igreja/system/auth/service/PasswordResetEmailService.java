package com.igreja.system.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PasswordResetEmailService {

    public void sendResetCode(String email, String code, long expirationMinutes) {
        log.info(
                "Codigo de recuperacao para {}: {} (expira em {} minutos)",
                email,
                code,
                expirationMinutes
        );
    }
}
