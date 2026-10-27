package com.bookloop.exchange;

import java.time.Instant;

/** Presence details for the other member in an authorized exchange conversation. */
public record PresenceResponse(String displayName, boolean online, Instant lastActiveAt) {
}
