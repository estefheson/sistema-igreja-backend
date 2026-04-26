package com.igreja.system.usermenupermission.service;

import com.igreja.system.member.entity.Member;
import com.igreja.system.member.repository.MemberRepository;
import com.igreja.system.user.dto.UserCreateRequest;
import com.igreja.system.user.dto.UserResponse;
import com.igreja.system.user.repository.UserRepository;
import com.igreja.system.user.service.UserService;
import com.igreja.system.usermenupermission.dto.MenuPermissionItemRequest;
import com.igreja.system.usermenupermission.dto.UserMenuPermissionUpdateRequest;
import com.igreja.system.usermenupermission.dto.UserMenuPermissionsResponse;
import com.igreja.system.usermenupermission.entity.MenuKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserMenuPermissionServiceIntegrationTest {

    @Autowired
    private UserMenuPermissionService userMenuPermissionService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnDefaultMenusForLeaderWithoutExplicitPermissions() {
        long suffix = System.currentTimeMillis();

        Member member = memberRepository.save(Member.builder()
                .fullName("Lider Permissoes " + suffix)
                .cpf(String.format("%011d", suffix % 100000000000L))
                .birthDate(LocalDate.of(1990, 2, 2))
                .email("menu.leader." + suffix + "@teste.local")
                .phone("11999999999")
                .active(true)
                .build());

        Long userId = null;

        try {
            UserResponse createdUser = userService.create(new UserCreateRequest(
                    "menu.leader." + suffix,
                    "menu.leader.user." + suffix + "@teste.local",
                    "Senha@123",
                    member.getId(),
                    true,
                    List.of("ROLE_LEADER")
            ));
            userId = createdUser.id();

            authenticate(createdUser.username(), "ROLE_LEADER");

            UserMenuPermissionsResponse myPermissions = userMenuPermissionService.findMyMenuPermissions();

            assertThat(myPermissions.allowedMenus()).containsExactly(
                    MenuKey.DASHBOARD,
                    MenuKey.MEMBERS,
                    MenuKey.MINISTRIES,
                    MenuKey.ROOM_RESERVATIONS,
                    MenuKey.RESERVATIONS,
                    MenuKey.SCHEDULE_NEEDS,
                    MenuKey.SERVICE_SCALES
            );
        } finally {
            if (userId != null) {
                userRepository.deleteById(userId);
            }
            memberRepository.deleteById(member.getId());
        }
    }

    @Test
    void shouldReturnRoleDefaultsForMemberAndAllowAdminOverrides() {
        long suffix = System.currentTimeMillis();

        Member member = memberRepository.save(Member.builder()
                .fullName("Membro Permissoes " + suffix)
                .cpf(String.format("%011d", suffix % 100000000000L))
                .birthDate(LocalDate.of(1992, 2, 2))
                .email("menu.member." + suffix + "@teste.local")
                .phone("11999999999")
                .active(true)
                .build());

        Long userId = null;

        try {
            UserResponse createdUser = userService.create(new UserCreateRequest(
                    "menu.member." + suffix,
                    "menu.user." + suffix + "@teste.local",
                    "Senha@123",
                    member.getId(),
                    true,
                    List.of("ROLE_MEMBER")
            ));
            userId = createdUser.id();

            authenticate(createdUser.username(), "ROLE_MEMBER");

            UserMenuPermissionsResponse myPermissions = userMenuPermissionService.findMyMenuPermissions();

            assertThat(myPermissions.allowedMenus()).containsExactly(
                    MenuKey.RESERVATIONS,
                    MenuKey.SERVICE_SCALES
            );

            authenticate("admin", "ROLE_ADMIN");

            UserMenuPermissionsResponse updatedPermissions = userMenuPermissionService.updateByUserId(
                    createdUser.id(),
                    new UserMenuPermissionUpdateRequest(List.of(
                            new MenuPermissionItemRequest(MenuKey.ROOMS, true),
                            new MenuPermissionItemRequest(MenuKey.RESERVATIONS, true)
                    ))
            );

            assertThat(updatedPermissions.allowedMenus())
                    .containsExactly(MenuKey.ROOMS, MenuKey.RESERVATIONS);
        } finally {
            if (userId != null) {
                userRepository.deleteById(userId);
            }
            memberRepository.deleteById(member.getId());
        }
    }

    private void authenticate(String username, String... roles) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        java.util.Arrays.stream(roles)
                                .map(SimpleGrantedAuthority::new)
                                .toList()
                )
        );
    }
}
