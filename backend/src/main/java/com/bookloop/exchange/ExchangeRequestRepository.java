package com.bookloop.exchange;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select exchange from ExchangeRequest exchange where exchange.id = :id")
    Optional<ExchangeRequest> findByIdForUpdate(@Param("id") UUID id);

    @Query("select exchange from ExchangeRequest exchange where exchange.status = :status "
            + "and (exchange.requestedListing.id in :listingIds or exchange.offeredListing.id in :listingIds)")
    List<ExchangeRequest> findConflicts(@Param("status") ExchangeStatus status,
                                        @Param("listingIds") Collection<UUID> listingIds);

    List<ExchangeRequest> findAllByRequester_IdOrOwner_IdOrderByUpdatedAtDesc(UUID requesterId, UUID ownerId);

    List<ExchangeRequest> findAllByStatusAndExpiresAtBefore(ExchangeStatus status, Instant now);

    long countByOwner_IdAndOwnerViewedAtIsNullAndStatus(UUID ownerId, ExchangeStatus status);

    boolean existsByRequester_IdAndRequestedListing_IdAndOfferedListing_IdAndStatusIn(
            UUID requesterId, UUID requestedListingId, UUID offeredListingId, Collection<ExchangeStatus> statuses);
}
