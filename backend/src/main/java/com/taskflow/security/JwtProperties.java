package com.taskflow.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param secret         Base64-encoded HMAC key, at least 256 bits (32 bytes) once decoded
 * @param issuer         value of the {@code iss} claim; tokens from another issuer are rejected
 * @param accessTokenTtl lifetime of an access token
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl) {
}
