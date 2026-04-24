package com.igreja.system.auth.service;

import com.igreja.system.auth.dto.ChangePasswordRequest;
import com.igreja.system.auth.dto.ForgotPasswordRequest;
import com.igreja.system.auth.dto.LoginRequest;
import com.igreja.system.auth.dto.LoginResponse;
import com.igreja.system.auth.dto.ResetPasswordRequest;
import com.igreja.system.auth.entity.PasswordResetCode;
import com.igreja.system.auth.repository.PasswordResetCodeRepository;
import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.common.service.PasswordPolicyService;
import com.igreja.system.usermenupermission.entity.MenuKey;
import com.igreja.system.usermenupermission.service.UserMenuPermissionService;
import com.igreja.system.user.entity.User;
import com.igreja.system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final PasswordResetEmailService passwordResetEmailService;
    private final UserMenuPermissionService userMenuPermissionService;

    @Value("${auth.password-reset.expiration-minutes}")
    private long passwordResetExpirationMinutes;

    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            List<String> roles = authentication.getAuthorities()
                    .stream()
                    .map(role -> {
                        String authority = role.getAuthority();
                        return authority.startsWith("ROLE_") ? authority : "ROLE_" + authority;
                    })
                    .toList();
            List<MenuKey> menuPermissions = userMenuPermissionService.findAllowedMenusByUsername(authentication.getName());

            String token = jwtService.generateToken(authentication.getName(), roles);

            return new LoginResponse(token, authentication.getName(), roles, menuPermissions);
        } catch (DisabledException ex) {
            throw new BusinessException("Usuario inativo");
        } catch (BadCredentialsException ex) {
            throw new BusinessException("Usuario ou senha invalidos");
        } catch (AuthenticationException ex) {
            throw new BusinessException("Falha ao autenticar usuario");
        }
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        validateCurrentPasswordRequired(request.currentPassword());
        validatePasswordConfirmation(request.newPassword(), request.confirmPassword());
        passwordPolicyService.validate(request.newPassword());

        User authenticatedUser = findAuthenticatedUser();

        if (!passwordEncoder.matches(request.currentPassword(), authenticatedUser.getPassword())) {
            throw new BusinessException("Senha atual invalida");
        }

        authenticatedUser.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(authenticatedUser);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        validateEmailRequired(request.email());

        userRepository.findByEmail(request.email())
                .filter(User::getActive)
                .ifPresent(this::generateAndSendPasswordResetCode);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validateEmailRequired(request.email());
        validateCodeRequired(request.code());
        validatePasswordConfirmation(request.newPassword(), request.confirmPassword());
        passwordPolicyService.validate(request.newPassword());

        PasswordResetCode passwordResetCode = passwordResetCodeRepository
                .findFirstByUserEmailIgnoreCaseAndCodeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        request.email(),
                        request.code(),
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new BusinessException("Codigo de recuperacao invalido ou expirado"));

        User user = passwordResetCode.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));

        passwordResetCode.setUsed(true);
        passwordResetCodeRepository.markAllUnusedAsUsedByUserId(user.getId());
        passwordResetCodeRepository.save(passwordResetCode);
        userRepository.save(user);
    }

    private void generateAndSendPasswordResetCode(User user) {
        String code = generateResetCode();

        passwordResetCodeRepository.markAllUnusedAsUsedByUserId(user.getId());

        PasswordResetCode passwordResetCode = PasswordResetCode.builder()
                .user(user)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(passwordResetExpirationMinutes))
                .used(false)
                .build();

        passwordResetCodeRepository.save(passwordResetCode);
        passwordResetEmailService.sendResetCode(user.getEmail(), code, passwordResetExpirationMinutes);
    }

    private String generateResetCode() {
        int number = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(number);
    }

    private void validateCurrentPasswordRequired(String currentPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new BusinessException("Senha atual e obrigatoria");
        }
    }

    private void validateEmailRequired(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("Email e obrigatorio");
        }
    }

    private void validateCodeRequired(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Codigo de recuperacao e obrigatorio");
        }
    }

    private void validatePasswordConfirmation(String newPassword, String confirmPassword) {
        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new BusinessException("Confirmacao de senha e obrigatoria");
        }

        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new BusinessException("Nova senha e confirmacao de senha nao coincidem");
        }
    }

    private User findAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BusinessException("Usuario autenticado nao encontrado");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException("Usuario autenticado nao encontrado"));
    }
}
