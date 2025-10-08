package org.jwcarman.jpa.search;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field or getter method as searchable, enabling it to be automatically included in search queries.
 *
 * <p>When a field or method is annotated with {@code @Searchable}, the {@link Searchables} utility will
 * automatically include it in case-insensitive LIKE queries generated via
 * {@link Searchables#createSearchPredicate(String, jakarta.persistence.criteria.Root, jakarta.persistence.criteria.CriteriaBuilder)}.</p>
 *
 * <p>This annotation-driven approach eliminates the need to manually build search specifications for
 * each entity, providing a declarative way to define which fields should be searchable.</p>
 *
 * <h2>Supported Types</h2>
 * <p>Currently, only {@code String} fields and methods are supported. Other types are silently ignored.</p>
 *
 * <h2>Target Elements</h2>
 * <ul>
 *   <li><strong>FIELD:</strong> Applied directly to entity fields</li>
 *   <li><strong>METHOD:</strong> Applied to getter methods (useful with Lombok)</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Field Annotation</h3>
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
 * <h3>Using with SearchableRepository</h3>
 * <pre>{@code
 * @Repository
 * public interface PersonRepository extends SearchableRepository<Person, UUID> {
 *     // Inherits search() method
 * }
 *
 * @Service
 * public class PersonService {
 *
 *     @Autowired
 *     private PersonRepository personRepository;
 *
 *     public PageDto<PersonDto> search(String query, PageSpec pageSpec) {
 *         Pageable pageable = Pageables.pageableOf(pageSpec, PersonSort.class);
 *
 *         // Automatically searches across all @Searchable fields
 *         Page<Person> page = personRepository.search(query, pageable);
 *
 *         return Pages.pageDtoOf(page.map(PersonDto::fromEntity));
 *     }
 * }
 *
 * // Example: search("john") will match:
 * //   - firstName contains "john"
 * //   - lastName contains "john"
 * //   - email contains "john"
 * }</pre>
 *
 * <h3>Manual Usage with Searchables Utility</h3>
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
 * <h3>Using on Getter Methods (with Lombok)</h3>
 * <pre>{@code
 * @Entity
 * @Getter
 * public class Article extends BaseEntity {
 *
 *     @Searchable
 *     private String title;
 *
 *     @Searchable
 *     private String author;
 *
 *     private String content;  // NOT searchable
 *
 *     // Lombok generates getters, @Searchable is on the field
 * }
 * }</pre>
 *
 * <h2>Search Behavior</h2>
 * <ul>
 *   <li><strong>Case-Insensitive:</strong> All searches are case-insensitive</li>
 *   <li><strong>Partial Match:</strong> Uses SQL LIKE with wildcards (e.g., {@code %john%})</li>
 *   <li><strong>OR Logic:</strong> Matches if the search term appears in ANY searchable field</li>
 *   <li><strong>Wildcard Escaping:</strong> Special characters ({@code %}, {@code _}, {@code \}) are escaped</li>
 *   <li><strong>Null-Safe:</strong> Null or blank search terms return all results</li>
 * </ul>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Mark fields that users would naturally want to search</li>
 *   <li>Avoid marking fields with sensitive information</li>
 *   <li>Don't mark too many fields (can impact query performance)</li>
 *   <li>Consider using specific query methods for exact matches (e.g., SKU, email)</li>
 *   <li>Test search performance with realistic data volumes</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Only works with String fields/methods</li>
 *   <li>Does not support searching across relationships (use custom specifications for that)</li>
 *   <li>Uses simple LIKE queries (consider full-text search for advanced use cases)</li>
 * </ul>
 *
 * @see Searchables
 * @see org.jwcarman.jpa.spring.search.SearchableRepository
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Searchable {
}
