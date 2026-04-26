package com.igreja.system.ministry.service;

import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.member.entity.Member;
import com.igreja.system.member.repository.MemberRepository;
import com.igreja.system.ministry.dto.MinistryActiveUpdateRequest;
import com.igreja.system.ministry.dto.MinistryCreateRequest;
import com.igreja.system.ministry.dto.MinistryMemberLeaderUpdateRequest;
import com.igreja.system.ministry.dto.MinistryMemberResponse;
import com.igreja.system.ministry.dto.MinistryResponse;
import com.igreja.system.ministry.dto.MinistryUpdateRequest;
import com.igreja.system.ministry.entity.Ministry;
import com.igreja.system.ministry.entity.MinistryMember;
import com.igreja.system.ministry.repository.MinistryMemberRepository;
import com.igreja.system.ministry.repository.MinistryRepository;
import com.igreja.system.user.entity.User;
import com.igreja.system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MinistryService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_LEADER = "ROLE_LEADER";

    private final MinistryRepository ministryRepository;
    private final MinistryMemberRepository ministryMemberRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    @Transactional
    public MinistryResponse create(MinistryCreateRequest request) {
        validateAdminAccess();
        validateRequiredName(request.name());
        validateName(request.name());

        Ministry ministry = Ministry.builder()
                .name(request.name())
                .description(request.description())
                .active(request.active() != null ? request.active() : true)
                .build();

        Ministry savedMinistry = ministryRepository.save(ministry);

        return toResponse(savedMinistry);
    }

    public List<MinistryResponse> findAll() {
        if (isAdmin()) {
            return ministryRepository.findAll()
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        validateLeaderAccess();

        Long authenticatedMemberId = findAuthenticatedMemberId();

        return ministryRepository.findAllById(ministryMemberRepository.findLeaderMinistryIdsByMemberId(authenticatedMemberId))
                .stream()
                .sorted(Comparator.comparing(Ministry::getName).thenComparing(Ministry::getId))
                .map(this::toResponse)
                .toList();
    }

    public MinistryResponse findById(Long id) {
        Ministry ministry = findMinistryById(id);
        validateCanViewMinistry(ministry);

        return toResponse(ministry);
    }

    public List<MinistryMemberResponse> findMembersByMinistryId(Long id) {
        Ministry ministry = findMinistryById(id);
        validateCanViewMinistry(ministry);

        return ministryMemberRepository.findAllByMinistryId(id)
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    public List<MinistryMemberResponse> findLeadersByMinistryId(Long id) {
        Ministry ministry = findMinistryById(id);
        validateCanViewMinistry(ministry);

        return ministryMemberRepository.findAllLeadersByMinistryId(id)
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public MinistryResponse update(Long id, MinistryUpdateRequest request) {
        Ministry ministry = findMinistryById(id);
        validateCanUpdateMinistry(ministry, request);

        validateRequiredName(request.name());
        validateName(request.name(), id);

        ministry.setName(request.name());
        ministry.setDescription(request.description());

        if (isAdmin()) {
            ministry.setActive(request.active() != null ? request.active() : ministry.getActive());
        }

        Ministry updatedMinistry = ministryRepository.save(ministry);

        return toResponse(updatedMinistry);
    }

    @Transactional
    public MinistryResponse updateActive(Long id, MinistryActiveUpdateRequest request) {
        validateAdminAccess();
        Ministry ministry = findMinistryById(id);

        if (request.active() == null) {
            throw new BusinessException("Active e obrigatorio");
        }

        ministry.setActive(request.active());

        Ministry updatedMinistry = ministryRepository.save(ministry);

        return toResponse(updatedMinistry);
    }

    @Transactional
    public MinistryResponse addMember(Long id, Long memberId) {
        validateAdminAccess();
        Ministry ministry = findMinistryById(id);
        Member member = findMemberById(memberId);

        if (!ministryMemberRepository.existsByMinistryIdAndMemberId(id, memberId)) {
            ministryMemberRepository.save(
                    MinistryMember.builder()
                            .ministry(ministry)
                            .member(member)
                            .leader(false)
                            .build()
            );
        }

        return toResponse(ministry);
    }

    @Transactional
    public MinistryResponse removeMember(Long id, Long memberId) {
        validateAdminAccess();
        Ministry ministry = findMinistryById(id);
        findMemberById(memberId);

        MinistryMember ministryMember = ministryMemberRepository.findByMinistryIdAndMemberId(id, memberId)
                .orElseThrow(() -> new BusinessException("Membro precisa estar vinculado ao ministerio"));

        validateLastLeaderRemoval(ministryMember);

        ministryMemberRepository.delete(ministryMember);

        return toResponse(ministry);
    }

    @Transactional
    public MinistryMemberResponse updateLeader(Long ministryId, Long memberId, MinistryMemberLeaderUpdateRequest request) {
        validateLeaderRequired(request.leader());
        validateAdminAccess();
        findMinistryById(ministryId);
        findMemberById(memberId);

        MinistryMember ministryMember = ministryMemberRepository.findByMinistryIdAndMemberId(ministryId, memberId)
                .orElseThrow(() -> new BusinessException("Membro precisa estar vinculado ao ministerio"));

        validateLastLeaderUnset(ministryMember, request.leader());

        ministryMember.setLeader(request.leader());

        MinistryMember updatedMinistryMember = ministryMemberRepository.save(ministryMember);

        return toMemberResponse(updatedMinistryMember);
    }

    private void validateCanViewMinistry(Ministry ministry) {
        if (isAdmin()) {
            return;
        }

        validateLeaderAccess();

        if (!isLeaderOfMinistry(ministry.getId())) {
            throw new BusinessException("Usuario nao possui permissao para visualizar este ministerio");
        }
    }

    private void validateCanUpdateMinistry(Ministry ministry, MinistryUpdateRequest request) {
        if (isAdmin()) {
            return;
        }

        validateLeaderAccess();

        if (!isLeaderOfMinistry(ministry.getId())) {
            throw new BusinessException("Usuario nao possui permissao para alterar este ministerio");
        }

        if (request.active() != null && !request.active().equals(ministry.getActive())) {
            throw new BusinessException("Usuario nao possui permissao para alterar a situacao do ministerio");
        }
    }

    private void validateLeaderAccess() {
        if (!hasAuthority(ROLE_LEADER)) {
            throw new BusinessException("Usuario nao possui permissao para visualizar ministerios");
        }
    }

    private void validateAdminAccess() {
        if (!isAdmin()) {
            throw new BusinessException("Apenas administradores podem alterar dados de ministerios");
        }
    }

    private boolean isLeaderOfMinistry(Long ministryId) {
        Long authenticatedMemberId = findAuthenticatedMemberId();

        return authenticatedMemberId != null
                && ministryMemberRepository.existsByMinistryIdAndMemberIdAndLeaderTrue(ministryId, authenticatedMemberId);
    }

    private Long findAuthenticatedMemberId() {
        User authenticatedUser = findAuthenticatedUser();

        if (authenticatedUser.getMember() == null || authenticatedUser.getMember().getId() == null) {
            return null;
        }

        return authenticatedUser.getMember().getId();
    }

    private boolean isAdmin() {
        return hasAuthority(ROLE_ADMIN);
    }

    private User findAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BusinessException("Usuario autenticado nao encontrado");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException("Usuario autenticado nao encontrado"));
    }

    private boolean hasAuthority(String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities()
                .stream()
                .anyMatch(grantedAuthority -> roleName.equals(grantedAuthority.getAuthority()));
    }

    private void validateRequiredName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Nome e obrigatorio");
        }
    }

    private void validateName(String name) {
        if (ministryRepository.findByName(name).isPresent()) {
            throw new BusinessException("Nome ja cadastrado");
        }
    }

    private void validateName(String name, Long ministryId) {
        ministryRepository.findByName(name)
                .filter(existingMinistry -> !existingMinistry.getId().equals(ministryId))
                .ifPresent(existingMinistry -> {
                    throw new BusinessException("Nome ja cadastrado");
                });
    }

    private void validateLeaderRequired(Boolean leader) {
        if (leader == null) {
            throw new BusinessException("Leader e obrigatorio");
        }
    }

    private void validateLastLeaderRemoval(MinistryMember ministryMember) {
        if (Boolean.TRUE.equals(ministryMember.getLeader())
                && ministryMemberRepository.countByMinistryIdAndLeaderTrue(ministryMember.getMinistry().getId()) == 1) {
            throw new BusinessException("Nao e permitido remover o ultimo lider do ministerio");
        }
    }

    private void validateLastLeaderUnset(MinistryMember ministryMember, Boolean leader) {
        if (Boolean.FALSE.equals(leader)
                && Boolean.TRUE.equals(ministryMember.getLeader())
                && ministryMemberRepository.countByMinistryIdAndLeaderTrue(ministryMember.getMinistry().getId()) == 1) {
            throw new BusinessException("Nao e permitido remover o ultimo lider do ministerio");
        }
    }

    private Ministry findMinistryById(Long id) {
        return ministryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Ministerio nao encontrado"));
    }

    private Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Membro nao encontrado"));
    }

    private MinistryResponse toResponse(Ministry ministry) {
        return new MinistryResponse(
                ministry.getId(),
                ministry.getName(),
                ministry.getDescription(),
                ministry.getActive()
        );
    }

    private MinistryMemberResponse toMemberResponse(MinistryMember ministryMember) {
        Member member = ministryMember.getMember();

        return new MinistryMemberResponse(
                member.getId(),
                member.getFullName(),
                member.getCpf(),
                member.getBirthDate(),
                member.getEmail(),
                member.getPhone(),
                member.getMembershipStartDate(),
                member.getDescription(),
                member.getActive(),
                ministryMember.getLeader()
        );
    }
}
