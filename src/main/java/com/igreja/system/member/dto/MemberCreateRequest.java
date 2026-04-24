package com.igreja.system.member.dto;

import java.time.LocalDate;

public record MemberCreateRequest(
        String fullName,
        String cpf,
        LocalDate birthDate,
        String email,
        String phone,
        LocalDate membershipStartDate,
        String description,
        Boolean active
) {
}