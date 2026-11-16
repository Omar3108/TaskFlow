package com.taskflow.auth;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param ttl          lifetime of a refresh token (and of its cookie)
 * @param cookieSecure whether the cookie is flagged {@code Secure} (HTTPS only); must be true
 *                     in production
 */
@Validated
@ConfigurationProperties(prefix = "app.refresh-token")
public record RefreshTokenProperties(@NotNull Duration ttl, boolean cookieSecure) {
}
