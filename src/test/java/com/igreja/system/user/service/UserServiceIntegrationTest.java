package com.igreja.system.user.service;

import com.igreja.system.member.entity.Member;
import com.igreja.system.member.repository.MemberRepository;
import com.igreja.system.user.dto.UserCreateRequest;
import com.igreja.system.user.dto.UserResponse;
import com.igreja.system.user.dto.UserUpdateRequest;
import com.igreja.system.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void shouldAllowRoleMemberToCoexistWithOtherRoles() {
        long suffix = System.currentTimeMillis();

        Member member = memberRepository.save(Member.builder()
                .fullName("Membro Teste Roles " + suffix)
                .cpf(String.format("%011d", suffix % 100000000000L))
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("member.roles." + suffix + "@teste.local")
                .phone("11999999999")
                .active(true)
                .build());

        Long userId = null;

        try {
            UserResponse createdUser = userService.create(new UserCreateRequest(
                    "user.roles." + suffix,
                    "user.roles." + suffix + "@teste.local",
                    "Senha@123",
                    member.getId(),
                    true,
                    List.of("ROLE_MEMBER")
            ));

            userId = createdUser.id();

            assertThat(createdUser.roles()).containsExactly("ROLE_MEMBER");

            UserResponse updatedUser = userService.update(createdUser.id(), new UserUpdateRequest(
                    "user.roles.updated." + suffix,
                    "user.roles.updated." + suffix + "@teste.local",
                    member.getId(),
                    true,
                    List.of("ROLE_ADMIN", "ROLE_LEADER", "ROLE_MEMBER")
            ));

            assertThat(updatedUser.roles())
                    .containsExactly("ROLE_ADMIN", "ROLE_LEADER", "ROLE_MEMBER");
        } finally {
            if (userId != null) {
                userRepository.deleteById(userId);
            }
            memberRepository.deleteById(member.getId());
        }
    }
}
