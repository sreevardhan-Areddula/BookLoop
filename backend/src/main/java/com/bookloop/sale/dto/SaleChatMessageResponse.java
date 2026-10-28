package com.bookloop.sale.dto;



import com.bookloop.sale.entity.SaleChatMessage;
import com.bookloop.sale.entity.SaleSharedBillDocument;
import java.time.Instant;
import java.util.UUID;

/** Conversation message fields visible to the buyer and seller. */
public record SaleChatMessageResponse(UUID id, UUID senderId, String senderName, String text,
                                      Instant createdAt, Instant readAt,
                                      UUID sharedBillId, String sharedBillFilename) {
    public static SaleChatMessageResponse from(SaleChatMessage message) {
        SaleSharedBillDocument bill = message.getSharedBill();
        return new SaleChatMessageResponse(message.getId(), message.getSender().getId(),
                message.getSender().getDisplayName(), message.getText(), message.getCreatedAt(),
                message.getReadAt(), bill == null ? null : bill.getId(),
                bill == null ? null : bill.getOriginalFilename());
    }
}
