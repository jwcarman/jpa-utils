package org.jwcarman.jpa.integration;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "org.jwcarman.jpa.integration")
@EntityScan(basePackages = "org.jwcarman.jpa.integration")
public class TestConfig {
}
