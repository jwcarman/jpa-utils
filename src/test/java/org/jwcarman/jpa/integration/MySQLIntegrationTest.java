package org.jwcarman.jpa.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for MySQL database.
 * <p>
 * Tests all pagination, sorting, and search functionality against a real MySQL database
 * running in a Docker container via Testcontainers.
 * </p>
 */
@Testcontainers
@Tag("integration")
public class MySQLIntegrationTest extends DatabaseIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:9.1");
}
