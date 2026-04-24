package com.igreja.system.ministry.dto;

import java.time.LocalDate;

public record MinistryMemberResponse(
        Long id,
        String fullName,
        String cpf,
        LocalDate birthDate,
        String email,
        String phone,
        LocalDate membershipStartDate,
        String description,
        Boolean active,
        Boolean leader
) {
}
