package com.bookloop.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByEmail(String email);

    boolean existsByPendingEmailAndIdNot(String pendingEmail, UUID id);

    Optional<Account> findByEmail(String email);
}
