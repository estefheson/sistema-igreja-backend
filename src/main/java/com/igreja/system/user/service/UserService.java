package com.igreja.system.user.service;

import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.common.service.PasswordPolicyService;
import com.igreja.system.member.entity.Member;
import com.igreja.system.member.repository.MemberRepository;
import com.igreja.system.role.entity.Role;
import com.igreja.system.role.repository.RoleRepository;
import com.igreja.system.user.dto.UserActiveUpdateRequest;
import com.igreja.system.user.dto.UserCreateRequest;
import com.igreja.system.user.dto.UserPasswordUpdateRequest;
import com.igreja.system.user.dto.UserResponse;
import com.igreja.system.user.dto.UserUpdateRequest;
import com.igreja.system.user.entity.User;
import com.igreja.system.user.repository.UserRepository;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        validateUsername(request.username());
        validateEmail(request.email());
        validateRequiredMemberId(request.memberId());
        validateMemberAvailable(request.memberId(), null);
        passwordPolicyService.validate(request.password());

        Set<Role> roles = resolveRoles(request.roles());
        Member member = findMemberById(request.memberId());

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .member(member)
                .active(request.active() != null ? request.active() : true)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse findById(Long id) {
        User user = findUserById(id);

        return toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findUserById(id);

        validateRequiredUsername(request.username());
        validateRequiredEmail(request.email());
        validateRequiredMemberId(request.memberId());
        validateUsername(request.username(), id);
        validateEmail(request.email(), id);
        validateMemberAvailable(request.memberId(), id);

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setMember(findMemberById(request.memberId()));
        user.setActive(request.active() != null ? request.active() : user.getActive());
        user.setRoles(resolveRoles(request.roles()));

        User updatedUser = userRepository.save(user);

        return toResponse(updatedUser);
    }

    @Transactional
    public UserResponse updateActive(Long id, UserActiveUpdateRequest request) {
        User user = findUserById(id);

        if (request.active() == null) {
            throw new BusinessException("Active e obrigatorio");
        }

        if (Boolean.FALSE.equals(request.active()) && findAuthenticatedUser().getId().equals(user.getId())) {
            throw new BusinessException("Voce nao pode inativar seu proprio usuario");
        }

        user.setActive(request.active());

        User updatedUser = userRepository.save(user);

        return toResponse(updatedUser);
    }

    @Transactional
    public UserResponse updatePassword(Long id, UserPasswordUpdateRequest request) {
        User user = findUserById(id);

        passwordPolicyService.validate(request.password());

        user.setPassword(passwordEncoder.encode(request.password()));

        User updatedUser = userRepository.save(user);

        return toResponse(updatedUser);
    }

    private void validateUsername(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("Username ja cadastrado");
        }
    }

    private void validateEmail(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("Email ja cadastrado");
        }
    }

    private void validateUsername(String username, Long userId) {
        userRepository.findByUsername(username)
                .filter(existingUser -> !existingUser.getId().equals(userId))
                .ifPresent(existingUser -> {
                    throw new BusinessException("Username ja cadastrado");
                });
    }

    private void validateEmail(String email, Long userId) {
        userRepository.findByEmail(email)
                .filter(existingUser -> !existingUser.getId().equals(userId))
                .ifPresent(existingUser -> {
                    throw new BusinessException("Email ja cadastrado");
                });
    }

    private void validateRequiredUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("Username e obrigatorio");
        }
    }

    private void validateRequiredEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("Email e obrigatorio");
        }
    }

    private void validateRequiredMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException("Membro e obrigatorio");
        }
    }

    private void validateMemberAvailable(Long memberId, Long userId) {
        findMemberById(memberId);

        userRepository.findByMemberId(memberId)
                .filter(existingUser -> userId == null || !existingUser.getId().equals(userId))
                .ifPresent(existingUser -> {
                    throw new BusinessException("Membro ja vinculado a outro usuario");
                });
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario nao encontrado"));
    }

    private Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Membro nao encontrado"));
    }

    private User findAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BusinessException("Usuario autenticado nao encontrado");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException("Usuario autenticado nao encontrado"));
    }

    private Set<Role> resolveRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of();
        }

        return roleNames.stream()
                .map(this::findRoleByName)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Role findRoleByName(String roleName) {
        String normalizedRoleName = normalizeRoleName(roleName);

        return roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() -> new BusinessException("Role nao encontrada: " + normalizedRoleName));
    }

    private UserResponse toResponse(User user) {
        List<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .map(this::normalizeRoleName)
                .sorted()
                .toList();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getMember().getId(),
                user.getMember().getFullName(),
                user.getMember().getCpf(),
                user.getActive(),
                roles
        );
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new BusinessException("Role e obrigatoria");
        }

        String normalizedRoleName = roleName.trim().toUpperCase(Locale.ROOT);

        return normalizedRoleName.startsWith(ROLE_PREFIX)
                ? normalizedRoleName
                : ROLE_PREFIX + normalizedRoleName;
    }
}
