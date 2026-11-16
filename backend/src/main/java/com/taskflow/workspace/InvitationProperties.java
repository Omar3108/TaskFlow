package com.taskflow.workspace;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param ttl how long an invitation can be accepted after it is created
 */
@Validated
@ConfigurationProperties(prefix = "app.invitation")
public record InvitationProperties(@NotNull Duration ttl) {
}
