package org.jwcarman.jpa.spring.search;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

@Configuration
@EnableJpaRepositories(basePackages = "org.jwcarman.jpa.spring.search")
@EnableAutoConfiguration
@EnableJpaAuditing
public class SpringBootConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("test");
    }
}
