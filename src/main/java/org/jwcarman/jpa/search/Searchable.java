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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field or getter method as searchable, enabling it to be automatically included in search
 * queries.
 *
 * <p>When a field or method is annotated with {@code @Searchable}, the {@link Searchables} utility
 * will automatically include it in the case-insensitive queries generated via {@link
 * Searchables#createSearchPredicate(String, jakarta.persistence.criteria.Root,
 * jakarta.persistence.criteria.CriteriaBuilder)}. Each field matches according to its {@link
 * #match() match mode}: {@link MatchMode#CONTAINS} (the default), {@link MatchMode#PREFIX} or
 * {@link MatchMode#EXACT}.
 *
 * <p>This annotation-driven approach eliminates the need to manually build search specifications
 * for each entity, providing a declarative way to define which fields should be searchable.
 *
 * <h2>Supported Types</h2>
 *
 * <p>Currently, only {@code String} fields and methods are supported. Other types are silently
 * ignored.
 *
 * <h2>Target Elements</h2>
 *
 * <ul>
 *   <li><strong>FIELD:</strong> Applied directly to entity fields (field access, the usual case)
 *   <li><strong>METHOD:</strong> Applied to getter methods, for an entity that uses property access
 *       ({@code @Id} on a getter). On a field-access entity an annotated getter is not seen.
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Field Annotation</h3>
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
 *     // Constructors, getters, setters...
 * }
 *
 * // Search will match across firstName, lastName, and email
 * // but NOT internalNotes
 * }</pre>
 *
 * <h3>Selective Searchability</h3>
 *
 * <pre>{@code
 * @Entity
 * public class Product extends BaseEntity {
 *
 *     @Searchable
 *     private String name;           // Searchable
 *
 *     @Searchable
 *     private String description;    // Searchable
 *
 *     private String sku;            // NOT searchable (exact match only)
 *     private BigDecimal price;      // NOT searchable (not a String)
 *     private String internalCode;   // NOT searchable
 *
 *     // Constructors, getters, setters...
 * }
 * }</pre>
 *
 * <h3>Choosing a Match Mode</h3>
 *
 * <pre>{@code
 * @Entity
 * public class Customer extends BaseEntity {
 *
 *     @Searchable(match = MatchMode.PREFIX)
 *     private String lastName;       // LOWER(last_name) LIKE 'smi%'
 *
 *     @Searchable(match = MatchMode.EXACT)
 *     private String state;          // LOWER(state) = 'oh'
 * }
 * }</pre>
 *
 * <h3>Using with SearchableRepository</h3>
 *
 * <pre>{@code
 * @Repository
 * public interface PersonRepository extends SearchableRepository<Person, UUID> {
 *     // Inherits search() method
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
 *         // Automatically searches across all @Searchable fields
 *         return PAGINATOR.paginate(pageSpec,
 *                 pageable -> personRepository.search(query, pageable),
 *                 PersonDto::fromEntity);
 *     }
 * }
 *
 * // Example: search("john smith") matches a row when "john" is in firstName, lastName or email,
 * // AND "smith" is in firstName, lastName or email.
 * }</pre>
 *
 * <h3>Manual Usage with Searchables Utility</h3>
 *
 * <pre>{@code
 * @Repository
 * public interface CustomPersonRepository extends JpaRepository<Person, UUID>, JpaSpecificationExecutor<Person> {
 * }
 *
 * @Service
 * public class PersonService {
 *
 *     @Autowired
 *     private CustomPersonRepository personRepository;
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
 * <h3>Using on Getter Methods (Property Access)</h3>
 *
 * <pre>{@code
 * @Entity
 * public class Article {
 *
 *     private String id;
 *     private String title;
 *
 *     @Id
 *     public String getId() { return id; }
 *
 *     @Searchable
 *     public String getTitle() { return title; }
 *
 *     // setters...
 * }
 * }</pre>
 *
 * <h2>Search Behavior</h2>
 *
 * <ul>
 *   <li><strong>Case-Insensitive:</strong> All searches are case-insensitive
 *   <li><strong>Per-Field Mode:</strong> Each field matches by its {@link #match()} mode; {@link
 *       MatchMode#CONTAINS} uses SQL LIKE with wildcards on both sides (e.g., {@code %john%})
 *   <li><strong>Word by Word:</strong> The term is split on whitespace; a row matches when every
 *       word matches at least one searchable field
 *   <li><strong>Wildcard Escaping:</strong> Special characters ({@code %}, {@code _}, {@code \})
 *       are escaped for {@link MatchMode#CONTAINS} and {@link MatchMode#PREFIX}; {@link
 *       MatchMode#EXACT} compares with {@code =}
 *   <li><strong>Null-Safe:</strong> Null or blank search terms return all results
 * </ul>
 *
 * <h2>Best Practices</h2>
 *
 * <ul>
 *   <li>Mark fields that users would naturally want to search
 *   <li>Avoid marking fields with sensitive information
 *   <li>Don't mark too many fields (can impact query performance)
 *   <li>Consider using specific query methods for exact matches (e.g., SKU, email)
 *   <li>Test search performance with realistic data volumes
 * </ul>
 *
 * <h2>Limitations</h2>
 *
 * <ul>
 *   <li>Only works with String fields/methods
 *   <li>Does not support searching across relationships (use custom specifications for that)
 *   <li>Uses simple LIKE and equality queries (consider full-text search for advanced use cases)
 *   <li>Indexes help only when every searchable field on the entity is {@link MatchMode#PREFIX} or
 *       {@link MatchMode#EXACT}; see {@link MatchMode}
 * </ul>
 *
 * @see Searchables
 * @see org.jwcarman.jpa.spring.search.SearchableRepository
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Searchable {

  /**
   * How this field matches each word of a search term.
   *
   * @return the match mode, {@link MatchMode#CONTAINS} by default
   */
  MatchMode match() default MatchMode.CONTAINS;
}
