package com.bookloop.account.repository;



import com.bookloop.account.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findFirstByAccount_IdOrderByCreatedAtDesc(UUID accountId);

    void deleteByAccount_Id(UUID accountId);
}
