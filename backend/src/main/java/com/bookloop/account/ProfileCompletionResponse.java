package com.bookloop.account;

/** Member details and upgraded access token returned after city completion. */
public record ProfileCompletionResponse(String accessToken, LoginResponse.MemberSummary member) {
}
