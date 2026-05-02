package com.igreja.system.reservation.service;

import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.ministry.entity.Ministry;
import com.igreja.system.ministry.repository.MinistryMemberRepository;
import com.igreja.system.ministry.repository.MinistryRepository;
import com.igreja.system.reservation.dto.ReservationCalendarSummaryResponse;
import com.igreja.system.reservation.dto.ReservationCreateRequest;
import com.igreja.system.reservation.dto.ReservationCancelRequest;
import com.igreja.system.reservation.dto.ReservationResponse;
import com.igreja.system.reservation.entity.Reservation;
import com.igreja.system.reservation.entity.ReservationStatus;
import com.igreja.system.reservation.repository.ReservationRepository;
import com.igreja.system.room.entity.Room;
import com.igreja.system.room.entity.RoomReservationRule;
import com.igreja.system.room.repository.RoomRepository;
import com.igreja.system.scheduleassignment.repository.ScheduleAssignmentRepository;
import com.igreja.system.scheduleneed.repository.ScheduleNeedRepository;
import com.igreja.system.scheduleneed.service.ScheduleNeedService;
import com.igreja.system.user.entity.User;
import com.igreja.system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_LEADER = "ROLE_LEADER";
    private static final String ROLE_MEMBER = "ROLE_MEMBER";

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MinistryRepository ministryRepository;
    private final MinistryMemberRepository ministryMemberRepository;
    private final UserRepository userRepository;
    private final ScheduleNeedService scheduleNeedService;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final ScheduleNeedRepository scheduleNeedRepository;

    @Transactional
    public ReservationResponse create(ReservationCreateRequest request) {
        validateCanCreateReservation();
        validateRequiredRoom(request.roomId());
        validateRequiredReservationDate(request.reservationDate());
        validateRequiredStartTime(request.startTime());
        validateRequiredEndTime(request.endTime());
        validateRequiredUsingMinistry(request.usingMinistryId());
        validateTimeRange(request.startTime(), request.endTime());
        validateAcceptedMinutes(request.startTime(), "Horario inicial");
        validateAcceptedMinutes(request.endTime(), "Horario final");

        Room room = findRoomById(request.roomId());
        validateRoomReservationRule(room, request.reservationDate(), request.startTime(), request.endTime());
        validateNoConflict(room.getId(), request.reservationDate(), request.startTime(), request.endTime());

        Ministry usingMinistry = findMinistryById(request.usingMinistryId());
        Set<Ministry> scheduleDemandMinistries = resolveScheduleDemandMinistries(request.scheduleDemandMinistryIds());
        User requestedBy = findAuthenticatedUser();

        Reservation reservation = Reservation.builder()
                .room(room)
                .reservationDate(request.reservationDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .description(request.description())
                .cancelReason(null)
                .status(ReservationStatus.PENDING)
                .requestedBy(requestedBy)
                .usingMinistry(usingMinistry)
                .scheduleDemandMinistries(scheduleDemandMinistries)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        scheduleNeedService.createForReservation(savedReservation);

        return toResponse(savedReservation);
    }

    public List<ReservationResponse> findAll(Long roomId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        User authenticatedUser = findAuthenticatedUser();

        return filterVisibleReservations(findReservationsByFilters(roomId, startDate, endDate), authenticatedUser)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ReservationCalendarSummaryResponse> findCalendarSummary(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        User authenticatedUser = findAuthenticatedUser();

        Map<LocalDate, List<Reservation>> reservationsByDate = filterVisibleReservationsForCalendar(
                findReservationsByFilters(null, startDate, endDate),
                authenticatedUser
        ).stream()
                .collect(Collectors.groupingBy(
                        Reservation::getReservationDate,
                        TreeMap::new,
                        Collectors.toList()
                ));

        return reservationsByDate.entrySet()
                .stream()
                .map(entry -> toCalendarSummaryResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<ReservationResponse> findPending() {
        validateCanViewPendingReservations();

        return reservationRepository.findAllByStatusWithRelations(ReservationStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ReservationResponse findById(Long id) {
        Reservation reservation = findReservationById(id);
        validateCanViewReservation(reservation, findAuthenticatedUser());

        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse approve(Long id) {
        validateAdminAccess();
        Reservation reservation = findReservationById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("Reserva so pode ser aprovada quando estiver pendente");
        }

        reservation.setStatus(ReservationStatus.APPROVED);

        Reservation updatedReservation = reservationRepository.save(reservation);
        scheduleNeedService.createForReservation(updatedReservation);

        return toResponse(updatedReservation);
    }

    @Transactional
    public ReservationResponse cancel(Long id, ReservationCancelRequest request) {
        validateAdminAccess();
        Reservation reservation = findReservationById(id);
        validateRequiredCancelReason(request);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("Reserva ja esta cancelada");
        }

        scheduleAssignmentRepository.deleteByReservationId(reservation.getId());
        scheduleNeedRepository.deleteByReservationId(reservation.getId());

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelReason(request.reason().trim());

        Reservation updatedReservation = reservationRepository.save(reservation);

        return toResponse(updatedReservation);
    }

    private void validateRequiredRoom(Long roomId) {
        if (roomId == null) {
            throw new BusinessException("Ambiente e obrigatorio");
        }
    }

    private void validateRequiredReservationDate(LocalDate reservationDate) {
        if (reservationDate == null) {
            throw new BusinessException("Data da reserva e obrigatoria");
        }
    }

    private void validateRequiredStartTime(LocalTime startTime) {
        if (startTime == null) {
            throw new BusinessException("Horario inicial e obrigatorio");
        }
    }

    private void validateRequiredEndTime(LocalTime endTime) {
        if (endTime == null) {
            throw new BusinessException("Horario final e obrigatorio");
        }
    }

    private void validateRequiredUsingMinistry(Long usingMinistryId) {
        if (usingMinistryId == null) {
            throw new BusinessException("Ministerio de uso e obrigatorio");
        }
    }

    private void validateRequiredCancelReason(ReservationCancelRequest request) {
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new BusinessException("Motivo do cancelamento e obrigatorio");
        }
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("Horario final deve ser maior que o horario inicial");
        }
    }

    private void validateAcceptedMinutes(LocalTime time, String fieldLabel) {
        if ((time.getMinute() != 0 && time.getMinute() != 30) || time.getSecond() != 0 || time.getNano() != 0) {
            throw new BusinessException(fieldLabel + " deve usar apenas minutos 00 ou 30");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException("Data final deve ser maior ou igual a data inicial");
        }
    }

    private void validateCanCreateReservation() {
        if (isAdmin() || isLeader()) {
            return;
        }

        throw new BusinessException("Usuario nao possui permissao para criar reservas");
    }

    private void validateAdminAccess() {
        if (!isAdmin()) {
            throw new BusinessException("Usuario nao possui permissao para aprovar ou cancelar reservas");
        }
    }

    private void validateCanViewPendingReservations() {
        if (!isAdmin()) {
            throw new BusinessException("Usuario nao possui permissao para visualizar reservas pendentes");
        }
    }

    private void validateRoomReservationRule(Room room, LocalDate reservationDate, LocalTime startTime, LocalTime endTime) {
        DayOfWeek reservationDayOfWeek = reservationDate.getDayOfWeek();

        RoomReservationRule reservationRule = room.getReservationRules()
                .stream()
                .filter(rule -> reservationDayOfWeek.equals(rule.getDayOfWeek()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Ambiente nao possui configuracao de reserva para " + reservationDayOfWeek));

        if (!Boolean.TRUE.equals(reservationRule.getEnabled())) {
            throw new BusinessException("Ambiente nao permite reservas em " + reservationDayOfWeek);
        }

        if (reservationRule.getStartTime() == null || reservationRule.getEndTime() == null) {
            throw new BusinessException("Ambiente possui regra invalida para " + reservationDayOfWeek);
        }

        if (startTime.isBefore(reservationRule.getStartTime()) || endTime.isAfter(reservationRule.getEndTime())) {
            throw new BusinessException("Reserva fora do horario permitido do ambiente");
        }
    }

    private void validateNoConflict(Long roomId, LocalDate reservationDate, LocalTime startTime, LocalTime endTime) {
        boolean hasConflict = reservationRepository.existsConflictingReservation(
                roomId,
                reservationDate,
                startTime,
                endTime,
                ReservationStatus.CANCELLED
        );

        if (hasConflict) {
            throw new BusinessException("Ja existe reserva para este ambiente no horario informado");
        }
    }

    private Room findRoomById(Long id) {
        return roomRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException("Ambiente nao encontrado"));
    }

    private Reservation findReservationById(Long id) {
        return reservationRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new BusinessException("Reserva nao encontrada"));
    }

    private List<Reservation> filterVisibleReservations(List<Reservation> reservations, User authenticatedUser) {
        if (!isMember(authenticatedUser) || isAdmin(authenticatedUser) || isLeader(authenticatedUser)) {
            return reservations;
        }

        return reservations.stream()
                .filter(this::isApprovedReservation)
                .toList();
    }

    private List<Reservation> filterVisibleReservationsForCalendar(List<Reservation> reservations, User authenticatedUser) {
        if (isAdmin(authenticatedUser)) {
            return reservations;
        }

        if (isLeader(authenticatedUser)) {
            return filterLeaderVisibleReservationsForCalendar(reservations, authenticatedUser);
        }

        if (isMember(authenticatedUser)) {
            return reservations.stream()
                    .filter(this::isApprovedReservation)
                    .toList();
        }

        return List.of();
    }

    private List<Reservation> filterLeaderVisibleReservationsForCalendar(List<Reservation> reservations, User authenticatedUser) {
        Set<Long> leaderMinistryIds = findLeaderMinistryIds(authenticatedUser);

        return reservations.stream()
                .filter(reservation -> isRequestedByUser(reservation, authenticatedUser)
                        || isLinkedToAnyMinistry(reservation, leaderMinistryIds))
                .toList();
    }

    private void validateCanViewReservation(Reservation reservation, User authenticatedUser) {
        if (!isMember(authenticatedUser) || isAdmin(authenticatedUser) || isLeader(authenticatedUser)) {
            return;
        }

        if (!isApprovedReservation(reservation)) {
            throw new BusinessException("Reserva nao encontrada ou usuario sem permissao");
        }
    }

    private boolean isApprovedReservation(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.APPROVED;
    }

    private boolean isRequestedByUser(Reservation reservation, User authenticatedUser) {
        return reservation.getRequestedBy() != null
                && reservation.getRequestedBy().getId() != null
                && authenticatedUser.getId() != null
                && authenticatedUser.getId().equals(reservation.getRequestedBy().getId());
    }

    private boolean isLinkedToAnyMinistry(Reservation reservation, Set<Long> ministryIds) {
        if (ministryIds.isEmpty()) {
            return false;
        }

        return streamReservationMinistries(reservation)
                .map(Ministry::getId)
                .filter(Objects::nonNull)
                .anyMatch(ministryIds::contains);
    }

    private User findAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BusinessException("Usuario autenticado nao encontrado");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException("Usuario autenticado nao encontrado"));
    }

    private Ministry findMinistryById(Long id) {
        return ministryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Ministerio nao encontrado"));
    }

    private Set<Long> findLeaderMinistryIds(User authenticatedUser) {
        if (authenticatedUser.getMember() == null || authenticatedUser.getMember().getId() == null) {
            return Set.of();
        }

        return new LinkedHashSet<>(ministryMemberRepository.findLeaderMinistryIdsByMemberId(
                authenticatedUser.getMember().getId()
        ));
    }

    private Set<Ministry> resolveScheduleDemandMinistries(List<Long> scheduleDemandMinistryIds) {
        Set<Ministry> scheduleDemandMinistries = new LinkedHashSet<>();

        if (scheduleDemandMinistryIds == null || scheduleDemandMinistryIds.isEmpty()) {
            return scheduleDemandMinistries;
        }

        for (Long ministryId : scheduleDemandMinistryIds) {
            if (ministryId == null) {
                throw new BusinessException("Lista de ministerios de demanda nao aceita valores nulos");
            }

            scheduleDemandMinistries.add(findMinistryById(ministryId));
        }

        return scheduleDemandMinistries;
    }

    private List<Reservation> findReservationsByFilters(Long roomId, LocalDate startDate, LocalDate endDate) {
        if (roomId != null) {
            if (startDate != null && endDate != null) {
                return reservationRepository.findAllByRoomIdAndReservationDateBetweenWithRelations(roomId, startDate, endDate);
            }

            if (startDate != null) {
                return reservationRepository.findAllByRoomIdAndReservationDateGreaterThanEqualWithRelations(roomId, startDate);
            }

            if (endDate != null) {
                return reservationRepository.findAllByRoomIdAndReservationDateLessThanEqualWithRelations(roomId, endDate);
            }

            return reservationRepository.findAllByRoomIdWithRelations(roomId);
        }

        if (startDate != null && endDate != null) {
            return reservationRepository.findAllByReservationDateBetweenWithRelations(startDate, endDate);
        }

        if (startDate != null) {
            return reservationRepository.findAllByReservationDateGreaterThanEqualWithRelations(startDate);
        }

        if (endDate != null) {
            return reservationRepository.findAllByReservationDateLessThanEqualWithRelations(endDate);
        }

        return reservationRepository.findAllWithRelations();
    }

    private ReservationCalendarSummaryResponse toCalendarSummaryResponse(LocalDate date, List<Reservation> reservations) {
        long approvedCount = reservations.stream()
                .filter(this::isApprovedReservation)
                .count();

        return new ReservationCalendarSummaryResponse(
                date,
                approvedCount,
                reservations.size()
        );
    }

    private boolean isAdmin() {
        return hasAuthority(ROLE_ADMIN);
    }

    private boolean isLeader() {
        return hasAuthority(ROLE_LEADER);
    }

    private boolean hasAuthority(String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(authority -> roleName.equals(authority.getAuthority()));
    }

    private boolean isAdmin(User user) {
        return hasRole(user, ROLE_ADMIN);
    }

    private boolean isLeader(User user) {
        return hasRole(user, ROLE_LEADER);
    }

    private boolean isMember(User user) {
        return hasRole(user, ROLE_MEMBER);
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()));
    }

    private Stream<Ministry> streamReservationMinistries(Reservation reservation) {
        return Stream.concat(
                        Stream.of(reservation.getUsingMinistry()),
                        reservation.getScheduleDemandMinistries().stream()
                )
                .filter(Objects::nonNull);
    }

    private ReservationResponse toResponse(Reservation reservation) {
        List<Long> scheduleDemandMinistryIds = reservation.getScheduleDemandMinistries()
                .stream()
                .map(Ministry::getId)
                .sorted()
                .toList();

        List<String> scheduleDemandMinistryNames = reservation.getScheduleDemandMinistries()
                .stream()
                .map(Ministry::getName)
                .sorted()
                .toList();

        List<Long> ministryIds = streamReservationMinistries(reservation)
                .map(Ministry::getId)
                .distinct()
                .sorted()
                .toList();

        return new ReservationResponse(
                reservation.getId(),
                reservation.getRoom().getId(),
                reservation.getRoom().getName(),
                resolveRequesterName(reservation.getRequestedBy()),
                reservation.getRequestedBy() != null ? reservation.getRequestedBy().getId() : null,
                reservation.getRequestedBy() != null ? reservation.getRequestedBy().getUsername() : null,
                reservation.getUsingMinistry() != null ? reservation.getUsingMinistry().getId() : null,
                reservation.getUsingMinistry() != null ? reservation.getUsingMinistry().getName() : null,
                scheduleDemandMinistryIds,
                scheduleDemandMinistryNames,
                reservation.getCreatedAt(),
                reservation.getReservationDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getDescription(),
                reservation.getCancelReason(),
                reservation.getStatus(),
                ministryIds
        );
    }

    private String resolveRequesterName(User requestedBy) {
        if (requestedBy == null) {
            return null;
        }

        if (requestedBy.getMember() != null && requestedBy.getMember().getFullName() != null && !requestedBy.getMember().getFullName().isBlank()) {
            return requestedBy.getMember().getFullName();
        }

        return requestedBy.getUsername();
    }
}
