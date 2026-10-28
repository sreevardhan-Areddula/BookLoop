package com.bookloop.book.repository;



import com.bookloop.book.entity.FreeClaimRequest;
import com.bookloop.book.entity.FreeClaimStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FreeClaimRequestRepository extends JpaRepository<FreeClaimRequest, UUID> {
    List<FreeClaimRequest> findAllByRequester_IdOrOwner_IdOrderByUpdatedAtDesc(UUID requesterId, UUID ownerId);
    boolean existsByRequester_IdAndListing_IdAndStatusIn(UUID requesterId, UUID listingId, List<FreeClaimStatus> statuses);
    long countByOwner_IdAndOwnerViewedAtIsNullAndStatus(UUID ownerId, FreeClaimStatus status);
    List<FreeClaimRequest> findAllByListing_IdAndStatusAndIdNot(UUID listingId, FreeClaimStatus status, UUID excludedId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select claim from FreeClaimRequest claim where claim.id = :id")
    Optional<FreeClaimRequest> findByIdForUpdate(@Param("id") UUID id);
}
