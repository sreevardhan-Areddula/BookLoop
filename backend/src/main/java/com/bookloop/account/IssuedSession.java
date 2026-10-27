package com.bookloop.account;

/** Internal result containing the raw refresh token only long enough to set its cookie. */
public record IssuedSession(String accessToken, String refreshToken, LoginResponse.MemberSummary member) {

    LoginResponse loginResponse() {
        return new LoginResponse(accessToken, 900, member);
    }
}
