package com.igreja.system.reservation.service;

import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.member.entity.Member;
import com.igreja.system.member.repository.MemberRepository;
import com.igreja.system.ministry.entity.Ministry;
import com.igreja.system.ministry.repository.MinistryRepository;
import com.igreja.system.reservation.dto.ReservationCreateRequest;
import com.igreja.system.reservation.dto.ReservationResponse;
import com.igreja.system.reservation.entity.Reservation;
import com.igreja.system.reservation.entity.ReservationStatus;
import com.igreja.system.reservation.repository.ReservationRepository;
import com.igreja.system.room.entity.Room;
import com.igreja.system.room.repository.RoomRepository;
import com.igreja.system.user.dto.UserCreateRequest;
import com.igreja.system.user.dto.UserResponse;
import com.igreja.system.user.entity.User;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MinistryRepository ministryRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRestrictMemberReservationVisibilityAndLeaderApprovalActions() {
        long suffix = System.currentTimeMillis();

        Member memberMember = memberRepository.save(Member.builder()
                .fullName("Member Reservation " + suffix)
                .cpf(String.format("%011d", suffix % 100000000000L))
                .birthDate(LocalDate.of(1992, 6, 6))
                .email("member.reservation." + suffix + "@teste.local")
                .phone("11999999999")
                .active(true)
                .build());

        Member leaderMember = memberRepository.save(Member.builder()
                .fullName("Leader Reservation " + suffix)
                .cpf(String.format("%011d", (suffix + 1) % 100000000000L))
                .birthDate(LocalDate.of(1990, 7, 7))
                .email("leader.reservation." + suffix + "@teste.local")
                .phone("11888888888")
                .active(true)
                .build());

        Ministry ministry = ministryRepository.save(Ministry.builder()
                .name("Ministerio Reserva " + suffix)
                .description("Ministerio teste")
                .active(true)
                .build());

        Room room = roomRepository.save(Room.builder()
                .name("Sala Reserva " + suffix)
                .description("Sala teste")
                .capacity(15)
                .usageRules("Uso livre")
                .active(true)
                .build());

        Long memberUserId = null;
        Long leaderUserId = null;
        Reservation approvedReservation = null;
        Reservation pendingReservation = null;

        try {
            UserResponse createdMemberUser = userService.create(new UserCreateRequest(
                    "member.reservation." + suffix,
                    "member.reservation.user." + suffix + "@teste.local",
                    "Senha@123",
                    memberMember.getId(),
                    true,
                    List.of("ROLE_MEMBER")
            ));
            memberUserId = createdMemberUser.id();

            UserResponse createdLeaderUser = userService.create(new UserCreateRequest(
                    "leader.reservation." + suffix,
                    "leader.reservation.user." + suffix + "@teste.local",
                    "Senha@123",
                    leaderMember.getId(),
                    true,
                    List.of("ROLE_LEADER")
            ));
            leaderUserId = createdLeaderUser.id();

            User requester = userRepository.findById(leaderUserId).orElseThrow();

            approvedReservation = reservationRepository.save(Reservation.builder()
                    .room(room)
                    .reservationDate(LocalDate.of(2026, 4, 18))
                    .startTime(LocalTime.of(19, 0))
                    .endTime(LocalTime.of(21, 0))
                    .description("Reserva aprovada")
                    .status(ReservationStatus.APPROVED)
                    .createdAt(LocalDateTime.now())
                    .requestedBy(requester)
                    .usingMinistry(ministry)
                    .build());

            pendingReservation = reservationRepository.save(Reservation.builder()
                    .room(room)
                    .reservationDate(LocalDate.of(2026, 4, 19))
                    .startTime(LocalTime.of(19, 0))
                    .endTime(LocalTime.of(21, 0))
                    .description("Reserva pendente")
                    .status(ReservationStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .requestedBy(requester)
                    .usingMinistry(ministry)
                    .build());
            Long approvedReservationId = approvedReservation.getId();
            Long pendingReservationId = pendingReservation.getId();

            authenticate(createdMemberUser.username(), "ROLE_MEMBER");

            List<ReservationResponse> memberReservations = reservationService.findAll(room.getId(), null, null);

            assertThat(memberReservations).extracting(ReservationResponse::id).containsExactly(approvedReservationId);
            assertThat(reservationService.findById(approvedReservationId).status()).isEqualTo(ReservationStatus.APPROVED);

            assertThatThrownBy(() -> reservationService.findById(pendingReservationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("permissao");

            assertThatThrownBy(() -> reservationService.create(new ReservationCreateRequest(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("criar reservas");

            authenticate(createdLeaderUser.username(), "ROLE_LEADER");

            List<ReservationResponse> leaderReservations = reservationService.findAll(room.getId(), null, null);

            assertThat(leaderReservations).extracting(ReservationResponse::id)
                    .containsExactly(approvedReservationId, pendingReservationId);
            assertThat(reservationService.findById(pendingReservationId).status()).isEqualTo(ReservationStatus.PENDING);

            assertThatThrownBy(() -> reservationService.approve(pendingReservationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("aprovar ou cancelar");
        } finally {
            if (approvedReservation != null) {
                reservationRepository.deleteById(approvedReservation.getId());
            }
            if (pendingReservation != null) {
                reservationRepository.deleteById(pendingReservation.getId());
            }
            if (leaderUserId != null) {
                userRepository.deleteById(leaderUserId);
            }
            if (memberUserId != null) {
                userRepository.deleteById(memberUserId);
            }
            roomRepository.deleteById(room.getId());
            ministryRepository.deleteById(ministry.getId());
            memberRepository.deleteById(leaderMember.getId());
            memberRepository.deleteById(memberMember.getId());
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
