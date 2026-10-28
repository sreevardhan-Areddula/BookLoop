package com.bookloop.account.entity;



import com.bookloop.account.dto.LoginResponse;
/** Internal result containing the raw refresh token only long enough to set its cookie. */
public record IssuedSession(String accessToken, String refreshToken, LoginResponse.MemberSummary member) {

    public LoginResponse loginResponse() {
        return new LoginResponse(accessToken, 900, member);
    }
}
