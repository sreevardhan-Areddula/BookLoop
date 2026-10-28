package com.bookloop.exchange.dto;



import com.bookloop.exchange.entity.ChatMessage;
import java.time.Instant;
import java.util.UUID;

/** Text message fields needed by the two exchange participants. */
public record ChatMessageResponse(UUID id, UUID senderId, String senderName, String text,
                                 Instant createdAt, Instant readAt) {
    /** Converts a persisted chat message to its display form. */
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getSender().getId(),
                message.getSender().getDisplayName(), message.getText(),
                message.getCreatedAt(), message.getReadAt());
    }
}
