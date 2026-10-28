package com.bookloop.exchange.dto;


import java.time.Instant;

/** Presence details for the other member in an authorized exchange conversation. */
public record PresenceResponse(String displayName, boolean online, Instant lastActiveAt) {
}
