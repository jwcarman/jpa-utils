package org.jwcarman.jpa.spring.web;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PagingWebAutoConfigurationTest {

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PagingWebAutoConfiguration.class));

    @Test
    void registers_the_converter_in_a_servlet_web_application() {
        webRunner.run(context -> assertThat(context).hasSingleBean(SortDirectionConverter.class));
    }

    @Test
    void backs_off_when_the_application_defines_its_own_converter() {
        webRunner.withUserConfiguration(CustomConverterConfig.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(SortDirectionConverter.class)
                        .getBean(SortDirectionConverter.class)
                        .isSameAs(CustomConverterConfig.CUSTOM));
    }

    @Test
    void does_nothing_outside_a_web_application() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PagingWebAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(SortDirectionConverter.class));
    }

    @Test
    void is_listed_for_spring_boot_to_discover() {
        assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()))
                .contains(PagingWebAutoConfiguration.class.getName());
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomConverterConfig {
        static final SortDirectionConverter CUSTOM = new SortDirectionConverter();

        @Bean
        SortDirectionConverter customSortDirectionConverter() {
            return CUSTOM;
        }
    }
}
