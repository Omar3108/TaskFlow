package com.taskflow.workspace.dto;

/**
 * @param token the raw invitation token. Returned only here, at creation time; the server
 *              keeps just its hash, so it cannot be shown again
 */
public record InvitationCreatedResponse(InvitationResponse invitation, String token) {
}
