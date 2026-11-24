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
package org.jwcarman.jpa.pagination;

import lombok.Getter;

/**
 * Exception thrown when an unknown sort by value is provided that cannot be resolved to a valid
 * enum constant.
 *
 * <p>This exception is typically thrown when converting a string sort field name to an enum
 * constant implementing {@link SortPropertyProvider}, and the provided value does not match any of
 * the enum's constant names.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * try {
 *     Pageable pageable = paginator.pageableOf(pageSpec);
 * } catch (UnknownSortByValueException e) {
 *     // e.getProvidedValue() returns "FOO"
 *     // e.getExpectedValues() returns ["FIRST_NAME", "LAST_NAME", "EMAIL"]
 *     // e.getMessage() returns "Unknown sort by value \"FOO\", expecting one of FIRST_NAME, LAST_NAME, EMAIL."
 *     log.error("Invalid sort field provided", e);
 *     throw new BadRequestException("Invalid sort field: " + e.getProvidedValue());
 * }
 * }</pre>
 */
@Getter
public class UnknownSortByValueException extends RuntimeException {

  /** The invalid sort by value that was provided. */
  private final String providedValue;

  /** The array of expected enum constant names. */
  private final String[] expectedValues;

  /**
   * Creates a new exception for an unknown sort by value.
   *
   * @param providedValue the invalid value that was provided
   * @param expectedValues the array of valid enum constant names
   */
  public UnknownSortByValueException(String providedValue, String[] expectedValues) {
    super(buildMessage(providedValue, expectedValues));
    this.providedValue = providedValue;
    this.expectedValues = expectedValues;
  }

  private static String buildMessage(String providedValue, String[] expectedValues) {
    return String.format(
        "Unknown sort by value \"%s\", expecting one of %s.",
        providedValue, String.join(", ", expectedValues));
  }
}
