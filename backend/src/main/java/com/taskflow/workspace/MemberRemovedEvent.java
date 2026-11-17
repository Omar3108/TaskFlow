package com.taskflow.workspace;

import java.util.UUID;

/**
 * Published when a user stops being a member of a workspace (removed or left).
 *
 * <p>Other features react to it (cards drop the user as assignee) without the workspace
 * feature having to know that they exist. Listeners run synchronously, inside the same
 * transaction as the removal, so either everything is applied or nothing is.
 */
public record MemberRemovedEvent(UUID workspaceId, UUID userId) {
}
