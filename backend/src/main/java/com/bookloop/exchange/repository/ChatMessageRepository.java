package com.bookloop.exchange.repository;



import com.bookloop.exchange.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findAllByExchange_IdOrderByCreatedAtAsc(UUID exchangeId);

    List<ChatMessage> findAllByExchange_IdAndCreatedAtAfterOrderByCreatedAtAsc(UUID exchangeId, Instant after);

    long countByRecipient_IdAndReadAtIsNull(UUID recipientId);

    @Modifying
    @Query("update ChatMessage message set message.readAt = :readAt "
            + "where message.exchange.id = :exchangeId and message.recipient.id = :recipientId "
            + "and message.readAt is null")
    int markConversationRead(@Param("exchangeId") UUID exchangeId,
                             @Param("recipientId") UUID recipientId,
                             @Param("readAt") Instant readAt);
}
