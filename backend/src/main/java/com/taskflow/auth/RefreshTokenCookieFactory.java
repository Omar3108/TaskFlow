package com.taskflow.auth;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Builds the cookie that carries the refresh token. */
@Component
public class RefreshTokenCookieFactory {

    public static final String COOKIE_NAME = "refresh_token";
    /** The browser only sends the cookie to the auth endpoints, not with every API call. */
    static final String COOKIE_PATH = "/api/v1/auth";

    private final RefreshTokenProperties properties;

    public RefreshTokenCookieFactory(RefreshTokenProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(String rawRefreshToken) {
        return build(rawRefreshToken, properties.ttl());
    }

    /** An empty cookie with Max-Age=0 tells the browser to delete it. */
    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)                       // not readable from JavaScript (XSS)
                .secure(properties.cookieSecure())    // HTTPS only
                .sameSite("Strict")                   // never sent on cross-site requests (CSRF)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
