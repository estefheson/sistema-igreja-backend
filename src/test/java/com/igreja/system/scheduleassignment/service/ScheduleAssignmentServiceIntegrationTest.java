package com.igreja.system.scheduleassignment.service;

import com.igreja.system.member.entity.Member;
import com.igreja.system.member.repository.MemberRepository;
import com.igreja.system.scheduleassignment.dto.ScheduleAssignmentResponse;
import com.igreja.system.user.dto.UserCreateRequest;
import com.igreja.system.user.dto.UserResponse;
import com.igreja.system.user.repository.UserRepository;
import com.igreja.system.user.service.UserService;
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
class ScheduleAssignmentServiceIntegrationTest {

    @Autowired
    private ScheduleAssignmentService scheduleAssignmentService;

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
    void shouldReturnEmptyListForAuthenticatedMemberWithoutAssignments() {
        long suffix = System.currentTimeMillis();

        Member member = memberRepository.save(Member.builder()
                .fullName("Membro Sem Escalas " + suffix)
                .cpf(String.format("%011d", suffix % 100000000000L))
                .birthDate(LocalDate.of(1991, 3, 3))
                .email("member.no.assignments." + suffix + "@teste.local")
                .phone("11999999999")
                .active(true)
                .build());

        Long userId = null;

        try {
            UserResponse createdUser = userService.create(new UserCreateRequest(
                    "member.no.assignments." + suffix,
                    "user.no.assignments." + suffix + "@teste.local",
                    "Senha@123",
                    member.getId(),
                    true,
                    List.of("ROLE_MEMBER")
            ));
            userId = createdUser.id();

            authenticate(createdUser.username(), "ROLE_MEMBER");

            List<ScheduleAssignmentResponse> assignments = scheduleAssignmentService.findMyServiceScales(null, null);

            assertThat(assignments).isEmpty();
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
