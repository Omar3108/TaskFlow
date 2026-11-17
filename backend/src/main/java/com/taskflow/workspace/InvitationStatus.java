package com.taskflow.workspace;

public enum InvitationStatus {
    /** Waiting for the invitee. The only status in which an invitation can be used. */
    PENDING,
    ACCEPTED,
    DECLINED,
    /** Cancelled by a workspace admin. */
    REVOKED,
    /** Passed its expiry date and was replaced by a newer invitation. */
    EXPIRED
}
