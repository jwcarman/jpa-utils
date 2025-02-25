package org.jwcarman.jpa.spring.search;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "org.jwcarman.jpa.spring.search")
@EnableAutoConfiguration
public class SpringBootConfig {

}
