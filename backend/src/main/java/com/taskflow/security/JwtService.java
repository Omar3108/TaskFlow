package com.taskflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Creates and validates access tokens (signed JWTs, HMAC-SHA256).
 *
 * <p>Refresh tokens are <em>not</em> JWTs: they are opaque random values handled by
 * {@code RefreshTokenService}.
 */
@Service
public class JwtService {

    private static final String EMAIL_CLAIM = "email";

    private final SecretKey signingKey;
    private final JwtParser parser;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        // Fails fast at startup (WeakKeyException) if the key is shorter than 256 bits.
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.parser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    public String generateAccessToken(UUID userId, String email) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(userId.toString())
                .claim(EMAIL_CLAIM, email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates signature, issuer and expiration.
     *
     * @return the authenticated user, or empty if the token is invalid in any way. Callers do
     *         not need to know why it failed: every failure means "not authenticated".
     */
    public Optional<AuthenticatedUser> parseAccessToken(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get(EMAIL_CLAIM, String.class);
            return Optional.of(new AuthenticatedUser(userId, email));
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            // JwtException: bad signature, expired, malformed, wrong issuer.
            // IllegalArgument/NullPointer: blank token, or a subject that is not a UUID.
            return Optional.empty();
        }
    }

    public Duration getAccessTokenTtl() {
        return properties.accessTokenTtl();
    }
}
