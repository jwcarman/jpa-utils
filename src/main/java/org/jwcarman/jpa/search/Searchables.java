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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for building JPA Criteria API predicates based on fields marked with {@link
 * Searchable}.
 *
 * <p>This utility automatically generates case-insensitive predicates for all String fields or
 * methods annotated with {@code @Searchable}: every word of the search term must match at least one
 * of them, each field according to its {@link MatchMode}. It handles SQL wildcard escaping and
 * provides null-safe operation.
 *
 * <p>The words reach the database as bind parameters, never as SQL text: the predicate is built
 * with the Criteria API, and Hibernate binds Criteria values as parameters by default ({@code
 * hibernate.criteria.value_handling_mode} is {@code BIND}). Don't set that mode to {@code INLINE}
 * for searches over browser input: Hibernate then writes the words into the SQL as quoted literals,
 * and safety rests on its quoting matching how the database reads backslashes (PostgreSQL's {@code
 * standard_conforming_strings}, MySQL's {@code NO_BACKSLASH_ESCAPES}).
 *
 * <h2>Key Features</h2>
 *
 * <ul>
 *   <li><strong>Annotation-Driven:</strong> Automatically discovers {@code @Searchable} fields
 *   <li><strong>Case-Insensitive:</strong> All searches use lowercase comparison
 *   <li><strong>Wildcard Escaping:</strong> Automatically escapes {@code %}, {@code _}, and {@code
 *       \}
 *   <li><strong>Word by Word:</strong> The term is split on whitespace, Unicode spaces, control and
 *       invisible format characters, and each word is cut to {@value #MAX_WORD_LENGTH} characters;
 *       a row matches when every word appears in at least one searchable field, in any order (at
 *       most {@value #MAX_SEARCH_WORDS} words are used)
 *   <li><strong>Null-Safe:</strong> Returns all results when search term is null or blank
 *   <li><strong>Type-Safe:</strong> Only processes String fields
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>With SearchableRepository (Recommended)</h3>
 *
 * <pre>{@code
 * @Repository
 * public interface PersonRepository extends SearchableRepository<Person, UUID> {
 *     // search() method is inherited
 * }
 *
 * @Service
 * public class PersonService {
 *
 *     private static final Paginator PAGINATOR = Paginator.of(Person.class, PersonSort.class);
 *
 *     @Autowired
 *     private PersonRepository personRepository;
 *
 *     public PageDto<PersonDto> search(String query, PageSpec pageSpec) {
 *         return PAGINATOR.paginate(pageSpec,
 *                 pageable -> personRepository.search(query, pageable),
 *                 PersonDto::fromEntity);
 *     }
 * }
 * }</pre>
 *
 * <h3>Manual Usage with JpaSpecificationExecutor</h3>
 *
 * <pre>{@code
 * @Repository
 * public interface PersonRepository extends JpaRepository<Person, UUID>, JpaSpecificationExecutor<Person> {
 * }
 *
 * @Service
 * public class PersonService {
 *
 *     @Autowired
 *     private PersonRepository personRepository;
 *
 *     public Page<Person> search(String query, Pageable pageable) {
 *         Specification<Person> spec = (root, criteriaQuery, criteriaBuilder) ->
 *             Searchables.createSearchPredicate(query, root, criteriaBuilder);
 *
 *         return personRepository.findAll(spec, pageable);
 *     }
 * }
 * }</pre>
 *
 * <h3>Combining with Other Specifications</h3>
 *
 * <pre>{@code
 * @Service
 * public class PersonService {
 *
 *     @Autowired
 *     private PersonRepository personRepository;
 *
 *     public Page<Person> searchActive(String query, Pageable pageable) {
 *         Specification<Person> searchSpec = (root, criteriaQuery, criteriaBuilder) ->
 *             Searchables.createSearchPredicate(query, root, criteriaBuilder);
 *
 *         Specification<Person> activeSpec = (root, criteriaQuery, criteriaBuilder) ->
 *             criteriaBuilder.isTrue(root.get("active"));
 *
 *         // Combine specifications: search AND active
 *         Specification<Person> combined = searchSpec.and(activeSpec);
 *
 *         return personRepository.findAll(combined, pageable);
 *     }
 * }
 * }</pre>
 *
 * <h3>Example Entity</h3>
 *
 * <pre>{@code
 * @Entity
 * public class Person extends BaseEntity {
 *
 *     @Searchable
 *     private String firstName;
 *
 *     @Searchable
 *     private String lastName;
 *
 *     @Searchable
 *     private String email;
 *
 *     private String internalNotes;  // NOT searchable
 *
 *     // When searching for "john", will match:
 *     // - firstName LIKE '%john%' OR
 *     // - lastName LIKE '%john%' OR
 *     // - email LIKE '%john%'
 * }
 * }</pre>
 *
 * <h2>Search Behavior</h2>
 *
 * <h3>Wildcard Escaping</h3>
 *
 * <p>Special SQL wildcard characters are automatically escaped:
 *
 * <pre>{@code
 * // User searches for: "100%"
 * // Automatically escaped to: "100\%"
 * // SQL LIKE pattern becomes: "%100\\%%"
 *
 * // User searches for: "test_value"
 * // Automatically escaped to: "test\_value"
 * // SQL LIKE pattern becomes: "%test\\_value%"
 * }</pre>
 *
 * <h3>Null and Blank Handling</h3>
 *
 * <pre>{@code
 * // Null search term - returns ALL results
 * Predicate predicate = Searchables.createSearchPredicate(null, root, builder);
 * // Result: builder.conjunction() (always true)
 *
 * // Blank search term - returns ALL results
 * Predicate predicate = Searchables.createSearchPredicate("   ", root, builder);
 * // Result: builder.conjunction() (always true)
 *
 * // Empty search term - returns ALL results
 * Predicate predicate = Searchables.createSearchPredicate("", root, builder);
 * // Result: builder.conjunction() (always true)
 * }</pre>
 *
 * <h3>No Searchable Fields</h3>
 *
 * <pre>{@code
 * // If entity has no @Searchable fields
 * Predicate predicate = Searchables.createSearchPredicate("query", root, builder);
 * // Result: builder.conjunction() (always true, returns all results)
 * }</pre>
 *
 * <h2>Performance Considerations</h2>
 *
 * <ul>
 *   <li><strong>Database Indexes:</strong> LIKE queries with leading wildcards ({@code %term%})
 *       cannot use indexes efficiently
 *   <li><strong>Query Size:</strong> The predicate has one comparison per word per
 *       {@code @Searchable} field
 *   <li><strong>Match Modes:</strong> {@link MatchMode#PREFIX} and {@link MatchMode#EXACT} can use
 *       an index on {@code LOWER(column)}, but only when every searchable field of the entity uses
 *       one of them
 *   <li><strong>Full-Text Search:</strong> For large text fields or complex search requirements,
 *       consider database-specific full-text search
 *   <li><strong>Pagination:</strong> Always use pagination to limit result sets
 * </ul>
 *
 * <h2>Limitations</h2>
 *
 * <ul>
 *   <li>Only supports String fields/methods
 *   <li>Does not search across entity relationships
 *   <li>Uses simple LIKE queries (not full-text search)
 *   <li>Leading wildcard ({@code %term}) prevents index usage
 * </ul>
 *
 * @see Searchable
 * @see org.jwcarman.jpa.spring.search.SearchableRepository
 */
@Slf4j
public final class Searchables {

  // -------------------------- STATIC FIELDS --------------------------

  /**
   * Cache of searchable attributes per entity type to avoid repeated reflection operations. Key:
   * the metamodel's entity type, Value: the searchable String attributes with their match modes.
   * Weakly keyed, so a closed {@code EntityManagerFactory}'s metamodel isn't kept alive by the
   * cache.
   */
  private static final Map<EntityType<?>, List<SearchableAttribute>> SEARCHABLE_ATTRIBUTES_CACHE =
      Collections.synchronizedMap(new WeakHashMap<>());

  /**
   * The most words of a search term that are matched; any beyond are ignored, bounding the size of
   * the query.
   */
  public static final int MAX_SEARCH_WORDS = 10;

  /**
   * The most characters (code points) of a word that are matched; a longer word is cut to this
   * length, bounding the size of each bound value.
   */
  public static final int MAX_WORD_LENGTH = 100;

  /**
   * What separates words: any whitespace, Unicode space or line separator ({@code \p{Z}}, e.g. a
   * non-breaking space), control character ({@code \p{Cc}}, e.g. tab or NUL) or invisible format
   * character ({@code \p{Cf}}, e.g. a zero-width space or byte-order mark). None of them ever
   * reaches the database. The zero-width non-joiner and joiner (U+200C, U+200D) are the exception:
   * they are part of words in Persian, Indic scripts and emoji sequences, so they stay inside
   * words.
   */
  private static final Pattern SEPARATORS =
      Pattern.compile("[\\p{Z}\\p{Cc}\\p{Cf}&&[^\\u200C\\u200D]]+");

  // --------------------------- CONSTRUCTORS ---------------------------

  private Searchables() {}

  // -------------------------- STATIC METHODS --------------------------

  /**
   * Creates a JPA Criteria API predicate that searches all {@code @Searchable} String fields.
   *
   * <p>The search term is split into words on whitespace, Unicode spaces, control and invisible
   * format characters (lowercased, cut to {@value #MAX_WORD_LENGTH} characters, duplicates dropped,
   * at most {@value #MAX_SEARCH_WORDS}). A row matches when <em>every</em> word appears,
   * case-insensitively, in at least one searchable field, so {@code "john smith"} finds a row with
   * {@code firstName = "John"} and {@code lastName = "Smith"}, in either order. SQL wildcards in
   * the words are escaped.
   *
   * <p><strong>Behavior:</strong>
   *
   * <ul>
   *   <li>If {@code searchTerm} is null or blank → returns {@code builder.conjunction()} (all
   *       results)
   *   <li>If no {@code @Searchable} fields exist → returns {@code builder.conjunction()} (all
   *       results)
   *   <li>Otherwise → an AND, over the words, of an OR over the searchable fields
   * </ul>
   *
   * <p><strong>Per Mode:</strong> {@link MatchMode#CONTAINS} → {@code LOWER(col) LIKE '%word%'
   * ESCAPE '\'}; {@link MatchMode#PREFIX} → {@code LOWER(col) LIKE 'word%' ESCAPE '\'}; {@link
   * MatchMode#EXACT} → {@code LOWER(col) = 'word'}.
   *
   * <p><strong>Search Pattern:</strong>
   *
   * <pre>{@code
   * // Input: "john smith"
   * // SQL: WHERE (LOWER(firstName) LIKE '%john%' ESCAPE '\' OR LOWER(lastName) LIKE '%john%' ESCAPE '\')
   * //        AND (LOWER(firstName) LIKE '%smith%' ESCAPE '\' OR LOWER(lastName) LIKE '%smith%' ESCAPE '\')
   * }</pre>
   *
   * @param searchTerm the search term to match (case-insensitive, wildcards escaped), may be null
   *     or blank
   * @param root the query root representing the entity, must not be null
   * @param builder the criteria builder for constructing predicates, must not be null
   * @return a predicate matching every word of the search term across the searchable fields, never
   *     null
   * @throws NullPointerException if {@code root} or {@code builder} is null
   */
  public static Predicate createSearchPredicate(
      String searchTerm, Root<?> root, CriteriaBuilder builder) {
    final List<String> words = wordsOf(searchTerm);
    if (words.isEmpty()) {
      return builder.conjunction(); // No filtering if searchTerm is empty/null
    }

    final var attributes =
        SEARCHABLE_ATTRIBUTES_CACHE.computeIfAbsent(
            root.getModel(), Searchables::searchableAttributesOf);
    if (attributes.isEmpty()) {
      return builder.conjunction();
    }

    return builder.and(
        words.stream()
            .map(
                word ->
                    builder.or(
                        attributes.stream()
                            .map(attribute -> matches(root, builder, attribute, word))
                            .toArray(Predicate[]::new)))
            .toArray(Predicate[]::new));
  }

  /**
   * Splits a search term into the words that are matched: separated by whitespace, Unicode spaces,
   * control and invisible format characters; lowercased; cut to {@value #MAX_WORD_LENGTH} code
   * points; without duplicates; in order; at most {@value #MAX_SEARCH_WORDS}.
   *
   * @param searchTerm the search term, may be null or blank
   * @return the words, empty for a null term or one made only of separators
   */
  static List<String> wordsOf(String searchTerm) {
    if (searchTerm == null) {
      return List.of();
    }
    return SEPARATORS
        .splitAsStream(searchTerm)
        .filter(word -> !word.isEmpty())
        .map(word -> cut(word.toLowerCase(Locale.ROOT)))
        .distinct()
        .limit(MAX_SEARCH_WORDS)
        .toList();
  }

  private static String cut(String word) {
    final int codePoints = Math.min(MAX_WORD_LENGTH, word.codePointCount(0, word.length()));
    return word.substring(0, word.offsetByCodePoints(0, codePoints));
  }

  /**
   * Finds the searchable attributes of an entity type: its String attributes whose field or getter
   * carries {@link Searchable}, each with its match mode. Uncached; {@link #createSearchPredicate}
   * caches the result per entity type.
   *
   * @param entityType the entity type to inspect
   * @return the searchable String attributes with their match modes (may be empty but never null)
   */
  static List<SearchableAttribute> searchableAttributesOf(EntityType<?> entityType) {
    final List<SearchableAttribute> searchableAttributes =
        entityType.getSingularAttributes().stream()
            .filter(attribute -> String.class.equals(attribute.getJavaType()))
            .flatMap(
                attribute ->
                    searchableOf(attribute).stream()
                        .map(
                            searchable ->
                                new SearchableAttribute(attribute.getName(), searchable.match())))
            .toList();
    if (searchableAttributes.isEmpty()) {
      log.warn("No searchable attributes found for {}", entityType.getName());
    }
    return searchableAttributes;
  }

  private static Predicate matches(
      Root<?> root, CriteriaBuilder builder, SearchableAttribute attribute, String word) {
    final var lower = builder.lower(root.get(attribute.name()).as(String.class));
    return switch (attribute.match()) {
      case CONTAINS -> builder.like(lower, "%" + sanitizeSearchTerm(word) + "%", '\\');
      case PREFIX -> builder.like(lower, sanitizeSearchTerm(word) + "%", '\\');
      case EXACT -> builder.equal(lower, word);
    };
  }

  private static Optional<Searchable> searchableOf(SingularAttribute<?, ?> attribute) {
    return Optional.ofNullable(
        switch (attribute.getJavaMember()) {
          case Method m -> m.getAnnotation(Searchable.class);
          case Field f -> f.getAnnotation(Searchable.class);
          case null, default -> null;
        });
  }

  /** A searchable attribute of an entity type: its name and how it matches. */
  record SearchableAttribute(String name, MatchMode match) {}

  private static String sanitizeSearchTerm(String searchTerm) {
    return searchTerm
        .replace("\\", "\\\\") // Escape backslash
        .replace("%", "\\%") // Escape wildcard '%'
        .replace("_", "\\_"); // Escape wildcard '_'
  }
}
