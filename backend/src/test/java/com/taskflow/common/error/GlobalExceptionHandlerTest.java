package com.taskflow.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;

/**
 * The two safety nets of the handler, which integration tests rarely reach: a constraint
 * violation that slipped past the service checks, and an unexpected exception.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void constraintViolationsBecomeAGenericConflict() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement",
                new SQLException("duplicate key value violates unique constraint \"uq_users_email\""));

        ProblemDetail problem = handler.handleDataIntegrityViolation(exception);

        assertThat(problem.getStatus()).isEqualTo(409);
        // The database message (table and constraint names) must not reach the client.
        assertThat(problem.getDetail())
                .isEqualTo("The request conflicts with the current state of the resource")
                .doesNotContain("uq_users_email");
    }

    @Test
    void unexpectedExceptionsBecomeA500WithoutInternalDetails() {
        ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("secret stack detail"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred").doesNotContain("secret");
    }

    @Test
    void apiExceptionsKeepTheirStatusAndMessage() {
        ProblemDetail problem = handler.handleApiException(new ConflictException("Already a member"));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).isEqualTo("Already a member");
    }
}
