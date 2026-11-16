package com.taskflow.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(15);
    private static final String SECRET = base64("unit-test-secret-unit-test-secret-1234");
    private static final String OTHER_SECRET = base64("another-secret-another-secret-12345678");

    private final UUID userId = UUID.randomUUID();

    @Test
    void generatedTokenIsParsedBackToTheSameUser() {
        JwtService jwtService = serviceAt(NOW, SECRET, "taskflow");

        String token = jwtService.generateAccessToken(userId, "ada@example.com");

        assertThat(jwtService.parseAccessToken(token))
                .contains(new AuthenticatedUser(userId, "ada@example.com"));
    }

    @Test
    void tokenIsValidUntilJustBeforeItExpires() {
        String token = serviceAt(NOW, SECRET, "taskflow").generateAccessToken(userId, "ada@example.com");

        JwtService justBeforeExpiry = serviceAt(NOW.plus(TTL).minusSeconds(1), SECRET, "taskflow");

        assertThat(justBeforeExpiry.parseAccessToken(token)).isPresent();
    }

    @Test
    void expiredTokenIsRejected() {
        String token = serviceAt(NOW, SECRET, "taskflow").generateAccessToken(userId, "ada@example.com");

        JwtService afterExpiry = serviceAt(NOW.plus(TTL).plusSeconds(1), SECRET, "taskflow");

        assertThat(afterExpiry.parseAccessToken(token)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String forged = serviceAt(NOW, OTHER_SECRET, "taskflow").generateAccessToken(userId, "ada@example.com");

        assertThat(serviceAt(NOW, SECRET, "taskflow").parseAccessToken(forged)).isEmpty();
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        String foreign = serviceAt(NOW, SECRET, "someone-else").generateAccessToken(userId, "ada@example.com");

        assertThat(serviceAt(NOW, SECRET, "taskflow").parseAccessToken(foreign)).isEmpty();
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = serviceAt(NOW, SECRET, "taskflow");
        String token = jwtService.generateAccessToken(userId, "ada@example.com");
        String[] parts = token.split("\\.");

        // Replace the payload with one claiming a different user, keeping the original signature.
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"iss\":\"taskflow\",\"sub\":\"" + UUID.randomUUID() + "\"}").getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThat(jwtService.parseAccessToken(tampered)).isEmpty();
    }

    @Test
    void garbageIsRejectedWithoutThrowing() {
        JwtService jwtService = serviceAt(NOW, SECRET, "taskflow");

        assertThat(jwtService.parseAccessToken("not-a-jwt")).isEmpty();
        assertThat(jwtService.parseAccessToken("")).isEmpty();
        Optional<AuthenticatedUser> fromNull = jwtService.parseAccessToken(null);
        assertThat(fromNull).isEmpty();
    }

    private static JwtService serviceAt(Instant instant, String secret, String issuer) {
        return new JwtService(new JwtProperties(secret, issuer, TTL), Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
