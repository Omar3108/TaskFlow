package com.taskflow.common.error;

import org.springframework.http.HttpStatus;

/** The caller is known and can see the resource, but is not allowed to perform the action. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
