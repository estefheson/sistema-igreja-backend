package com.igreja.system.usermenupermission.service;

import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.role.entity.Role;
import com.igreja.system.user.entity.User;
import com.igreja.system.user.repository.UserRepository;
import com.igreja.system.usermenupermission.dto.MenuPermissionItemRequest;
import com.igreja.system.usermenupermission.dto.MenuPermissionItemResponse;
import com.igreja.system.usermenupermission.dto.UserMenuPermissionUpdateRequest;
import com.igreja.system.usermenupermission.dto.UserMenuPermissionsResponse;
import com.igreja.system.usermenupermission.entity.MenuKey;
import com.igreja.system.usermenupermission.entity.UserMenuPermission;
import com.igreja.system.usermenupermission.repository.UserMenuPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserMenuPermissionService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_LEADER = "ROLE_LEADER";
    private static final String ROLE_MEMBER = "ROLE_MEMBER";
    private static final Set<MenuKey> LEADER_DEFAULT_MENUS = EnumSet.of(
            MenuKey.DASHBOARD,
            MenuKey.MEMBERS,
            MenuKey.MINISTRIES,
            MenuKey.ROOM_RESERVATIONS,
            MenuKey.RESERVATIONS,
            MenuKey.SCHEDULE_NEEDS,
            MenuKey.SERVICE_SCALES
    );
    private static final Set<MenuKey> MEMBER_DEFAULT_MENUS = EnumSet.of(
            MenuKey.SERVICE_SCALES,
            MenuKey.RESERVATIONS
    );

    private final UserRepository userRepository;
    private final UserMenuPermissionRepository userMenuPermissionRepository;

    public UserMenuPermissionsResponse findMyMenuPermissions() {
        return toResponse(findAuthenticatedUser(), false);
    }

    public UserMenuPermissionsResponse findByUserId(Long userId) {
        validateAdminAccess();
        return toResponse(findUserById(userId), false);
    }

    public List<MenuKey> findAllowedMenusByUsername(String username) {
        return toResponse(findUserByUsername(username), true).allowedMenus();
    }

    public void validateAuthenticatedUserHasMenuAccess(MenuKey menuKey) {
        User user = findAuthenticatedUser();

        if (hasRole(user, ROLE_ADMIN)) {
            return;
        }

        if (!isMenuAllowed(user, menuKey)) {
            throw new BusinessException("Usuario nao possui permissao para acessar este modulo");
        }
    }

    @Transactional
    public UserMenuPermissionsResponse updateByUserId(Long userId, UserMenuPermissionUpdateRequest request) {
        validateAdminAccess();

        User user = findUserById(userId);
        List<MenuPermissionItemRequest> permissions = normalizePermissions(request);

        userMenuPermissionRepository.deleteByUserId(user.getId());

        if (!permissions.isEmpty()) {
            userMenuPermissionRepository.saveAll(permissions.stream()
                    .map(permission -> UserMenuPermission.builder()
                            .user(user)
                            .menuKey(permission.menuKey())
                            .allowed(permission.allowed())
                            .build())
                    .toList());
        }

        return toResponse(user, false);
    }

    private UserMenuPermissionsResponse toResponse(User user, boolean allowedMenusOnly) {
        Map<MenuKey, Boolean> explicitPermissions = loadExplicitPermissions(user.getId());
        List<MenuPermissionItemResponse> permissions = Arrays.stream(MenuKey.values())
                .map(menuKey -> toMenuPermissionResponse(user, menuKey, explicitPermissions))
                .toList();
        List<MenuKey> allowedMenus = permissions.stream()
                .filter(MenuPermissionItemResponse::allowed)
                .map(MenuPermissionItemResponse::menuKey)
                .toList();

        return new UserMenuPermissionsResponse(
                user.getId(),
                user.getUsername(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .sorted()
                        .toList(),
                allowedMenus,
                allowedMenusOnly ? List.of() : permissions
        );
    }

    private Map<MenuKey, Boolean> loadExplicitPermissions(Long userId) {
        Map<MenuKey, Boolean> permissionsByMenu = new EnumMap<>(MenuKey.class);

        userMenuPermissionRepository.findAllByUserIdOrderByMenuKeyAsc(userId)
                .forEach(permission -> permissionsByMenu.put(permission.getMenuKey(), permission.getAllowed()));

        return permissionsByMenu;
    }

    private MenuPermissionItemResponse toMenuPermissionResponse(
            User user,
            MenuKey menuKey,
            Map<MenuKey, Boolean> explicitPermissions
    ) {
        if (hasRole(user, ROLE_ADMIN)) {
            return new MenuPermissionItemResponse(menuKey, true, explicitPermissions.containsKey(menuKey));
        }

        boolean explicit = explicitPermissions.containsKey(menuKey);
        boolean hasNoExplicitPermissions = explicitPermissions.isEmpty();
        boolean allowed = explicit
                ? Boolean.TRUE.equals(explicitPermissions.get(menuKey))
                : isAllowedByDefault(user, menuKey, hasNoExplicitPermissions);

        return new MenuPermissionItemResponse(menuKey, allowed, explicit);
    }

    private boolean isAllowedByDefault(User user, MenuKey menuKey, boolean hasNoExplicitPermissions) {
        if (hasRole(user, ROLE_ADMIN)) {
            return true;
        }

        if (!hasNoExplicitPermissions) {
            return false;
        }

        if (hasRole(user, ROLE_LEADER) && LEADER_DEFAULT_MENUS.contains(menuKey)) {
            return true;
        }

        return hasRole(user, ROLE_MEMBER) && MEMBER_DEFAULT_MENUS.contains(menuKey);
    }

    private boolean isMenuAllowed(User user, MenuKey menuKey) {
        Map<MenuKey, Boolean> explicitPermissions = loadExplicitPermissions(user.getId());
        boolean explicit = explicitPermissions.containsKey(menuKey);

        return explicit
                ? Boolean.TRUE.equals(explicitPermissions.get(menuKey))
                : isAllowedByDefault(user, menuKey, explicitPermissions.isEmpty());
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(roleName::equals);
    }

    private List<MenuPermissionItemRequest> normalizePermissions(UserMenuPermissionUpdateRequest request) {
        if (request == null || request.permissions() == null || request.permissions().isEmpty()) {
            return List.of();
        }

        Set<MenuKey> informedMenus = EnumSet.noneOf(MenuKey.class);
        List<MenuPermissionItemRequest> permissions = request.permissions().stream()
                .map(permission -> {
                    if (permission.menuKey() == null) {
                        throw new BusinessException("Menu e obrigatorio");
                    }

                    if (permission.allowed() == null) {
                        throw new BusinessException("Campo allowed da permissao de menu e obrigatorio");
                    }

                    if (!informedMenus.add(permission.menuKey())) {
                        throw new BusinessException("Nao e permitido informar menus duplicados");
                    }

                    return permission;
                })
                .toList();

        return permissions;
    }

    private void validateAdminAccess() {
        if (!hasRole(findAuthenticatedUser(), ROLE_ADMIN)) {
            throw new BusinessException("Usuario nao possui permissao para administrar menus");
        }
    }

    private User findAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BusinessException("Usuario autenticado nao encontrado");
        }

        return findUserByUsername(authentication.getName());
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuario nao encontrado"));
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Usuario nao encontrado"));
    }
}
