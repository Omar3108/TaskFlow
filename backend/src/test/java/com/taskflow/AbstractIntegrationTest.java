package com.taskflow;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests. Boots the full application against a real PostgreSQL
 * running in Docker.
 *
 * <p>The container is a JVM-wide singleton: it is started once in the static initializer and
 * shared by every subclass, so Spring can also reuse the cached application context across
 * test classes. Testcontainers removes the container when the JVM exits.
 *
 * <p>The database is not cleaned between tests: each test creates its own data (for example a
 * user with a unique e-mail), which keeps tests independent and able to run in any order.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // @ServiceConnection points spring.datasource.* (and Flyway) at this container.
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }
}
