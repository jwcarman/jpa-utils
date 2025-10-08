package org.jwcarman.jpa.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

/**
 * Integration tests for PostgreSQL database.
 * <p>
 * Tests all pagination, sorting, and search functionality against a real PostgreSQL database
 * running in a Docker container via Testcontainers.
 * </p>
 */
@Testcontainers
@Tag("integration")
public class PostgreSQLIT extends DatabaseIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withStartupTimeout(Duration.ofSeconds(60))
            .withStartupAttempts(1);
}
