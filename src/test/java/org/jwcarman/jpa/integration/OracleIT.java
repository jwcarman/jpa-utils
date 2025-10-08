package org.jwcarman.jpa.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

/**
 * Integration tests for Oracle Database Free.
 * <p>
 * Tests all pagination, sorting, and search functionality against a real Oracle database
 * running in a Docker container via Testcontainers. Uses Oracle Database Free (formerly XE).
 * </p>
 */
@Testcontainers
@Tag("integration")
public class OracleIT extends DatabaseIT {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:23-slim-faststart")
            .withStartupTimeout(Duration.ofSeconds(120))
            .withStartupAttempts(1);
}
