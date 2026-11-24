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
package org.jwcarman.jpa.search;

/**
 * How a {@link Searchable} field matches each word of a search term.
 *
 * <p>Every mode is case-insensitive: the column is wrapped in {@code LOWER(...)} and the word is
 * lowercased. The modes differ in whether the database can answer them from an index, which is the
 * reason to choose one over {@link #CONTAINS}.
 *
 * <p>For each word the searchable fields are ORed together, so the database can use indexes only
 * when <em>every</em> {@code @Searchable} field on the entity is {@link #PREFIX} or {@link #EXACT}
 * and indexed. A single {@link #CONTAINS} field makes the whole search scan the table.
 *
 * @see Searchable#match()
 */
public enum MatchMode {

  /**
   * The word appears anywhere in the field: {@code LOWER(col) LIKE '%word%' ESCAPE '\'}. The
   * default.
   *
   * <p>A leading wildcard cannot use an ordinary B-tree index, so this scans the table.
   */
  CONTAINS,

  /**
   * The field starts with the word: {@code LOWER(col) LIKE 'word%' ESCAPE '\'}.
   *
   * <p>Can use an index on {@code LOWER(col)} on PostgreSQL, where that index must be built with
   * {@code text_pattern_ops} (or the C collation). MySQL does not use functional indexes for {@code
   * LIKE}, and MariaDB has no expression indexes, so on those databases this still scans the table.
   */
  PREFIX,

  /**
   * The field equals the word: {@code LOWER(col) = 'word'}.
   *
   * <p>Can use a plain index on {@code LOWER(col)} (a functional index on MySQL; MariaDB has no
   * expression indexes, so there it scans); suited to codes such as a state ({@code OH}). Because
   * the term is split into words, a field whose value contains whitespace ({@code New York}) never
   * matches.
   */
  EXACT
}
