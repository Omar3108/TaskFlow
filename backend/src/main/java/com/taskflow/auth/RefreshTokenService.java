package com.taskflow.auth;

import com.taskflow.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, rotates and revokes refresh tokens.
 *
 * <p>A refresh token is an opaque random value (256 bits). The client receives the raw value
 * once; the database only stores its SHA-256 hash, so a leaked database does not leak usable
 * tokens. A fast hash is enough here (unlike passwords) because the input is already a
 * high-entropy random value that cannot be brute-forced.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository, RefreshTokenProperties properties, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
        this.clock = clock;
    }

    /** Creates a new token for the user and returns its raw value (the only time it is visible). */
    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant expiresAt = clock.instant().plus(properties.ttl());
        refreshTokenRepository.save(new RefreshToken(user, hash(rawToken), expiresAt));
        return rawToken;
    }

    /**
     * Exchanges a valid token for a new one. Every token is single-use.
     *
     * <p>Reuse detection: presenting a token that was already rotated means two parties hold
     * it (the real user and possibly a thief), so every session of that user is revoked.
     * {@code noRollbackFor} keeps that revocation committed even though the method then throws.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String rawToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = clock.instant();
        User user = current.getUser();

        if (current.isRevoked()) {
            int revoked = refreshTokenRepository.revokeAllActiveByUserId(user.getId(), now);
            log.warn("Refresh token reuse detected for user {}; revoked {} active session(s)", user.getId(), revoked);
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        current.revoke(now);
        return new Rotation(user, issue(user));
    }

    /** Logout. Unknown or already revoked tokens are ignored, so the operation is idempotent. */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    private static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            // Every Java runtime is required to provide SHA-256.
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public record Rotation(User user, String newRawToken) {
    }
}
