package com.taskflow.auth;

import com.taskflow.common.error.UnauthorizedException;

/** The refresh token is missing, unknown, expired or already used. */
public class InvalidRefreshTokenException extends UnauthorizedException {

    public InvalidRefreshTokenException() {
        // One message for every cause: the client only needs to know it must log in again.
        super("Invalid or expired refresh token");
    }
}
