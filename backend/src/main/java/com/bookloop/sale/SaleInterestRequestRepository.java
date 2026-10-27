package com.bookloop.sale;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaleInterestRequestRepository extends JpaRepository<SaleInterestRequest, UUID> {
    List<SaleInterestRequest> findAllByRequester_IdOrOwner_IdOrderByUpdatedAtDesc(UUID requesterId, UUID ownerId);
    boolean existsByRequester_IdAndListing_IdAndStatusIn(UUID requesterId, UUID listingId,
                                                          List<SaleInterestStatus> statuses);
    long countByOwner_IdAndOwnerViewedAtIsNullAndStatus(UUID ownerId, SaleInterestStatus status);
    List<SaleInterestRequest> findAllByListing_IdAndStatusAndIdNot(UUID listingId,
                                                                   SaleInterestStatus status, UUID excludedId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from SaleInterestRequest request where request.id = :id")
    Optional<SaleInterestRequest> findByIdForUpdate(@Param("id") UUID id);

}
