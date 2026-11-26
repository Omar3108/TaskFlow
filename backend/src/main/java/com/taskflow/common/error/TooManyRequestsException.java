package com.taskflow.common.error;

import org.springframework.http.HttpStatus;

/** The caller has hit a rate limit and should try again later. */
public class TooManyRequestsException extends ApiException {

    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
