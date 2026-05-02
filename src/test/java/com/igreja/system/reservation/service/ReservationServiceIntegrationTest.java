package com.igreja.system.reservation.service;

import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.member.entity.Member;
import com.igreja.system.member.repository.MemberRepository;
import com.igreja.system.ministry.entity.Ministry;
import com.igreja.system.ministry.entity.MinistryMember;
import com.igreja.system.ministry.repository.MinistryMemberRepository;
import com.igreja.system.ministry.repository.MinistryRepository;
import com.igreja.system.reservation.dto.ReservationCalendarSummaryResponse;
import com.igreja.system.reservation.dto.ReservationCreateRequest;
import com.igreja.system.reservation.dto.ReservationResponse;
import com.igreja.system.reservation.entity.Reservation;
import com.igreja.system.reservation.entity.ReservationStatus;
import com.igreja.system.reservation.repository.ReservationRepository;
import com.igreja.system.room.entity.Room;
import com.igreja.system.room.entity.RoomReservationRule;
import com.igreja.system.room.repository.RoomRepository;
import com.igreja.system.scheduleneed.entity.ScheduleNeed;
import com.igreja.system.scheduleneed.entity.ScheduleNeedStatus;
import com.igreja.system.scheduleneed.repository.ScheduleNeedRepository;
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
import java.util.Comparator;
import java.util.LinkedHashSet;
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
    private MinistryMemberRepository ministryMemberRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ScheduleNeedRepository scheduleNeedRepository;

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

    @Test
    void shouldCreateScheduleNeedsForUsingAndDemandMinistriesWithoutDuplicates() {
        long suffix = System.currentTimeMillis();
        LocalDate reservationDate = LocalDate.of(2026, 5, 4);

        Member leaderMember = memberRepository.save(Member.builder()
                .fullName("Leader Need " + suffix)
                .cpf(String.format("%011d", suffix % 100000000000L))
                .birthDate(LocalDate.of(1991, 8, 8))
                .email("leader.need." + suffix + "@teste.local")
                .phone("11777777777")
                .active(true)
                .build());

        Member adminMember = memberRepository.save(Member.builder()
                .fullName("Admin Need " + suffix)
                .cpf(String.format("%011d", (suffix + 1) % 100000000000L))
                .birthDate(LocalDate.of(1989, 9, 9))
                .email("admin.need." + suffix + "@teste.local")
                .phone("11666666666")
                .active(true)
                .build());

        Ministry usingMinistry = ministryRepository.save(Ministry.builder()
                .name("Ministerio Uso Need " + suffix)
                .description("Ministerio de uso")
                .active(true)
                .build());

        Ministry demandMinistry = ministryRepository.save(Ministry.builder()
                .name("Ministerio Demanda Need " + suffix)
                .description("Ministerio de demanda")
                .active(true)
                .build());

        Room room = Room.builder()
                .name("Sala Need " + suffix)
                .description("Sala teste need")
                .capacity(20)
                .usageRules("Uso livre")
                .active(true)
                .build();
        room.addReservationRule(RoomReservationRule.builder()
                .dayOfWeek(reservationDate.getDayOfWeek())
                .enabled(true)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(22, 0))
                .build());
        room = roomRepository.save(room);

        Long leaderUserId = null;
        Long adminUserId = null;
        Long reservationId = null;
        Long ministryMemberId = null;

        try {
            ministryMemberId = ministryMemberRepository.save(MinistryMember.builder()
                    .ministry(usingMinistry)
                    .member(leaderMember)
                    .leader(true)
                    .build()).getId();

            UserResponse createdLeaderUser = userService.create(new UserCreateRequest(
                    "leader.need." + suffix,
                    "leader.need.user." + suffix + "@teste.local",
                    "Senha@123",
                    leaderMember.getId(),
                    true,
                    List.of("ROLE_LEADER")
            ));
            leaderUserId = createdLeaderUser.id();

            UserResponse createdAdminUser = userService.create(new UserCreateRequest(
                    "admin.need." + suffix,
                    "admin.need.user." + suffix + "@teste.local",
                    "Senha@123",
                    adminMember.getId(),
                    true,
                    List.of("ROLE_ADMIN")
            ));
            adminUserId = createdAdminUser.id();

            authenticate(createdLeaderUser.username(), "ROLE_LEADER");

            ReservationResponse createdReservation = reservationService.create(new ReservationCreateRequest(
                    room.getId(),
                    reservationDate,
                    LocalTime.of(19, 0),
                    LocalTime.of(21, 0),
                    "Reserva com necessidades",
                    usingMinistry.getId(),
                    List.of(usingMinistry.getId(), demandMinistry.getId(), demandMinistry.getId())
            ));
            reservationId = createdReservation.id();
            Long createdReservationId = createdReservation.id();

            assertThat(reservationRepository.existsById(createdReservationId)).isTrue();
            assertThat(createdReservation.status()).isEqualTo(ReservationStatus.PENDING);

            List<ScheduleNeed> createdNeeds = scheduleNeedRepository.findAll().stream()
                    .filter(scheduleNeed -> createdReservationId.equals(scheduleNeed.getReservation().getId()))
                    .sorted(Comparator.comparing(ScheduleNeed::getId))
                    .toList();

            assertThat(createdNeeds).hasSize(2);
            assertThat(createdNeeds)
                    .extracting(scheduleNeed -> scheduleNeed.getMinistry().getId())
                    .containsExactlyInAnyOrder(usingMinistry.getId(), demandMinistry.getId());
            assertThat(createdNeeds)
                    .extracting(ScheduleNeed::getStatus)
                    .containsOnly(ScheduleNeedStatus.PENDING);

            authenticate(createdAdminUser.username(), "ROLE_ADMIN");

            ReservationResponse approvedReservation = reservationService.approve(createdReservationId);

            assertThat(approvedReservation.status()).isEqualTo(ReservationStatus.APPROVED);

            List<ScheduleNeed> needsAfterApproval = scheduleNeedRepository.findAll().stream()
                    .filter(scheduleNeed -> createdReservationId.equals(scheduleNeed.getReservation().getId()))
                    .toList();

            assertThat(needsAfterApproval).hasSize(2);
            assertThat(needsAfterApproval)
                    .extracting(scheduleNeed -> scheduleNeed.getMinistry().getId())
                    .containsExactlyInAnyOrder(usingMinistry.getId(), demandMinistry.getId());
        } finally {
            if (reservationId != null) {
                Long cleanupReservationId = reservationId;
                scheduleNeedRepository.deleteAll(scheduleNeedRepository.findAll().stream()
                        .filter(scheduleNeed -> cleanupReservationId.equals(scheduleNeed.getReservation().getId()))
                        .toList());
                reservationRepository.deleteById(reservationId);
            }
            if (leaderUserId != null) {
                userRepository.deleteById(leaderUserId);
            }
            if (adminUserId != null) {
                userRepository.deleteById(adminUserId);
            }
            if (ministryMemberId != null) {
                ministryMemberRepository.deleteById(ministryMemberId);
            }
            roomRepository.deleteById(room.getId());
            ministryRepository.deleteById(demandMinistry.getId());
            ministryRepository.deleteById(usingMinistry.getId());
            memberRepository.deleteById(adminMember.getId());
            memberRepository.deleteById(leaderMember.getId());
        }
    }

    @Test
    void shouldReturnCalendarSummaryRespectingVisibilityRules() {
        long suffix = System.currentTimeMillis();
        LocalDate firstDate = LocalDate.of(2026, 6, 15);
        LocalDate secondDate = firstDate.plusDays(1);

        Member leaderMember = memberRepository.save(Member.builder()
                .fullName("Leader Calendar " + suffix)
                .cpf(String.format("%011d", suffix % 100000000000L))
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("leader.calendar." + suffix + "@teste.local")
                .phone("11555555555")
                .active(true)
                .build());

        Member adminMember = memberRepository.save(Member.builder()
                .fullName("Admin Calendar " + suffix)
                .cpf(String.format("%011d", (suffix + 1) % 100000000000L))
                .birthDate(LocalDate.of(1988, 2, 2))
                .email("admin.calendar." + suffix + "@teste.local")
                .phone("11444444444")
                .active(true)
                .build());

        Member memberMember = memberRepository.save(Member.builder()
                .fullName("Member Calendar " + suffix)
                .cpf(String.format("%011d", (suffix + 2) % 100000000000L))
                .birthDate(LocalDate.of(1995, 3, 3))
                .email("member.calendar." + suffix + "@teste.local")
                .phone("11333333333")
                .active(true)
                .build());

        Ministry leaderMinistry = ministryRepository.save(Ministry.builder()
                .name("Ministerio Lider Calendar " + suffix)
                .description("Ministerio liderado")
                .active(true)
                .build());

        Ministry otherMinistry = ministryRepository.save(Ministry.builder()
                .name("Ministerio Outro Calendar " + suffix)
                .description("Ministerio nao liderado")
                .active(true)
                .build());

        Room room = roomRepository.save(Room.builder()
                .name("Sala Calendar " + suffix)
                .description("Sala calendario")
                .capacity(25)
                .usageRules("Uso livre")
                .active(true)
                .build());

        Long leaderUserId = null;
        Long adminUserId = null;
        Long memberUserId = null;
        Long ministryMemberId = null;
        Long ownPendingReservationId = null;
        Long linkedApprovedReservationId = null;
        Long unrelatedApprovedReservationId = null;
        Long linkedCancelledReservationId = null;

        try {
            ministryMemberId = ministryMemberRepository.save(MinistryMember.builder()
                    .ministry(leaderMinistry)
                    .member(leaderMember)
                    .leader(true)
                    .build()).getId();

            UserResponse createdLeaderUser = userService.create(new UserCreateRequest(
                    "leader.calendar." + suffix,
                    "leader.calendar.user." + suffix + "@teste.local",
                    "Senha@123",
                    leaderMember.getId(),
                    true,
                    List.of("ROLE_LEADER")
            ));
            leaderUserId = createdLeaderUser.id();

            UserResponse createdAdminUser = userService.create(new UserCreateRequest(
                    "admin.calendar." + suffix,
                    "admin.calendar.user." + suffix + "@teste.local",
                    "Senha@123",
                    adminMember.getId(),
                    true,
                    List.of("ROLE_ADMIN")
            ));
            adminUserId = createdAdminUser.id();

            UserResponse createdMemberUser = userService.create(new UserCreateRequest(
                    "member.calendar." + suffix,
                    "member.calendar.user." + suffix + "@teste.local",
                    "Senha@123",
                    memberMember.getId(),
                    true,
                    List.of("ROLE_MEMBER")
            ));
            memberUserId = createdMemberUser.id();

            User leaderUser = userRepository.findById(leaderUserId).orElseThrow();
            User adminUser = userRepository.findById(adminUserId).orElseThrow();

            ownPendingReservationId = reservationRepository.save(Reservation.builder()
                    .room(room)
                    .reservationDate(firstDate)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(9, 0))
                    .description("Reserva propria pendente")
                    .status(ReservationStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .requestedBy(leaderUser)
                    .usingMinistry(otherMinistry)
                    .build()).getId();

            linkedApprovedReservationId = reservationRepository.save(Reservation.builder()
                    .room(room)
                    .reservationDate(firstDate)
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(11, 0))
                    .description("Reserva vinculada aprovada")
                    .status(ReservationStatus.APPROVED)
                    .createdAt(LocalDateTime.now())
                    .requestedBy(adminUser)
                    .usingMinistry(otherMinistry)
                    .scheduleDemandMinistries(new LinkedHashSet<>(List.of(leaderMinistry)))
                    .build()).getId();

            unrelatedApprovedReservationId = reservationRepository.save(Reservation.builder()
                    .room(room)
                    .reservationDate(firstDate)
                    .startTime(LocalTime.of(12, 0))
                    .endTime(LocalTime.of(13, 0))
                    .description("Reserva aprovada sem vinculo")
                    .status(ReservationStatus.APPROVED)
                    .createdAt(LocalDateTime.now())
                    .requestedBy(adminUser)
                    .usingMinistry(otherMinistry)
                    .build()).getId();

            linkedCancelledReservationId = reservationRepository.save(Reservation.builder()
                    .room(room)
                    .reservationDate(secondDate)
                    .startTime(LocalTime.of(14, 0))
                    .endTime(LocalTime.of(15, 0))
                    .description("Reserva vinculada cancelada")
                    .status(ReservationStatus.CANCELLED)
                    .createdAt(LocalDateTime.now())
                    .requestedBy(adminUser)
                    .usingMinistry(leaderMinistry)
                    .build()).getId();

            authenticate(createdAdminUser.username(), "ROLE_ADMIN");
            assertThat(reservationService.findCalendarSummary(firstDate, secondDate)).containsExactly(
                    new ReservationCalendarSummaryResponse(firstDate, 2, 3),
                    new ReservationCalendarSummaryResponse(secondDate, 0, 1)
            );

            authenticate(createdLeaderUser.username(), "ROLE_LEADER");
            assertThat(reservationService.findCalendarSummary(firstDate, secondDate)).containsExactly(
                    new ReservationCalendarSummaryResponse(firstDate, 1, 2),
                    new ReservationCalendarSummaryResponse(secondDate, 0, 1)
            );

            authenticate(createdMemberUser.username(), "ROLE_MEMBER");
            assertThat(reservationService.findCalendarSummary(firstDate, secondDate)).containsExactly(
                    new ReservationCalendarSummaryResponse(firstDate, 2, 2)
            );

            authenticate(createdAdminUser.username(), "ROLE_ADMIN");
            assertThat(reservationService.findCalendarSummary(secondDate.plusDays(1), secondDate.plusDays(2))).isEmpty();
        } finally {
            if (linkedCancelledReservationId != null) {
                reservationRepository.deleteById(linkedCancelledReservationId);
            }
            if (unrelatedApprovedReservationId != null) {
                reservationRepository.deleteById(unrelatedApprovedReservationId);
            }
            if (linkedApprovedReservationId != null) {
                reservationRepository.deleteById(linkedApprovedReservationId);
            }
            if (ownPendingReservationId != null) {
                reservationRepository.deleteById(ownPendingReservationId);
            }
            if (leaderUserId != null) {
                userRepository.deleteById(leaderUserId);
            }
            if (adminUserId != null) {
                userRepository.deleteById(adminUserId);
            }
            if (memberUserId != null) {
                userRepository.deleteById(memberUserId);
            }
            if (ministryMemberId != null) {
                ministryMemberRepository.deleteById(ministryMemberId);
            }
            roomRepository.deleteById(room.getId());
            ministryRepository.deleteById(otherMinistry.getId());
            ministryRepository.deleteById(leaderMinistry.getId());
            memberRepository.deleteById(memberMember.getId());
            memberRepository.deleteById(adminMember.getId());
            memberRepository.deleteById(leaderMember.getId());
        }
    }

    @Test
    void shouldReturnPendingReservationsOnlyForAdminOrderedByDateAndTime() {
        long suffix = System.currentTimeMillis();

        Member adminMember = memberRepository.save(Member.builder()
                .fullName("Admin Pending " + suffix)
                .cpf(String.format("%011d", suffix % 100000000000L))
                .birthDate(LocalDate.of(1987, 4, 4))
                .email("admin.pending." + suffix + "@teste.local")
                .phone("11222222222")
                .active(true)
                .build());

        Member leaderMember = memberRepository.save(Member.builder()
                .fullName("Leader Pending " + suffix)
                .cpf(String.format("%011d", (suffix + 1) % 100000000000L))
                .birthDate(LocalDate.of(1991, 5, 5))
                .email("leader.pending." + suffix + "@teste.local")
                .phone("11111111111")
                .active(true)
                .build());

        Member requesterMember = memberRepository.save(Member.builder()
                .fullName("Solicitante Pending " + suffix)
                .cpf(String.format("%011d", (suffix + 2) % 100000000000L))
                .birthDate(LocalDate.of(1993, 6, 6))
                .email("requester.pending." + suffix + "@teste.local")
                .phone("11000000000")
                .active(true)
                .build());

        Ministry usingMinistry = ministryRepository.save(Ministry.builder()
                .name("Ministerio Uso Pending " + suffix)
                .description("Ministerio de uso")
                .active(true)
                .build());

        Ministry demandMinistry = ministryRepository.save(Ministry.builder()
                .name("Ministerio Demanda Pending " + suffix)
                .description("Ministerio de demanda")
                .active(true)
                .build());

        Room room = roomRepository.save(Room.builder()
                .name("Sala Pending " + suffix)
                .description("Sala pendente")
                .capacity(30)
                .usageRules("Uso livre")
                .active(true)
                .build());

        Long adminUserId = null;
        Long leaderUserId = null;
        Long requesterUserId = null;
        Long firstPendingReservationId = null;
        Long secondPendingReservationId = null;
        Long thirdPendingReservationId = null;
        Long approvedReservationId = null;

        try {
            UserResponse createdAdminUser = userService.create(new UserCreateRequest(
                    "admin.pending." + suffix,
                    "admin.pending.user." + suffix + "@teste.local",
                    "Senha@123",
                    adminMember.getId(),
                    true,
                    List.of("ROLE_ADMIN")
            ));
            adminUserId = createdAdminUser.id();

            UserResponse createdLeaderUser = userService.create(new UserCreateRequest(
                    "leader.pending." + suffix,
                    "leader.pending.user." + suffix + "@teste.local",
                    "Senha@123",
                    leaderMember.getId(),
                    true,
                    List.of("ROLE_LEADER")
            ));
            leaderUserId = createdLeaderUser.id();

            UserResponse createdRequesterUser = userService.create(new UserCreateRequest(
                    "requester.pending." + suffix,
                    "requester.pending.user." + suffix + "@teste.local",
                    "Senha@123",
                    requesterMember.getId(),
                    true,
                    List.of("ROLE_LEADER")
            ));
            requesterUserId = createdRequesterUser.id();

            User requesterUser = userRepository.findById(requesterUserId).orElseThrow();

            thirdPendingReservationId = reservationRepository.save(Reservation.builder()
                    .room(room)
                    .reservationDate(LocalDate.of(2026, 7, 3))
                    .startTime(LocalTime.of(11, 0))
                    .endTime(LocalTime.of(12, 0))
                    .description("Terceira pendente")
                    .status(ReservationStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .requestedBy(requesterUser)
                    .usingMinistry(usingMinistry)
                    .scheduleDemandMinistries(new LinkedHashSet<>(List.of(demandMinistry)))
                    .build()).getId();

            firstPendingReservationId = reservationRepository.save(Reservation.builder()
                    .room(room)
                    .reservationDate(LocalDate.of(2026, 7, 1))
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(9, 0))
                    .description("Primeira pendente")
                    .status(ReservationStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .requestedBy(requesterUser)
                    .usingMinistry(usingMinistry)
                    .scheduleDemandMinistries(new LinkedHashSet<>(List.of(demandMinistry)))
                    .build()).getId();

            secondPendingReservationId = reservationRepository.save(Reservation.builder()
                    .room(room)
                    .reservationDate(LocalDate.of(2026, 7, 1))
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(11, 0))
                    .description("Segunda pendente")
                    .status(ReservationStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .requestedBy(requesterUser)
                    .usingMinistry(usingMinistry)
                    .build()).getId();

            approvedReservationId = reservationRepository.save(Reservation.builder()
                    .room(room)
                    .reservationDate(LocalDate.of(2026, 7, 2))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 0))
                    .description("Reserva aprovada")
                    .status(ReservationStatus.APPROVED)
                    .createdAt(LocalDateTime.now())
                    .requestedBy(requesterUser)
                    .usingMinistry(usingMinistry)
                    .build()).getId();

            authenticate(createdAdminUser.username(), "ROLE_ADMIN");

            List<ReservationResponse> pendingReservations = reservationService.findPending();

            assertThat(pendingReservations).extracting(ReservationResponse::id).containsExactly(
                    firstPendingReservationId,
                    secondPendingReservationId,
                    thirdPendingReservationId
            );
            assertThat(pendingReservations).extracting(ReservationResponse::status)
                    .containsOnly(ReservationStatus.PENDING);
            assertThat(pendingReservations.getFirst().roomName()).isEqualTo(room.getName());
            assertThat(pendingReservations.getFirst().requesterName()).isEqualTo(requesterMember.getFullName());
            assertThat(pendingReservations.getFirst().usingMinistryName()).isEqualTo(usingMinistry.getName());
            assertThat(pendingReservations.getFirst().description()).isEqualTo("Primeira pendente");
            assertThat(pendingReservations.getFirst().scheduleDemandMinistryNames()).containsExactly(demandMinistry.getName());

            authenticate(createdLeaderUser.username(), "ROLE_LEADER");

            assertThatThrownBy(() -> reservationService.findPending())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("reservas pendentes");
        } finally {
            if (approvedReservationId != null) {
                reservationRepository.deleteById(approvedReservationId);
            }
            if (thirdPendingReservationId != null) {
                reservationRepository.deleteById(thirdPendingReservationId);
            }
            if (secondPendingReservationId != null) {
                reservationRepository.deleteById(secondPendingReservationId);
            }
            if (firstPendingReservationId != null) {
                reservationRepository.deleteById(firstPendingReservationId);
            }
            if (requesterUserId != null) {
                userRepository.deleteById(requesterUserId);
            }
            if (leaderUserId != null) {
                userRepository.deleteById(leaderUserId);
            }
            if (adminUserId != null) {
                userRepository.deleteById(adminUserId);
            }
            roomRepository.deleteById(room.getId());
            ministryRepository.deleteById(demandMinistry.getId());
            ministryRepository.deleteById(usingMinistry.getId());
            memberRepository.deleteById(requesterMember.getId());
            memberRepository.deleteById(leaderMember.getId());
            memberRepository.deleteById(adminMember.getId());
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
