package com.bookloop.sale;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SaleBillDocumentRepository extends JpaRepository<SaleBillDocument, UUID> {
    Optional<SaleBillDocument> findByIdAndOwner_Id(UUID id, UUID ownerId);
}
