package com.igreja.system.scheduleneed.service;

import com.igreja.system.member.entity.Member;
import com.igreja.system.member.repository.MemberRepository;
import com.igreja.system.ministry.entity.Ministry;
import com.igreja.system.ministry.entity.MinistryMember;
import com.igreja.system.ministry.repository.MinistryMemberRepository;
import com.igreja.system.ministry.repository.MinistryRepository;
import com.igreja.system.reservation.entity.Reservation;
import com.igreja.system.reservation.entity.ReservationStatus;
import com.igreja.system.reservation.repository.ReservationRepository;
import com.igreja.system.room.entity.Room;
import com.igreja.system.room.repository.RoomRepository;
import com.igreja.system.scheduleassignment.entity.ScheduleAssignment;
import com.igreja.system.scheduleassignment.repository.ScheduleAssignmentRepository;
import com.igreja.system.scheduleneed.dto.ScheduleNeedResponse;
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
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ScheduleNeedServiceIntegrationTest {

    @Autowired
    private ScheduleNeedService scheduleNeedService;

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

    @Autowired
    private ScheduleAssignmentRepository scheduleAssignmentRepository;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnMinisterialAgendaForLeaderEvenWhenNotAssigned() {
        long suffix = System.currentTimeMillis();

        Member leaderMember = memberRepository.save(Member.builder()
                .fullName("Lider Agenda " + suffix)
                .cpf(String.format("%011d", suffix % 100000000000L))
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("agenda.leader." + suffix + "@teste.local")
                .phone("11999999999")
                .active(true)
                .build());

        Member assignedMember = memberRepository.save(Member.builder()
                .fullName("Escalado Agenda " + suffix)
                .cpf(String.format("%011d", (suffix + 1) % 100000000000L))
                .birthDate(LocalDate.of(1991, 1, 1))
                .email("agenda.assigned." + suffix + "@teste.local")
                .phone("11888888888")
                .active(true)
                .build());

        Ministry ministry = ministryRepository.save(Ministry.builder()
                .name("Ministerio Agenda Lider " + suffix)
                .description("Ministerio teste")
                .active(true)
                .build());

        ministryMemberRepository.save(MinistryMember.builder()
                .ministry(ministry)
                .member(leaderMember)
                .leader(true)
                .build());

        ministryMemberRepository.save(MinistryMember.builder()
                .ministry(ministry)
                .member(assignedMember)
                .leader(false)
                .build());

        Room room = roomRepository.save(Room.builder()
                .name("Sala Agenda Lider " + suffix)
                .description("Sala teste")
                .capacity(20)
                .usageRules("Regras")
                .active(true)
                .build());

        UserResponse createdUser = userService.create(new UserCreateRequest(
                "agenda.leader." + suffix,
                "agenda.leader.user." + suffix + "@teste.local",
                "Senha@123",
                leaderMember.getId(),
                true,
                List.of("ROLE_LEADER")
        ));

        Long userId = createdUser.id();
        ScheduleAssignment savedAssignment = null;
        ScheduleNeed savedScheduleNeed = null;
        Reservation savedReservation = null;

        try {
            Reservation reservation = Reservation.builder()
                    .room(room)
                    .reservationDate(LocalDate.of(2026, 4, 20))
                    .startTime(LocalTime.of(19, 0))
                    .endTime(LocalTime.of(21, 0))
                    .description("Culto de jovens")
                    .status(ReservationStatus.APPROVED)
                    .createdAt(LocalDateTime.now())
                    .scheduleDemandMinistries(new LinkedHashSet<>(List.of(ministry)))
                    .build();
            savedReservation = reservationRepository.save(reservation);

            ScheduleNeed scheduleNeed = ScheduleNeed.builder()
                    .reservation(savedReservation)
                    .ministry(ministry)
                    .date(savedReservation.getReservationDate())
                    .startTime(savedReservation.getStartTime())
                    .endTime(savedReservation.getEndTime())
                    .status(ScheduleNeedStatus.FILLED)
                    .build();
            savedScheduleNeed = scheduleNeedRepository.save(scheduleNeed);

            User adminUser = userRepository.findByUsername("admin").orElse(null);
            savedAssignment = scheduleAssignmentRepository.save(ScheduleAssignment.builder()
                    .scheduleNeed(savedScheduleNeed)
                    .member(assignedMember)
                    .assignedByUser(adminUser)
                    .build());

            authenticate(createdUser.username(), "ROLE_LEADER");

            List<ScheduleNeedResponse> agenda = scheduleNeedService.findMyServiceAgenda(
                    LocalDate.of(2026, 4, 1),
                    LocalDate.of(2026, 4, 30)
            );

            assertThat(agenda).hasSize(1);

            ScheduleNeedResponse item = agenda.getFirst();
            assertThat(item.id()).isEqualTo(savedScheduleNeed.getId());
            assertThat(item.ministryId()).isEqualTo(ministry.getId());
            assertThat(item.roomName()).isEqualTo(room.getName());
            assertThat(item.authenticatedMemberAssigned()).isFalse();
            assertThat(item.assignments()).hasSize(1);
            assertThat(item.assignments().getFirst().memberId()).isEqualTo(assignedMember.getId());
        } finally {
            deleteScheduleFixtures(savedAssignment, savedScheduleNeed, savedReservation);
            userRepository.deleteById(userId);
            roomRepository.deleteById(room.getId());
            ministryMemberRepository.deleteAll(ministryMemberRepository.findAllByMinistryId(ministry.getId()));
            ministryRepository.deleteById(ministry.getId());
            memberRepository.deleteById(assignedMember.getId());
            memberRepository.deleteById(leaderMember.getId());
        }
    }

    @Test
    void shouldReturnOnlyOwnAgendaForMember() {
        long suffix = System.currentTimeMillis();

        Member authenticatedMember = memberRepository.save(Member.builder()
                .fullName("Membro Agenda " + suffix)
                .cpf(String.format("%011d", suffix % 100000000000L))
                .birthDate(LocalDate.of(1990, 2, 2))
                .email("agenda.member." + suffix + "@teste.local")
                .phone("11999999999")
                .active(true)
                .build());

        Member otherMember = memberRepository.save(Member.builder()
                .fullName("Outro Escalado " + suffix)
                .cpf(String.format("%011d", (suffix + 1) % 100000000000L))
                .birthDate(LocalDate.of(1991, 2, 2))
                .email("agenda.other." + suffix + "@teste.local")
                .phone("11888888888")
                .active(true)
                .build());

        Ministry ministry = ministryRepository.save(Ministry.builder()
                .name("Ministerio Agenda Membro " + suffix)
                .description("Ministerio teste")
                .active(true)
                .build());

        ministryMemberRepository.save(MinistryMember.builder()
                .ministry(ministry)
                .member(authenticatedMember)
                .leader(false)
                .build());

        ministryMemberRepository.save(MinistryMember.builder()
                .ministry(ministry)
                .member(otherMember)
                .leader(false)
                .build());

        Room room = roomRepository.save(Room.builder()
                .name("Sala Agenda Membro " + suffix)
                .description("Sala teste")
                .capacity(20)
                .usageRules("Regras")
                .active(true)
                .build());

        UserResponse createdUser = userService.create(new UserCreateRequest(
                "agenda.member." + suffix,
                "agenda.member.user." + suffix + "@teste.local",
                "Senha@123",
                authenticatedMember.getId(),
                true,
                List.of("ROLE_MEMBER")
        ));

        Long userId = createdUser.id();
        ScheduleAssignment ownAssignment = null;
        ScheduleAssignment otherAssignment = null;
        ScheduleNeed ownScheduleNeed = null;
        ScheduleNeed otherScheduleNeed = null;
        Reservation ownReservation = null;
        Reservation otherReservation = null;

        try {
            User adminUser = userRepository.findByUsername("admin").orElse(null);

            ownReservation = reservationRepository.save(Reservation.builder()
                    .room(room)
                    .reservationDate(LocalDate.of(2026, 4, 21))
                    .startTime(LocalTime.of(19, 0))
                    .endTime(LocalTime.of(21, 0))
                    .description("Culto membro proprio")
                    .status(ReservationStatus.APPROVED)
                    .createdAt(LocalDateTime.now())
                    .scheduleDemandMinistries(new LinkedHashSet<>(List.of(ministry)))
                    .build());

            ownScheduleNeed = scheduleNeedRepository.save(ScheduleNeed.builder()
                    .reservation(ownReservation)
                    .ministry(ministry)
                    .date(ownReservation.getReservationDate())
                    .startTime(ownReservation.getStartTime())
                    .endTime(ownReservation.getEndTime())
                    .status(ScheduleNeedStatus.FILLED)
                    .build());

            ownAssignment = scheduleAssignmentRepository.save(ScheduleAssignment.builder()
                    .scheduleNeed(ownScheduleNeed)
                    .member(authenticatedMember)
                    .assignedByUser(adminUser)
                    .build());

            otherReservation = reservationRepository.save(Reservation.builder()
                    .room(room)
                    .reservationDate(LocalDate.of(2026, 4, 22))
                    .startTime(LocalTime.of(19, 0))
                    .endTime(LocalTime.of(21, 0))
                    .description("Culto outro membro")
                    .status(ReservationStatus.APPROVED)
                    .createdAt(LocalDateTime.now())
                    .scheduleDemandMinistries(new LinkedHashSet<>(List.of(ministry)))
                    .build());

            otherScheduleNeed = scheduleNeedRepository.save(ScheduleNeed.builder()
                    .reservation(otherReservation)
                    .ministry(ministry)
                    .date(otherReservation.getReservationDate())
                    .startTime(otherReservation.getStartTime())
                    .endTime(otherReservation.getEndTime())
                    .status(ScheduleNeedStatus.FILLED)
                    .build());

            otherAssignment = scheduleAssignmentRepository.save(ScheduleAssignment.builder()
                    .scheduleNeed(otherScheduleNeed)
                    .member(otherMember)
                    .assignedByUser(adminUser)
                    .build());

            authenticate(createdUser.username(), "ROLE_MEMBER");

            List<ScheduleNeedResponse> agenda = scheduleNeedService.findMyServiceAgenda(
                    LocalDate.of(2026, 4, 1),
                    LocalDate.of(2026, 4, 30)
            );

            assertThat(agenda).hasSize(1);

            ScheduleNeedResponse item = agenda.getFirst();
            assertThat(item.id()).isEqualTo(ownScheduleNeed.getId());
            assertThat(item.authenticatedMemberAssigned()).isTrue();
            assertThat(item.assignments()).extracting(assignment -> assignment.memberId())
                    .containsExactly(authenticatedMember.getId());
        } finally {
            deleteScheduleFixtures(otherAssignment, otherScheduleNeed, otherReservation);
            deleteScheduleFixtures(ownAssignment, ownScheduleNeed, ownReservation);
            userRepository.deleteById(userId);
            roomRepository.deleteById(room.getId());
            ministryMemberRepository.deleteAll(ministryMemberRepository.findAllByMinistryId(ministry.getId()));
            ministryRepository.deleteById(ministry.getId());
            memberRepository.deleteById(otherMember.getId());
            memberRepository.deleteById(authenticatedMember.getId());
        }
    }

    private void deleteScheduleFixtures(
            ScheduleAssignment assignment,
            ScheduleNeed scheduleNeed,
            Reservation reservation
    ) {
        if (assignment != null) {
            scheduleAssignmentRepository.deleteById(assignment.getId());
        }
        if (scheduleNeed != null) {
            scheduleNeedRepository.deleteById(scheduleNeed.getId());
        }
        if (reservation != null) {
            reservationRepository.deleteById(reservation.getId());
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
