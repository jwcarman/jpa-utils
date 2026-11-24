/*
 * Copyright © 2025 James Carman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jwcarman.jpa.spring.web;

import static org.assertj.core.api.Assertions.assertThat;

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

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PagingWebAutoConfigurationTest {

  private final WebApplicationContextRunner webRunner =
      new WebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(PagingWebAutoConfiguration.class));

  @Test
  void registers_the_converter_in_a_servlet_web_application() {
    webRunner.run(
        context ->
            assertThat(context)
                .hasSingleBean(SortDirectionConverter.class)
                .getBean(SortDirectionConverter.class)
                .isInstanceOf(SortDirectionConverter.class));
  }

  @Test
  void backs_off_when_the_application_defines_its_own_converter() {
    webRunner
        .withUserConfiguration(CustomConverterConfig.class)
        .run(
            context ->
                assertThat(context)
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
