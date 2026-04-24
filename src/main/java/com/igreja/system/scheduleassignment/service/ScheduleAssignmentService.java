package com.igreja.system.scheduleassignment.service;

import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.member.entity.Member;
import com.igreja.system.member.repository.MemberRepository;
import com.igreja.system.ministry.repository.MinistryMemberRepository;
import com.igreja.system.reservation.entity.ReservationStatus;
import com.igreja.system.scheduleassignment.dto.ScheduleAssignmentCreateRequest;
import com.igreja.system.scheduleassignment.dto.ScheduleAssignmentResponse;
import com.igreja.system.scheduleassignment.entity.ScheduleAssignment;
import com.igreja.system.scheduleassignment.repository.ScheduleAssignmentRepository;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleAssignmentService {

    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final ScheduleNeedRepository scheduleNeedRepository;
    private final MemberRepository memberRepository;
    private final MinistryMemberRepository ministryMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public ScheduleAssignmentResponse create(Long scheduleNeedId, Long memberId) {
        return createMany(scheduleNeedId, new ScheduleAssignmentCreateRequest(List.of(memberId))).getFirst();
    }

    @Transactional
    public List<ScheduleAssignmentResponse> createMany(Long scheduleNeedId, ScheduleAssignmentCreateRequest request) {
        ScheduleNeed scheduleNeed = findValidScheduleNeedById(scheduleNeedId);
        User authenticatedUser = findAuthenticatedUser();
        validateCanManageScheduleNeed(scheduleNeed, authenticatedUser);

        Set<Long> memberIds = normalizeMemberIds(request);

        List<ScheduleAssignmentResponse> createdAssignments = memberIds.stream()
                .map(memberId -> createAssignment(scheduleNeed, memberId, authenticatedUser))
                .map(this::toResponse)
                .toList();

        updateScheduleNeedStatus(scheduleNeed);

        return createdAssignments;
    }

    @Transactional
    public ScheduleAssignmentResponse delete(Long scheduleNeedId, Long memberId) {
        ScheduleNeed scheduleNeed = findValidScheduleNeedById(scheduleNeedId);
        User authenticatedUser = findAuthenticatedUser();
        validateCanManageScheduleNeed(scheduleNeed, authenticatedUser);
        findMemberById(memberId);

        ScheduleAssignment scheduleAssignment = scheduleAssignmentRepository.findDetailedByScheduleNeedIdAndMemberId(scheduleNeedId, memberId)
                .orElseThrow(() -> new BusinessException("Escala nao encontrada"));

        ScheduleAssignmentResponse response = toResponse(scheduleAssignment);

        scheduleAssignmentRepository.delete(scheduleAssignment);
        updateScheduleNeedStatus(scheduleNeed);

        return response;
    }

    public List<ScheduleAssignmentResponse> findByMemberId(Long memberId, ScheduleNeedStatus status, LocalDate date) {
        findMemberById(memberId);
        validateCanViewMemberAssignments(memberId);

        return scheduleAssignmentRepository.findDetailedByMemberIdAndFilters(
                        memberId,
                        status,
                        date,
                        ReservationStatus.APPROVED
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ScheduleAssignmentResponse> findMyServiceScales(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        Long memberId = findAuthenticatedMemberId();

        if (memberId == null) {
            return List.of();
        }

        return findAssignmentsByMemberAndDateRange(memberId, startDate, endDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ScheduleAssignmentResponse> findByScheduleNeedId(Long scheduleNeedId) {
        ScheduleNeed scheduleNeed = findValidScheduleNeedById(scheduleNeedId);
        validateCanViewScheduleNeedAssignments(scheduleNeed);

        return scheduleAssignmentRepository.findAllDetailedByScheduleNeedId(scheduleNeedId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ScheduleAssignmentResponse> findAllByScheduleNeedIds(List<Long> scheduleNeedIds) {
        if (scheduleNeedIds == null || scheduleNeedIds.isEmpty()) {
            return List.of();
        }

        return scheduleAssignmentRepository.findAllDetailedByScheduleNeedIdIn(scheduleNeedIds)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ScheduleNeed findValidScheduleNeedById(Long id) {
        return scheduleNeedRepository.findApprovedDetailedById(id, ReservationStatus.APPROVED)
                .orElseThrow(() -> new BusinessException("Necessidade de escala nao encontrada ou vinculada a uma reserva nao aprovada"));
    }

    private Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Membro nao encontrado"));
    }

    private Set<Long> normalizeMemberIds(ScheduleAssignmentCreateRequest request) {
        if (request == null || request.memberIds() == null || request.memberIds().isEmpty()) {
            throw new BusinessException("Informe pelo menos um membro para montar a escala");
        }

        Set<Long> memberIds = new LinkedHashSet<>();

        for (Long memberId : request.memberIds()) {
            if (memberId == null) {
                throw new BusinessException("Lista de membros escalados nao aceita valores nulos");
            }

            memberIds.add(memberId);
        }

        return memberIds;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException("Data final deve ser maior ou igual a data inicial");
        }
    }

    private List<ScheduleAssignment> findAssignmentsByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return scheduleAssignmentRepository.findDetailedByMemberIdAndDateBetween(
                    memberId,
                    startDate,
                    endDate,
                    ReservationStatus.APPROVED
            );
        }

        if (startDate != null) {
            return scheduleAssignmentRepository.findDetailedByMemberIdAndDateGreaterThanEqual(
                    memberId,
                    startDate,
                    ReservationStatus.APPROVED
            );
        }

        if (endDate != null) {
            return scheduleAssignmentRepository.findDetailedByMemberIdAndDateLessThanEqual(
                    memberId,
                    endDate,
                    ReservationStatus.APPROVED
            );
        }

        return scheduleAssignmentRepository.findDetailedByMemberId(
                memberId,
                ReservationStatus.APPROVED
        );
    }

    private ScheduleAssignment createAssignment(ScheduleNeed scheduleNeed, Long memberId, User authenticatedUser) {
        Member member = findMemberById(memberId);

        boolean memberBelongsToMinistry = ministryMemberRepository.existsByMinistryIdAndMemberId(
                scheduleNeed.getMinistry().getId(),
                memberId
        );

        if (!memberBelongsToMinistry) {
            throw new BusinessException("Membro nao pertence ao ministerio da necessidade");
        }

        if (scheduleAssignmentRepository.existsByScheduleNeedIdAndMemberId(scheduleNeed.getId(), memberId)) {
            throw new BusinessException("Membro ja escalado para esta necessidade");
        }

        return scheduleAssignmentRepository.save(ScheduleAssignment.builder()
                .scheduleNeed(scheduleNeed)
                .member(member)
                .assignedByUser(authenticatedUser)
                .build());
    }

    private void updateScheduleNeedStatus(ScheduleNeed scheduleNeed) {
        ScheduleNeedStatus newStatus = scheduleAssignmentRepository.countByScheduleNeedId(scheduleNeed.getId()) > 0
                ? ScheduleNeedStatus.FILLED
                : ScheduleNeedStatus.PENDING;

        scheduleNeed.setStatus(newStatus);
        scheduleNeedRepository.save(scheduleNeed);
    }

    private void validateCanManageScheduleNeed(ScheduleNeed scheduleNeed, User authenticatedUser) {
        if (isAdmin()) {
            return;
        }

        Long authenticatedMemberId = authenticatedUser.getMember() != null
                ? authenticatedUser.getMember().getId()
                : null;

        if (authenticatedMemberId == null) {
            throw new BusinessException("Usuario nao possui permissao para montar escala deste ministerio");
        }

        boolean isLeaderOfMinistry = ministryMemberRepository.existsByMinistryIdAndMemberIdAndLeaderTrue(
                scheduleNeed.getMinistry().getId(),
                authenticatedMemberId
        );

        if (!isLeaderOfMinistry) {
            throw new BusinessException("Usuario nao possui permissao para montar escala deste ministerio");
        }
    }

    private void validateCanViewMemberAssignments(Long memberId) {
        if (isAdmin()) {
            return;
        }

        Long authenticatedMemberId = findAuthenticatedMemberId();

        if (authenticatedMemberId == null || !authenticatedMemberId.equals(memberId)) {
            throw new BusinessException("Usuario nao possui permissao para visualizar essas escalas");
        }
    }

    private void validateCanViewScheduleNeedAssignments(ScheduleNeed scheduleNeed) {
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
            throw new BusinessException("Usuario nao possui permissao para visualizar a escala deste ministerio");
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

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private ScheduleAssignmentResponse toResponse(ScheduleAssignment scheduleAssignment) {
        User assignedByUser = scheduleAssignment.getAssignedByUser();

        return new ScheduleAssignmentResponse(
                scheduleAssignment.getId(),
                scheduleAssignment.getScheduleNeed().getId(),
                scheduleAssignment.getScheduleNeed().getReservation().getId(),
                scheduleAssignment.getScheduleNeed().getReservation().getRoom().getId(),
                scheduleAssignment.getScheduleNeed().getReservation().getRoom().getName(),
                scheduleAssignment.getScheduleNeed().getMinistry().getId(),
                scheduleAssignment.getScheduleNeed().getMinistry().getName(),
                scheduleAssignment.getMember().getId(),
                scheduleAssignment.getMember().getFullName(),
                scheduleAssignment.getMember().getCpf(),
                scheduleAssignment.getScheduleNeed().getDate(),
                scheduleAssignment.getScheduleNeed().getStartTime(),
                scheduleAssignment.getScheduleNeed().getEndTime(),
                scheduleAssignment.getScheduleNeed().getReservation().getDescription(),
                scheduleAssignment.getScheduleNeed().getStatus(),
                scheduleAssignment.getScheduleNeed().getReservation().getStatus(),
                scheduleAssignment.getAssignedAt(),
                assignedByUser != null ? assignedByUser.getId() : null,
                assignedByUser != null ? assignedByUser.getUsername() : null,
                resolveAssignedByName(assignedByUser)
        );
    }

    private String resolveAssignedByName(User assignedByUser) {
        if (assignedByUser == null) {
            return null;
        }

        if (assignedByUser.getMember() != null
                && assignedByUser.getMember().getFullName() != null
                && !assignedByUser.getMember().getFullName().isBlank()) {
            return assignedByUser.getMember().getFullName();
        }

        return assignedByUser.getUsername();
    }
}
