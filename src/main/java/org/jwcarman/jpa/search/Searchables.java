package org.jwcarman.jpa.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for building JPA Criteria API predicates based on fields marked with {@link Searchable}.
 *
 * <p>This utility automatically generates case-insensitive LIKE predicates for all String fields
 * or methods annotated with {@code @Searchable}, using OR logic to match across multiple fields.
 * It handles SQL wildcard escaping and provides null-safe operation.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Annotation-Driven:</strong> Automatically discovers {@code @Searchable} fields</li>
 *   <li><strong>Case-Insensitive:</strong> All searches use lowercase comparison</li>
 *   <li><strong>Wildcard Escaping:</strong> Automatically escapes {@code %}, {@code _}, and {@code \}</li>
 *   <li><strong>OR Logic:</strong> Matches if search term appears in ANY searchable field</li>
 *   <li><strong>Null-Safe:</strong> Returns all results when search term is null or blank</li>
 *   <li><strong>Type-Safe:</strong> Only processes String fields</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>With SearchableRepository (Recommended)</h3>
 * <pre>{@code
 * @Repository
 * public interface PersonRepository extends SearchableRepository<Person, UUID> {
 *     // search() method is inherited
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
 *         Page<Person> page = personRepository.search(query, pageable);
 *         return Pages.pageDtoOf(page.map(PersonDto::fromEntity));
 *     }
 * }
 * }</pre>
 *
 * <h3>Manual Usage with JpaSpecificationExecutor</h3>
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
 * <p>Special SQL wildcard characters are automatically escaped:</p>
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
 * <pre>{@code
 * // If entity has no @Searchable fields
 * Predicate predicate = Searchables.createSearchPredicate("query", root, builder);
 * // Result: builder.conjunction() (always true, returns all results)
 * }</pre>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Database Indexes:</strong> LIKE queries with leading wildcards ({@code %term%}) cannot use indexes efficiently</li>
 *   <li><strong>Field Count:</strong> More {@code @Searchable} fields = more OR conditions = slower queries</li>
 *   <li><strong>Full-Text Search:</strong> For large text fields or complex search requirements, consider database-specific full-text search</li>
 *   <li><strong>Pagination:</strong> Always use pagination to limit result sets</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Only supports String fields/methods</li>
 *   <li>Does not search across entity relationships</li>
 *   <li>Uses simple LIKE queries (not full-text search)</li>
 *   <li>Leading wildcard ({@code %term}) prevents index usage</li>
 * </ul>
 *
 * @see Searchable
 * @see org.jwcarman.jpa.spring.search.SearchableRepository
 */
@UtilityClass
@Slf4j
public class Searchables {

// -------------------------- STATIC FIELDS --------------------------

    /**
     * Cache of searchable attributes per entity type to avoid repeated reflection operations.
     * Key: Entity Class, Value: List of searchable String attribute names
     */
    private static final ConcurrentHashMap<Class<?>, List<String>> SEARCHABLE_ATTRIBUTES_CACHE = new ConcurrentHashMap<>();

// -------------------------- STATIC METHODS --------------------------

    /**
     * Creates a JPA Criteria API predicate that searches all {@code @Searchable} String fields.
     *
     * <p>This method inspects the entity's metamodel to find all String attributes marked with
     * {@link Searchable}, then builds a predicate using OR logic with case-insensitive LIKE queries.</p>
     *
     * <p><strong>Behavior:</strong></p>
     * <ul>
     *   <li>If {@code searchTerm} is null or blank → returns {@code builder.conjunction()} (all results)</li>
     *   <li>If no {@code @Searchable} fields exist → returns {@code builder.conjunction()} (all results)</li>
     *   <li>Otherwise → returns OR predicate matching any searchable field</li>
     * </ul>
     *
     * <p><strong>Search Pattern:</strong></p>
     * <pre>{@code
     * // Input: "john"
     * // Pattern: "%john%"
     * // SQL: WHERE LOWER(firstName) LIKE '%john%' ESCAPE '\'
     * //         OR LOWER(lastName) LIKE '%john%' ESCAPE '\'
     * //         OR LOWER(email) LIKE '%john%' ESCAPE '\'
     * }</pre>
     *
     * @param searchTerm the search term to match (case-insensitive, wildcards escaped), may be null or blank
     * @param root       the query root representing the entity, must not be null
     * @param builder    the criteria builder for constructing predicates, must not be null
     * @return a predicate matching the search term across all searchable fields, never null
     * @throws NullPointerException if {@code root} or {@code builder} is null
     */
    public static Predicate createSearchPredicate(String searchTerm, Root<?> root, CriteriaBuilder builder) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return builder.conjunction(); // No filtering if searchTerm is empty/null
        }

        final String pattern = "%" + sanitizeSearchTerm(searchTerm).toLowerCase() + "%";

        final var attributeNames = getSearchableAttributeNames(root.getJavaType(), root.getModel());

        if (attributeNames.isEmpty()) {
            return builder.conjunction();
        } else {
            return builder.or(attributeNames.stream()
                    .map(attributeName -> like(root, builder, attributeName, pattern))
                    .toArray(Predicate[]::new));
        }
    }

    /**
     * Gets the names of searchable attributes for the given entity type, using a cache to avoid repeated reflection.
     *
     * @param entityClass the entity class to use as cache key
     * @param entityType the entity type to inspect
     * @return list of searchable String attribute names (may be empty but never null)
     */
    private static List<String> getSearchableAttributeNames(Class<?> entityClass, EntityType<?> entityType) {
        return SEARCHABLE_ATTRIBUTES_CACHE.computeIfAbsent(entityClass, clazz ->
                {
                    List<String> searchableAttributeNames = entityType.getSingularAttributes().stream()
                            .filter(attribute -> String.class.equals(attribute.getJavaType()))
                            .filter(Searchables::isSearchable)
                            .map(SingularAttribute::getName)
                            .toList();
                    if (searchableAttributeNames.isEmpty()) {
                        log.warn("No searchable attributes found for {}", entityType.getName());
                    }
                    return searchableAttributeNames;
                }
        );
    }

    private static Predicate like(Root<?> root, CriteriaBuilder builder, String attributeName, String pattern) {
        final var path = root.get(attributeName).as(String.class);
        final var lower = builder.lower(path);
        return builder.like(lower, pattern, '\\');
    }

    private static boolean isSearchable(SingularAttribute<?, ?> attribute) {
        return switch (attribute.getJavaMember()) {
            case Method m -> m.isAnnotationPresent(Searchable.class);
            case Field f -> f.isAnnotationPresent(Searchable.class);
            default -> false;
        };
    }

    private static String sanitizeSearchTerm(String searchTerm) {
        return searchTerm
                .replace("\\", "\\\\")  // Escape backslash
                .replace("%", "\\%")    // Escape wildcard '%'
                .replace("_", "\\_");   // Escape wildcard '_'
    }
}
