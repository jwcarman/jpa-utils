package org.jwcarman.jpa.spring.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Boot auto-configuration for jpa-utils' Spring MVC support: registers a {@link SortDirectionConverter} so
 * that {@link PageParams} binds {@code sortDirection} case-insensitively.
 *
 * <p>Applies to servlet web applications with Spring MVC on the classpath. Spring Boot adds every
 * {@code Converter} bean to the MVC conversion service. Define your own {@code SortDirectionConverter} bean to
 * replace this one.</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
public class PagingWebAutoConfiguration {

    /**
     * Creates the auto-configuration; Spring Boot instantiates it.
     */
    public PagingWebAutoConfiguration() {
        // No state; the bean method below does the work.
    }

    /**
     * The lenient {@code sortDirection} converter.
     *
     * @return the converter
     */
    @Bean
    @ConditionalOnMissingBean
    public SortDirectionConverter sortDirectionConverter() {
        return new SortDirectionConverter();
    }
}
