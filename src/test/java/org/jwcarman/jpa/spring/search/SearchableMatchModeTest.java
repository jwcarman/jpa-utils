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
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.jwcarman.jpa.search.Searchables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(
    showSql = false,
    properties =
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=org.jwcarman.jpa.spring.search.CapturingStatementInspector")
@ContextConfiguration(classes = {SpringBootConfig.class})
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchableMatchModeTest {

  @Autowired private LocationRepository locations;

  @Autowired private AnimalRepository animals;

  @BeforeEach
  void setUp() {
    // No name contains the letter "o"; the negative cases below rely on that.
    locations.save(new Location("HQ", "Columbus", "OH"));
    locations.save(new Location("Annex", "Cleveland", "OH"));
    locations.save(new Location("Lab", "Austin", "TX"));
    locations.save(new Location("Mart", "Albany", "New York"));
  }

  private List<String> namesFor(String term) {
    return locations.search(term, Pageable.unpaged()).map(Location::getName).getContent();
  }

  @Nested
  class Contains_is_the_default {

    @Test
    void matches_anywhere_in_the_field() {
      assertThat(namesFor("nne")).containsExactly("Annex");
    }
  }

  @Nested
  class Prefix {

    @Test
    void matches_the_start_of_the_field() {
      assertThat(namesFor("colum")).containsExactly("HQ");
    }

    @Test
    void ignores_case() {
      assertThat(namesFor("COLUM")).containsExactly("HQ");
    }

    @Test
    void does_not_match_the_middle() {
      assertThat(namesFor("umbus")).isEmpty();
    }

    @Test
    void treats_percent_literally() {
      assertThat(namesFor("c%")).isEmpty();
    }

    @Test
    void treats_underscore_literally() {
      assertThat(namesFor("c_")).isEmpty();
    }
  }

  @Nested
  class Exact {

    @Test
    void matches_the_whole_value_ignoring_case() {
      assertThat(namesFor("oh")).containsExactlyInAnyOrder("HQ", "Annex");
    }

    @Test
    void does_not_match_part_of_the_value() {
      assertThat(namesFor("o")).isEmpty();
    }

    @Test
    void treats_underscore_literally() {
      assertThat(namesFor("o_")).isEmpty();
    }

    @Test
    void treats_percent_literally() {
      assertThat(namesFor("o%")).isEmpty();
    }

    @Test
    void cannot_match_a_multi_word_value() {
      assertThat(namesFor("new york")).isEmpty();
    }

    @Test
    void reads_the_mode_from_an_annotated_getter() {
      final var rex = new Animal();
      rex.setName("Rex");
      rex.setSpecies("Dog");
      animals.save(rex);

      assertThat(animals.search("dog", Pageable.unpaged()).getTotalElements()).isEqualTo(1);
      assertThat(animals.search("do", Pageable.unpaged()).getTotalElements()).isZero();
    }
  }

  @Nested
  class Mixed_modes {

    @Test
    void match_each_word_by_its_own_field_mode() {
      assertThat(namesFor("annex oh")).containsExactly("Annex");
    }

    @Test
    void combine_prefix_and_exact() {
      assertThat(namesFor("colum oh")).containsExactly("HQ");
    }

    @Test
    void exclude_a_row_missing_any_word() {
      assertThat(namesFor("austin oh")).isEmpty();
    }
  }

  @Nested
  class Untrusted_input {

    @ParameterizedTest
    @ValueSource(
        strings = {
          "' OR '1'='1",
          "'; DROP TABLE location; --",
          "oh' --",
          "%",
          "_",
          "\\",
          "%%",
          "o\\h",
          "\\'||1--",
          "\\' OR 1=1 --",
          "'/**/OR/**/1=1--",
          "%' OR '1'='1",
          "\\%",
          "\\_",
          "abc\\",
          "oh\u0000' --",
          "\uD800",
          "\uFEFF%"
        })
    void matches_nothing_and_changes_nothing(String term) {
      assertThat(namesFor(term)).isEmpty();
      assertThat(locations.count()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"zq'x", "colum", "oh"})
    void never_appears_in_the_sql_text(String term) {
      CapturingStatementInspector.clear();

      namesFor(term);

      assertThat(CapturingStatementInspector.statements())
          .isNotEmpty()
          .noneMatch(sql -> sql.toLowerCase(Locale.ROOT).contains(term));
    }

    /**
     * The search's select statements; inserts left pending by {@code setUp} are flushed alongside
     * them.
     */
    private List<String> selects() {
      return CapturingStatementInspector.statements().stream()
          .filter(sql -> sql.startsWith("select"))
          .toList();
    }

    @Test
    void produces_the_same_sql_text_whatever_the_words() {
      CapturingStatementInspector.clear();
      namesFor("colum");
      final List<String> first = selects();

      CapturingStatementInspector.clear();
      namesFor("zq'x");

      assertThat(selects()).hasSize(1).isEqualTo(first);
    }

    @Test
    void matches_nothing_for_a_very_long_word() {
      assertThat(namesFor("o".repeat(100_000))).isEmpty();
    }

    @Test
    void binds_at_most_ten_words_of_at_most_a_hundred_characters() {
      final String manyLongWords =
          IntStream.range(0, 1_000)
              .mapToObj(i -> i + "x".repeat(500))
              .collect(Collectors.joining(" "));
      CapturingStatementInspector.clear();

      namesFor(manyLongWords);

      // Three searchable fields (name, city, state), so three bound values per word.
      assertThat(selects())
          .singleElement()
          .satisfies(
              sql ->
                  assertThat(sql.chars().filter(c -> c == '?').count())
                      .isEqualTo(3L * Searchables.MAX_SEARCH_WORDS));
    }
  }
}
