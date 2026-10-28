package com.bookloop.sale.repository;



import com.bookloop.sale.entity.SaleSharedBillDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SaleSharedBillDocumentRepository extends JpaRepository<SaleSharedBillDocument, UUID> {
    Optional<SaleSharedBillDocument> findByIdAndInterestRequest_Id(UUID id, UUID interestRequestId);
}
