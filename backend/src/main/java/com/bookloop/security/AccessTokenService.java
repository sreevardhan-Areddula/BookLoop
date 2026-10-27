package com.bookloop.security;

import com.bookloop.account.Account;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class AccessTokenService {

    public static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(15);
    private final JwtEncoder jwtEncoder;

    public AccessTokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    /** Creates an HS256 JWT scoped to member actions or city-profile completion. */
    public String issue(Account account) {
        Instant now = Instant.now();
        String scope = account.getCity() == null || account.getCity().isBlank()
                ? "profile:complete"
                : "member";
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("bookloop")
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TOKEN_LIFETIME))
                .subject(account.getId().toString())
                .claim("email", account.getEmail())
                .claim("display_name", account.getDisplayName())
                .claim("scope", scope)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
