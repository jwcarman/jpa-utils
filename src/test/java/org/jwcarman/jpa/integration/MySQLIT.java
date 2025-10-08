package org.jwcarman.jpa.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

/**
 * Integration tests for MySQL database.
 * <p>
 * Tests all pagination, sorting, and search functionality against a real MySQL database
 * running in a Docker container via Testcontainers.
 * </p>
 */
@Testcontainers
@Tag("integration")
public class MySQLIT extends DatabaseIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:9.2")
            .withStartupTimeout(Duration.ofSeconds(60))
            .withStartupAttempts(1)
            .withCommand("--skip-log-bin",
                    "--innodb-flush-log-at-trx-commit=2",
                    "--max-connections=50");
}
