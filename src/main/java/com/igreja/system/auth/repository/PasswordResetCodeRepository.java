package com.igreja.system.auth.repository;

import com.igreja.system.auth.entity.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    @Modifying
    @Query("""
            update PasswordResetCode prc
            set prc.used = true
            where prc.user.id = :userId
              and prc.used = false
            """)
    void markAllUnusedAsUsedByUserId(@Param("userId") Long userId);

    Optional<PasswordResetCode> findFirstByUserEmailIgnoreCaseAndCodeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email,
            String code,
            LocalDateTime now
    );
}
