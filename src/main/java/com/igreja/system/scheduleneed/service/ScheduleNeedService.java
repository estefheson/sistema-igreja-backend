package com.igreja.system.scheduleneed.service;

import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.ministry.entity.Ministry;
import com.igreja.system.ministry.repository.MinistryMemberRepository;
import com.igreja.system.reservation.entity.Reservation;
import com.igreja.system.reservation.entity.ReservationStatus;
import com.igreja.system.reservation.repository.ReservationRepository;
import com.igreja.system.scheduleassignment.dto.ScheduleAssignmentResponse;
import com.igreja.system.scheduleassignment.service.ScheduleAssignmentService;
import com.igreja.system.scheduleneed.dto.ScheduleNeedAssignedMemberResponse;
import com.igreja.system.scheduleneed.dto.ScheduleNeedResponse;
import com.igreja.system.scheduleneed.entity.ScheduleNeed;
import com.igreja.system.scheduleneed.entity.ScheduleNeedStatus;
import com.igreja.system.scheduleneed.repository.ScheduleNeedRepository;
import com.igreja.system.user.entity.User;
import com.igreja.system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleNeedService {

    private final ScheduleNeedRepository scheduleNeedRepository;
    private final ReservationRepository reservationRepository;
    private final ScheduleAssignmentService scheduleAssignmentService;
    private final MinistryMemberRepository ministryMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public List<ScheduleNeedResponse> createForReservation(Reservation reservation) {
        return reservation.getScheduleDemandMinistries()
                .stream()
                .filter(ministry -> !scheduleNeedRepository.existsByReservationIdAndMinistryId(
                        reservation.getId(),
                        ministry.getId()
                ))
                .map(ministry -> scheduleNeedRepository.save(createScheduleNeed(reservation, ministry)))
                .map(scheduleNeed -> toResponse(scheduleNeed, List.of(), null))
                .toList();
    }

    public List<ScheduleNeedResponse> findAll(ScheduleNeedStatus status, LocalDate date) {
        List<ScheduleNeed> scheduleNeeds = findVisibleScheduleNeeds(null, status, date);
        Map<Long, List<ScheduleAssignmentResponse>> assignmentsByScheduleNeedId = loadAssignmentsByScheduleNeedId(scheduleNeeds);

        return scheduleNeeds.stream()
                .map(scheduleNeed -> toResponse(
                        scheduleNeed,
                        assignmentsByScheduleNeedId.getOrDefault(scheduleNeed.getId(), List.of()),
                        null
                ))
                .toList();
    }

    public ScheduleNeedResponse findById(Long id) {
        ScheduleNeed scheduleNeed = scheduleNeedRepository.findApprovedDetailedById(id, ReservationStatus.APPROVED)
                .orElseThrow(() -> new BusinessException("Necessidade de escala nao encontrada ou vinculada a uma reserva nao aprovada"));
        validateCanViewScheduleNeed(scheduleNeed);

        List<ScheduleAssignmentResponse> assignments = scheduleAssignmentService.findAllByScheduleNeedIds(List.of(scheduleNeed.getId()));

        return toResponse(scheduleNeed, assignments, null);
    }

    public List<ScheduleNeedResponse> findByReservationId(Long reservationId, ScheduleNeedStatus status, LocalDate date) {
        if (!reservationRepository.existsById(reservationId)) {
            throw new BusinessException("Reserva nao encontrada");
        }

        List<ScheduleNeed> scheduleNeeds = findVisibleScheduleNeeds(reservationId, status, date);
        Map<Long, List<ScheduleAssignmentResponse>> assignmentsByScheduleNeedId = loadAssignmentsByScheduleNeedId(scheduleNeeds);

        return scheduleNeeds.stream()
                .map(scheduleNeed -> toResponse(
                        scheduleNeed,
                        assignmentsByScheduleNeedId.getOrDefault(scheduleNeed.getId(), List.of()),
                        null
                ))
                .toList();
    }

    public List<ScheduleNeedResponse> findMyServiceAgenda(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        Long authenticatedMemberId = findAuthenticatedMemberId();

        if (authenticatedMemberId == null) {
            return List.of();
        }

        List<Long> ministryIds = ministryMemberRepository.findMinistryIdsByMemberId(authenticatedMemberId);

        if (ministryIds.isEmpty()) {
            return List.of();
        }

        List<ScheduleNeed> scheduleNeeds = findMyAgendaScheduleNeeds(ministryIds, startDate, endDate);
        Map<Long, List<ScheduleAssignmentResponse>> assignmentsByScheduleNeedId = loadAssignmentsByScheduleNeedId(scheduleNeeds);

        return scheduleNeeds.stream()
                .map(scheduleNeed -> toResponse(
                        scheduleNeed,
                        assignmentsByScheduleNeedId.getOrDefault(scheduleNeed.getId(), List.of()),
                        authenticatedMemberId
                ))
                .toList();
    }

    private List<ScheduleNeed> findMyAgendaScheduleNeeds(List<Long> ministryIds, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return scheduleNeedRepository.findApprovedDetailedByMinistryIdsAndDateBetween(
                    ministryIds,
                    startDate,
                    endDate,
                    ReservationStatus.APPROVED
            );
        }

        if (startDate != null) {
            return scheduleNeedRepository.findApprovedDetailedByMinistryIdsAndDateGreaterThanEqual(
                    ministryIds,
                    startDate,
                    ReservationStatus.APPROVED
            );
        }

        if (endDate != null) {
            return scheduleNeedRepository.findApprovedDetailedByMinistryIdsAndDateLessThanEqual(
                    ministryIds,
                    endDate,
                    ReservationStatus.APPROVED
            );
        }

        return scheduleNeedRepository.findApprovedDetailedByMinistryIds(
                ministryIds,
                ReservationStatus.APPROVED
        );
    }

    private ScheduleNeed createScheduleNeed(Reservation reservation, Ministry ministry) {
        return ScheduleNeed.builder()
                .reservation(reservation)
                .ministry(ministry)
                .date(reservation.getReservationDate())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .status(ScheduleNeedStatus.PENDING)
                .build();
    }

    private List<ScheduleNeed> findVisibleScheduleNeeds(Long reservationId, ScheduleNeedStatus status, LocalDate date) {
        if (isAdmin()) {
            return scheduleNeedRepository.findApprovedDetailedByFilters(
                    reservationId,
                    status,
                    date,
                    ReservationStatus.APPROVED
            );
        }

        Long authenticatedMemberId = findAuthenticatedMemberId();

        if (authenticatedMemberId == null) {
            return List.of();
        }

        List<Long> leaderMinistryIds = ministryMemberRepository.findLeaderMinistryIdsByMemberId(authenticatedMemberId);

        if (leaderMinistryIds.isEmpty()) {
            return List.of();
        }

        return scheduleNeedRepository.findApprovedDetailedByFiltersAndMinistryIds(
                leaderMinistryIds,
                reservationId,
                status,
                date,
                ReservationStatus.APPROVED
        );
    }

    private Map<Long, List<ScheduleAssignmentResponse>> loadAssignmentsByScheduleNeedId(List<ScheduleNeed> scheduleNeeds) {
        if (scheduleNeeds.isEmpty()) {
            return Map.of();
        }

        return scheduleAssignmentService.findAllByScheduleNeedIds(
                        scheduleNeeds.stream().map(ScheduleNeed::getId).toList()
                )
                .stream()
                .collect(Collectors.groupingBy(
                        ScheduleAssignmentResponse::scheduleNeedId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private void validateCanViewScheduleNeed(ScheduleNeed scheduleNeed) {
        if (isAdmin()) {
            return;
        }

        Long authenticatedMemberId = findAuthenticatedMemberId();

        boolean canView = authenticatedMemberId != null
                && ministryMemberRepository.existsByMinistryIdAndMemberId(
                        scheduleNeed.getMinistry().getId(),
                        authenticatedMemberId
                );

        if (!canView) {
            throw new BusinessException("Necessidade de escala nao encontrada ou usuario sem permissao");
        }
    }

    private User findAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BusinessException("Usuario autenticado nao encontrado");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException("Usuario autenticado nao encontrado"));
    }

    private Long findAuthenticatedMemberId() {
        User authenticatedUser = findAuthenticatedUser();

        if (authenticatedUser.getMember() == null || authenticatedUser.getMember().getId() == null) {
            return null;
        }

        return authenticatedUser.getMember().getId();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException("Data final deve ser maior ou igual a data inicial");
        }
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private ScheduleNeedResponse toResponse(
            ScheduleNeed scheduleNeed,
            List<ScheduleAssignmentResponse> assignments,
            Long authenticatedMemberId
    ) {
        boolean authenticatedMemberAssigned = authenticatedMemberId != null
                && assignments.stream().anyMatch(assignment -> authenticatedMemberId.equals(assignment.memberId()));

        List<ScheduleNeedAssignedMemberResponse> assignedMembers = assignments.stream()
                .map(this::toAssignedMemberResponse)
                .toList();

        return new ScheduleNeedResponse(
                scheduleNeed.getId(),
                scheduleNeed.getReservation().getId(),
                scheduleNeed.getReservation().getRoom().getId(),
                scheduleNeed.getReservation().getRoom().getName(),
                scheduleNeed.getReservation().getDescription(),
                scheduleNeed.getMinistry().getId(),
                scheduleNeed.getMinistry().getName(),
                scheduleNeed.getDate(),
                scheduleNeed.getStartTime(),
                scheduleNeed.getEndTime(),
                scheduleNeed.getStatus(),
                scheduleNeed.getReservation().getStatus(),
                authenticatedMemberAssigned,
                authenticatedMemberAssigned,
                assignedMembers,
                assignments
        );
    }

    private ScheduleNeedAssignedMemberResponse toAssignedMemberResponse(ScheduleAssignmentResponse assignment) {
        return new ScheduleNeedAssignedMemberResponse(
                assignment.id(),
                assignment.memberId(),
                assignment.memberName(),
                assignment.memberCpf(),
                assignment.assignedAt(),
                assignment.assignedByUserId(),
                assignment.assignedByUsername(),
                assignment.assignedByName()
        );
    }
}
