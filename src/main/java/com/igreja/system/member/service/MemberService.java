package com.igreja.system.member.service;

import com.igreja.system.common.exception.BusinessException;
import com.igreja.system.member.dto.MemberActiveUpdateRequest;
import com.igreja.system.member.dto.MemberCreateRequest;
import com.igreja.system.member.dto.MemberResponse;
import com.igreja.system.member.dto.MemberUpdateRequest;
import com.igreja.system.member.entity.Member;
import com.igreja.system.member.repository.MemberRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository repository;

    @Transactional
    public MemberResponse create(MemberCreateRequest request) {
        boolean cpfAlreadyExists = repository.findByCpf(request.cpf()).isPresent();

        if (cpfAlreadyExists) {
            throw new BusinessException("CPF já cadastrado");
        }

        Member member = Member.builder()
                .fullName(request.fullName())
                .cpf(request.cpf())
                .birthDate(request.birthDate())
                .email(request.email())
                .phone(request.phone())
                .membershipStartDate(request.membershipStartDate())
                .description(request.description())
                .active(request.active() != null ? request.active() : true)
                .build();

        Member saved = repository.save(member);

        return toResponse(saved);
    }

    public List<MemberResponse> findAll(String fullName, String cpf) {
        return repository.findAll((root, query, criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<>();

                    if (fullName != null && !fullName.isBlank()) {
                        predicates.add(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("fullName")),
                                        "%" + fullName.toLowerCase() + "%"
                                )
                        );
                    }

                    if (cpf != null && !cpf.isBlank()) {
                        predicates.add(criteriaBuilder.equal(root.get("cpf"), cpf));
                    }

                    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                })
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MemberResponse findById(Long id) {
        Member member = findMemberById(id);

        return toResponse(member);
    }

    @Transactional
    public MemberResponse update(Long id, MemberUpdateRequest request) {
        Member member = findMemberById(id);

        validateRequiredFullName(request.fullName());
        validateRequiredCpf(request.cpf());
        validateRequiredBirthDate(request.birthDate());
        validateCpf(request.cpf(), id);

        member.setFullName(request.fullName());
        member.setCpf(request.cpf());
        member.setBirthDate(request.birthDate());
        member.setEmail(request.email());
        member.setPhone(request.phone());
        member.setMembershipStartDate(request.membershipStartDate());
        member.setDescription(request.description());
        member.setActive(request.active() != null ? request.active() : member.getActive());

        Member updatedMember = repository.save(member);

        return toResponse(updatedMember);
    }

    @Transactional
    public MemberResponse updateActive(Long id, MemberActiveUpdateRequest request) {
        Member member = findMemberById(id);

        if (request.active() == null) {
            throw new BusinessException("Active é obrigatório");
        }

        member.setActive(request.active());

        Member updatedMember = repository.save(member);

        return toResponse(updatedMember);
    }

    private void validateRequiredFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("Nome completo é obrigatório");
        }
    }

    private void validateRequiredCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            throw new BusinessException("CPF é obrigatório");
        }
    }

    private void validateRequiredBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new BusinessException("Data de nascimento é obrigatória");
        }
    }

    private void validateCpf(String cpf, Long memberId) {
        repository.findByCpf(cpf)
                .filter(existingMember -> !existingMember.getId().equals(memberId))
                .ifPresent(existingMember -> {
                    throw new BusinessException("CPF já cadastrado");
                });
    }

    private Member findMemberById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("Membro não encontrado"));
    }

    private MemberResponse toResponse(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getFullName(),
                member.getCpf(),
                member.getBirthDate(),
                member.getEmail(),
                member.getPhone(),
                member.getMembershipStartDate(),
                member.getDescription(),
                member.getActive()
        );
    }
}
