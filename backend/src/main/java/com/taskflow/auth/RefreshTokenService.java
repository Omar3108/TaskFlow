package com.taskflow.auth;

import com.taskflow.security.SecureTokens;
import com.taskflow.user.User;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, rotates and revokes refresh tokens.
 *
 * <p>A refresh token is an opaque random value. The client receives the raw value once; the
 * database only stores its hash (see {@link SecureTokens}).
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties properties;
    private final Clock clock;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository, RefreshTokenProperties properties, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
        this.clock = clock;
    }

    /** Creates a new token for the user and returns its raw value (the only time it is visible). */
    @Transactional
    public String issue(User user) {
        String rawToken = SecureTokens.generate();
        Instant expiresAt = clock.instant().plus(properties.ttl());
        refreshTokenRepository.save(new RefreshToken(user, SecureTokens.hash(rawToken), expiresAt));
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
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(SecureTokens.hash(rawToken))
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
        refreshTokenRepository.findByTokenHash(SecureTokens.hash(rawToken))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    public record Rotation(User user, String newRawToken) {
    }
}
