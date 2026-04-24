package com.igreja.system.common.service;

import com.igreja.system.common.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("Senha e obrigatoria");
        }

        if (password.length() < 8 || !hasSpecialCharacter(password)) {
            throw new BusinessException("Senha deve ter no minimo 8 caracteres e pelo menos 1 caractere especial");
        }
    }

    private boolean hasSpecialCharacter(String password) {
        return password.chars()
                .mapToObj(character -> (char) character)
                .anyMatch(character -> !Character.isLetterOrDigit(character) && !Character.isWhitespace(character));
    }
}
