package org.jwcarman.jpa.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

/**
 * Integration tests for MariaDB database.
 * <p>
 * Tests all pagination, sorting, and search functionality against a real MariaDB database
 * running in a Docker container via Testcontainers.
 * </p>
 */
@Testcontainers
@Tag("integration")
public class MariaDBIT extends DatabaseIT {

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.6")
            .withStartupTimeout(Duration.ofSeconds(60))
            .withStartupAttempts(1)
            .withCommand("--skip-log-bin",
                        "--innodb-flush-log-at-trx-commit=2",
                        "--max-connections=50");
}
