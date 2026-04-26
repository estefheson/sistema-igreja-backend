package com.igreja.system.ministry.service;

import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.member.entity.Member;
import com.igreja.system.member.repository.MemberRepository;
import com.igreja.system.ministry.dto.MinistryActiveUpdateRequest;
import com.igreja.system.ministry.dto.MinistryCreateRequest;
import com.igreja.system.ministry.dto.MinistryResponse;
import com.igreja.system.ministry.dto.MinistryUpdateRequest;
import com.igreja.system.ministry.entity.Ministry;
import com.igreja.system.ministry.entity.MinistryMember;
import com.igreja.system.ministry.repository.MinistryMemberRepository;
import com.igreja.system.ministry.repository.MinistryRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class MinistryServiceIntegrationTest {

    @Autowired
    private MinistryService ministryService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MinistryRepository ministryRepository;

    @Autowired
    private MinistryMemberRepository ministryMemberRepository;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRestrictLeaderToOwnedMinistriesAndBlockAdminOnlyActions() {
        long suffix = System.currentTimeMillis();

        Member leaderMember = memberRepository.save(Member.builder()
                .fullName("Leader Ministry " + suffix)
                .cpf(String.format("%011d", suffix % 100000000000L))
                .birthDate(LocalDate.of(1991, 5, 5))
                .email("leader.ministry." + suffix + "@teste.local")
                .phone("11999999999")
                .active(true)
                .build());

        Ministry ownedMinistry = ministryRepository.save(Ministry.builder()
                .name("Ministerio Lider " + suffix)
                .description("Ministerio do lider")
                .active(true)
                .build());

        Ministry otherMinistry = ministryRepository.save(Ministry.builder()
                .name("Ministerio Outro " + suffix)
                .description("Ministerio sem acesso")
                .active(true)
                .build());

        ministryMemberRepository.save(MinistryMember.builder()
                .ministry(ownedMinistry)
                .member(leaderMember)
                .leader(true)
                .build());

        Long userId = null;

        try {
            UserResponse createdUser = userService.create(new UserCreateRequest(
                    "leader.ministry." + suffix,
                    "leader.ministry.user." + suffix + "@teste.local",
                    "Senha@123",
                    leaderMember.getId(),
                    true,
                    List.of("ROLE_LEADER")
            ));
            userId = createdUser.id();

            authenticate(createdUser.username(), "ROLE_LEADER");

            List<MinistryResponse> visibleMinistries = ministryService.findAll();

            assertThat(visibleMinistries).extracting(MinistryResponse::id).containsExactly(ownedMinistry.getId());

            MinistryResponse updatedMinistry = ministryService.update(
                    ownedMinistry.getId(),
                    new MinistryUpdateRequest("Ministerio Lider Atualizado " + suffix, "Novo texto", true)
            );

            assertThat(updatedMinistry.name()).isEqualTo("Ministerio Lider Atualizado " + suffix);
            assertThat(updatedMinistry.active()).isTrue();

            assertThatThrownBy(() -> ministryService.findById(otherMinistry.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("permissao");

            assertThatThrownBy(() -> ministryService.update(
                    otherMinistry.getId(),
                    new MinistryUpdateRequest("Bloqueado " + suffix, "Sem acesso", true)
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("permissao");

            assertThatThrownBy(() -> ministryService.update(
                    ownedMinistry.getId(),
                    new MinistryUpdateRequest("Ministerio Lider Atualizado " + suffix, "Novo texto", false)
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("situacao");

            assertThatThrownBy(() -> ministryService.create(new MinistryCreateRequest(
                    "Ministerio Criacao Bloqueada " + suffix,
                    "Nao deve criar",
                    true
            )))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("administradores");

            assertThatThrownBy(() -> ministryService.updateActive(
                    ownedMinistry.getId(),
                    new MinistryActiveUpdateRequest(false)
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("administradores");
        } finally {
            if (userId != null) {
                userRepository.deleteById(userId);
            }
            ministryMemberRepository.deleteAll(ministryMemberRepository.findAllByMinistryId(ownedMinistry.getId()));
            ministryRepository.deleteById(otherMinistry.getId());
            ministryRepository.deleteById(ownedMinistry.getId());
            memberRepository.deleteById(leaderMember.getId());
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
