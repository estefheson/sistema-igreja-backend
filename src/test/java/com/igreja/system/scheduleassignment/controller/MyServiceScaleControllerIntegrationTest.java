package com.igreja.system.scheduleassignment.controller;

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
import com.igreja.system.scheduleneed.entity.ScheduleNeed;
import com.igreja.system.scheduleneed.entity.ScheduleNeedStatus;
import com.igreja.system.scheduleneed.repository.ScheduleNeedRepository;
import com.igreja.system.user.dto.UserCreateRequest;
import com.igreja.system.user.dto.UserResponse;
import com.igreja.system.user.repository.UserRepository;
import com.igreja.system.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MyServiceScaleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void shouldReturnOkForLeaderOnServiceAssignmentsMinistryAgendaEndpoint() throws Exception {
        long suffix = System.currentTimeMillis();

        Member leaderMember = memberRepository.save(Member.builder()
                .fullName("Leader Endpoint " + suffix)
                .cpf(String.format("%011d", suffix % 100000000000L))
                .birthDate(LocalDate.of(1990, 3, 3))
                .email("leader.endpoint." + suffix + "@teste.local")
                .phone("11999999999")
                .active(true)
                .build());

        Ministry ministry = ministryRepository.save(Ministry.builder()
                .name("Ministerio Endpoint " + suffix)
                .description("Ministerio teste")
                .active(true)
                .build());

        ministryMemberRepository.save(MinistryMember.builder()
                .ministry(ministry)
                .member(leaderMember)
                .leader(true)
                .build());

        Room room = roomRepository.save(Room.builder()
                .name("Sala Endpoint " + suffix)
                .description("Sala teste")
                .capacity(20)
                .usageRules("Regras")
                .active(true)
                .build());

        UserResponse createdUser = userService.create(new UserCreateRequest(
                "leader.endpoint." + suffix,
                "leader.endpoint.user." + suffix + "@teste.local",
                "Senha@123",
                leaderMember.getId(),
                true,
                List.of("ROLE_LEADER")
        ));

        Long userId = createdUser.id();
        Reservation savedReservation = null;
        ScheduleNeed savedScheduleNeed = null;

        try {
            savedReservation = reservationRepository.save(Reservation.builder()
                    .room(room)
                    .reservationDate(LocalDate.of(2026, 4, 25))
                    .startTime(LocalTime.of(19, 0))
                    .endTime(LocalTime.of(21, 0))
                    .description("Culto agenda lider")
                    .status(ReservationStatus.APPROVED)
                    .createdAt(LocalDateTime.now())
                    .scheduleDemandMinistries(new LinkedHashSet<>(List.of(ministry)))
                    .build());

            savedScheduleNeed = scheduleNeedRepository.save(ScheduleNeed.builder()
                    .reservation(savedReservation)
                    .ministry(ministry)
                    .date(savedReservation.getReservationDate())
                    .startTime(savedReservation.getStartTime())
                    .endTime(savedReservation.getEndTime())
                    .status(ScheduleNeedStatus.PENDING)
                    .build());

            mockMvc.perform(get("/api/service-assignments/me/ministry-agenda")
                            .param("startDate", "2026-04-01")
                            .param("endDate", "2026-04-30")
                            .with(user(createdUser.username()).authorities(new SimpleGrantedAuthority("ROLE_LEADER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(savedScheduleNeed.getId()))
                    .andExpect(jsonPath("$[0].ministryId").value(ministry.getId()))
                    .andExpect(jsonPath("$[0].reservationId").value(savedReservation.getId()))
                    .andExpect(jsonPath("$[0].authenticatedMemberAssigned").value(false));
        } finally {
            if (savedScheduleNeed != null) {
                scheduleNeedRepository.deleteById(savedScheduleNeed.getId());
            }
            if (savedReservation != null) {
                reservationRepository.deleteById(savedReservation.getId());
            }
            userRepository.deleteById(userId);
            roomRepository.deleteById(room.getId());
            ministryMemberRepository.deleteAll(ministryMemberRepository.findAllByMinistryId(ministry.getId()));
            ministryRepository.deleteById(ministry.getId());
            memberRepository.deleteById(leaderMember.getId());
        }
    }
}
