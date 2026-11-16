package com.taskflow.common.error;

import org.springframework.http.HttpStatus;

/** The request is well-formed but asks for something that is never valid. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
