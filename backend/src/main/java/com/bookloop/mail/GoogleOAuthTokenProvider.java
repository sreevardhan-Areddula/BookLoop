package com.bookloop.mail;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GoogleOAuthTokenProvider {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;

    public GoogleOAuthTokenProvider(
            ObjectMapper objectMapper,
            @Value("${bookloop.mail.oauth-client-id:}") String clientId,
            @Value("${bookloop.mail.oauth-client-secret:}") String clientSecret,
            @Value("${bookloop.mail.oauth-refresh-token:}") String refreshToken
    ) {
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
    }

    /** Exchanges the configured refresh token for a short-lived Gmail SMTP token. */
    public String accessToken() {
        if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank()) {
            throw new IllegalStateException("Gmail OAuth2 mail credentials are not configured.");
        }

        String form = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "refresh_token", refreshToken,
                "grant_type", "refresh_token"
        ).entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Google OAuth2 token request failed with HTTP "
                        + response.statusCode() + ".");
            }
            JsonNode token = objectMapper.readTree(response.body()).get("access_token");
            if (token == null || token.asText().isBlank()) {
                throw new IllegalStateException("Google OAuth2 response did not contain an access token.");
            }
            return token.asText();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read Google OAuth2 token response.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Google OAuth2 token request was interrupted.", exception);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
