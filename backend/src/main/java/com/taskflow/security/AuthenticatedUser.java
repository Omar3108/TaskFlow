package com.taskflow.security;

import java.util.UUID;

/**
 * The principal stored in the {@code SecurityContext} for an authenticated request.
 * Built from the access-token claims, so no database query is needed per request.
 */
public record AuthenticatedUser(UUID id, String email) {
}
