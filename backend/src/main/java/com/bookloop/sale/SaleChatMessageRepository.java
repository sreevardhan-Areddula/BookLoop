package com.bookloop.sale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaleChatMessageRepository extends JpaRepository<SaleChatMessage, UUID> {
    List<SaleChatMessage> findAllByInterestRequest_IdOrderByCreatedAtAsc(UUID interestRequestId);
    List<SaleChatMessage> findAllByInterestRequest_IdAndCreatedAtAfterOrderByCreatedAtAsc(
            UUID interestRequestId, Instant after);
    long countByRecipient_IdAndReadAtIsNull(UUID recipientId);
    Optional<SaleChatMessage> findByIdAndInterestRequest_Id(UUID id, UUID interestRequestId);

    @Modifying
    @Query("update SaleChatMessage message set message.readAt = :readAt "
            + "where message.interestRequest.id = :interestRequestId "
            + "and message.recipient.id = :recipientId and message.readAt is null")
    int markConversationRead(@Param("interestRequestId") UUID interestRequestId,
                             @Param("recipientId") UUID recipientId,
                             @Param("readAt") Instant readAt);
}
