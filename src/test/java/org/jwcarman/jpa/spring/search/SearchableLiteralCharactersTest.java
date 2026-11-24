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
package org.jwcarman.jpa.spring.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;

/**
 * Searches for the characters that mean something to SQL ({@code \}, {@code %}, {@code _}, {@code
 * '}) find exactly the rows holding them, in every match mode. A wrong escaping order or a missing
 * escape makes these match other rows, or none.
 */
@DataJpaTest(showSql = false)
@ContextConfiguration(classes = {SpringBootConfig.class})
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchableLiteralCharactersTest {

  @Autowired private LocationRepository locations;

  @BeforeEach
  void setUp() {
    //                         name (CONTAINS)  city (PREFIX)  state (EXACT)
    locations.save(new Location("back\\slash", "b\\x", "a\\b"));
    locations.save(new Location("50%off", "5%x", "a%c"));
    locations.save(new Location("under_score", "u_x", "a_b"));
    locations.save(new Location("O'Hare", "o'x", "q'r"));
    // A decoy for an unescaped "_" (matches any one character) or "%" (matches anything).
    locations.save(new Location("decoy", "uzx", "azb"));
  }

  private List<String> namesFor(String term) {
    return locations.search(term, Pageable.unpaged()).map(Location::getName).getContent();
  }

  @Nested
  class Contains {

    @ParameterizedTest
    @ValueSource(strings = {"\\", "k\\s", "back\\slash"})
    void finds_a_backslash_literally(String term) {
      assertThat(namesFor(term)).containsExactly("back\\slash");
    }

    @Test
    void does_not_treat_a_doubled_backslash_as_one() {
      assertThat(namesFor("\\\\")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "50%", "0%o"})
    void finds_a_percent_sign_literally(String term) {
      assertThat(namesFor(term)).containsExactly("50%off");
    }

    @ParameterizedTest
    @ValueSource(strings = {"_", "r_s", "under_"})
    void finds_an_underscore_literally(String term) {
      assertThat(namesFor(term)).containsExactly("under_score");
    }

    @Test
    void does_not_treat_an_underscore_as_any_character() {
      assertThat(namesFor("5_%")).isEmpty();
    }

    @Test
    void finds_a_quote_literally() {
      assertThat(namesFor("o'h")).containsExactly("O'Hare");
    }
  }

  @Nested
  class Prefix {

    @Test
    void finds_a_backslash_literally() {
      assertThat(namesFor("b\\")).containsExactly("back\\slash");
    }

    @Test
    void finds_a_percent_sign_literally() {
      assertThat(namesFor("5%")).containsExactly("50%off");
    }

    @Test
    void finds_an_underscore_literally() {
      assertThat(namesFor("u_")).containsExactly("under_score");
    }
  }

  @Nested
  class Exact {

    @Test
    void matches_a_value_with_a_backslash() {
      assertThat(namesFor("a\\b")).containsExactly("back\\slash");
    }

    @Test
    void matches_a_value_with_a_percent_sign() {
      assertThat(namesFor("a%c")).containsExactly("50%off");
    }

    @Test
    void matches_a_value_with_an_underscore() {
      assertThat(namesFor("a_b")).containsExactly("under_score");
    }

    @Test
    void matches_a_value_with_a_quote() {
      assertThat(namesFor("q'r")).containsExactly("O'Hare");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a%b", "a_", "a%"})
    void never_treats_wildcards_as_wildcards(String term) {
      assertThat(namesFor(term)).isEmpty();
    }
  }
}
