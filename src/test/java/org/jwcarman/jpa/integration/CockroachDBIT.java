package org.jwcarman.jpa.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.CockroachContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

/**
 * Integration tests for CockroachDB database.
 * <p>
 * Tests all pagination, sorting, and search functionality against a real CockroachDB database
 * running in a Docker container via Testcontainers. CockroachDB is PostgreSQL-compatible,
 * providing a distributed SQL database option.
 * </p>
 */
@Testcontainers
@Tag("integration")
public class CockroachDBIT extends DatabaseIT {

    @Container
    @ServiceConnection
    static CockroachContainer cockroach = new CockroachContainer("cockroachdb/cockroach:v24.3.4")
            .withStartupTimeout(Duration.ofSeconds(60))
            .withStartupAttempts(1);
}
