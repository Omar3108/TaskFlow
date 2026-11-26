package com.taskflow.demo;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param enabled         whether "Try the demo" is offered
 * @param ttl             how long a sandbox lives before it is deleted
 * @param maxPerIpPerHour sandboxes one client address may create per hour
 * @param maxActive       sandboxes that may exist at the same time (protects a small database)
 */
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(boolean enabled, Duration ttl, int maxPerIpPerHour, int maxActive) {
}
