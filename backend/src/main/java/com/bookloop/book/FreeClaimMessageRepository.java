package com.bookloop.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FreeClaimMessageRepository extends JpaRepository<FreeClaimMessage, UUID> {
    List<FreeClaimMessage> findAllByClaimRequest_IdOrderByCreatedAtAsc(UUID claimId);
    List<FreeClaimMessage> findAllByClaimRequest_IdAndCreatedAtAfterOrderByCreatedAtAsc(UUID claimId, Instant after);
    long countByRecipient_IdAndReadAtIsNull(UUID recipientId);

    @Modifying
    @Query("update FreeClaimMessage message set message.readAt = :readAt "
            + "where message.claimRequest.id = :claimId and message.recipient.id = :recipientId "
            + "and message.readAt is null")
    int markConversationRead(@Param("claimId") UUID claimId,
                             @Param("recipientId") UUID recipientId,
                             @Param("readAt") Instant readAt);
}
