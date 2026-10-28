package com.bookloop.book;

import java.time.Instant;
import java.util.UUID;

/** Message fields displayed to the two members in one Free-claim conversation. */
public record FreeClaimMessageResponse(UUID id, UUID senderId, String senderName,
                                       String text, Instant createdAt, Instant readAt) {
    public static FreeClaimMessageResponse from(FreeClaimMessage message) {
        return new FreeClaimMessageResponse(message.getId(), message.getSender().getId(),
                message.getSender().getDisplayName(), message.getText(), message.getCreatedAt(), message.getReadAt());
    }
}
