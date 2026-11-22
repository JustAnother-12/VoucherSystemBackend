package com.dacn.VoucherSystem.service.sender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GmailApiClient {

    private static final Pattern ACCESS_TOKEN_PATTERN =
            Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");

    private final boolean enabled;
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final HttpClient httpClient;

    private String accessToken;
    private Instant accessTokenExpiresAt = Instant.EPOCH;

    public GmailApiClient(
            @Value("${app.mail.gmail.enabled:false}") boolean enabled,
            @Value("${app.mail.gmail.client-id:}") String clientId,
            @Value("${app.mail.gmail.client-secret:}") String clientSecret,
            @Value("${app.mail.gmail.refresh-token:}") String refreshToken
    ) {
        this.enabled = enabled;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void send(String from, String to, String subject, String text) throws Exception {
        if (!enabled) {
            throw new IllegalStateException("Gmail API is not enabled");
        }
        if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank()) {
            throw new IllegalStateException("Gmail OAuth 2.0 credentials are not configured");
        }

        String mime = createMimeMessage(from, to, subject, text);
        String raw = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mime.getBytes(StandardCharsets.UTF_8));
        String body = "{\"raw\":\"" + raw + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + getAccessToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Gmail API returned HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    private synchronized String getAccessToken() throws Exception {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpiresAt)) {
            return accessToken;
        }

        String form = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&refresh_token=" + encode(refreshToken)
                + "&grant_type=refresh_token";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Gmail OAuth token refresh returned HTTP " + response.statusCode()
                            + ": " + response.body());
        }

        Matcher matcher = ACCESS_TOKEN_PATTERN.matcher(response.body());
        if (!matcher.find()) {
            throw new IllegalStateException("Gmail OAuth response did not contain an access_token");
        }

        accessToken = matcher.group(1);
        accessTokenExpiresAt = Instant.now().plus(Duration.ofMinutes(55));
        return accessToken;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String createMimeMessage(
            String from, String to, String subject, String text
    ) {
        String subjectHeader = "=?UTF-8?B?"
                + Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8))
                + "?=";
        return "From: " + from + "\r\n"
                + "To: " + to + "\r\n"
                + "Subject: " + subjectHeader + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: 8bit\r\n\r\n"
                + text;
    }
}
