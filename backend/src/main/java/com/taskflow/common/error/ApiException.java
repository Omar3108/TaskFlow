package com.taskflow.common.error;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected business errors. Each subclass maps to one HTTP status and is
 * translated into a {@code ProblemDetail} response by {@link GlobalExceptionHandler}.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
