package com.taskflow.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Opaque secret tokens (refresh tokens, invitation links).
 *
 * <p>The raw token is handed to the client once; only its SHA-256 hash is stored, so a leaked
 * database does not leak usable tokens. A fast hash is enough here (unlike passwords) because
 * the input is a 256-bit random value that cannot be guessed or brute-forced.
 */
public final class SecureTokens {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureTokens() {
    }

    /** A new URL-safe random token with 256 bits of entropy. */
    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Hex-encoded SHA-256 of the token: the value that is safe to store and to look up by. */
    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            // Every Java runtime is required to provide SHA-256.
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
