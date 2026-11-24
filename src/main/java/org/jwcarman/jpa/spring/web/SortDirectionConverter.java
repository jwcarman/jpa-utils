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

import java.util.Arrays;
import org.jwcarman.jpa.pagination.SortDirection;
import org.springframework.core.convert.converter.Converter;

/**
 * Converts a request parameter into a {@link SortDirection}, ignoring case and surrounding
 * whitespace, so clients may send {@code asc}, {@code DESC} or {@code Desc}.
 *
 * <p>Spring's built-in enum conversion is case-sensitive, which rejects the lowercase values most
 * clients send. In a Spring Boot application this converter is registered automatically by {@link
 * PagingWebAutoConfiguration}. In a plain Spring MVC application, register it yourself:
 *
 * <pre>{@code
 * @Configuration
 * public class WebConfig implements WebMvcConfigurer {
 *     @Override
 *     public void addFormatters(FormatterRegistry registry) {
 *         registry.addConverter(new SortDirectionConverter());
 *     }
 * }
 * }</pre>
 *
 * <p>A blank value converts to {@code null}, which a {@link org.jwcarman.jpa.pagination.PageSpec}
 * treats as absent (ascending). Any other value that is not a direction is rejected with an {@link
 * IllegalArgumentException}, which Spring MVC reports as 400 Bad Request.
 */
public class SortDirectionConverter implements Converter<String, SortDirection> {

  /** Creates a converter. It is stateless and thread-safe, so one instance serves every request. */
  public SortDirectionConverter() {
    // No state to initialize.
  }

  /**
   * Converts the given value.
   *
   * @param source the request parameter value
   * @return the matching direction, or {@code null} if the value is blank
   * @throws IllegalArgumentException if the value is neither blank nor a direction
   */
  @Override
  public SortDirection convert(String source) {
    final String value = source.strip();
    if (value.isEmpty()) {
      return null;
    }
    return Arrays.stream(SortDirection.values())
        .filter(direction -> direction.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "Unknown sort direction \"%s\", expecting one of ASC, DESC (case-insensitive).",
                        value)));
  }
}
