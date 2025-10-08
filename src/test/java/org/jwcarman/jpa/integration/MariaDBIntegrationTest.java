package org.jwcarman.jpa.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for MariaDB database.
 * <p>
 * Tests all pagination, sorting, and search functionality against a real MariaDB database
 * running in a Docker container via Testcontainers.
 * </p>
 */
@Testcontainers
@Tag("integration")
public class MariaDBIntegrationTest extends DatabaseIntegrationTest {

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.6");
}
