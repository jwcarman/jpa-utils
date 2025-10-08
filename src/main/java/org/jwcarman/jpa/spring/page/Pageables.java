package org.jwcarman.jpa.spring.page;

import lombok.experimental.UtilityClass;
import org.jwcarman.jpa.pagination.PageSpec;
import org.jwcarman.jpa.pagination.SortDirection;
import org.jwcarman.jpa.pagination.SortPropertyProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Optional;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

/**
 * Utility class for converting framework-agnostic {@link PageSpec} objects to Spring Data's {@link Pageable}.
 *
 * <p>This class bridges the gap between the library's pagination abstraction and Spring Data JPA's pagination
 * mechanism. It handles conversion of page indices, page sizes, sort fields, and sort directions with sensible
 * defaults for missing values.</p>
 *
 * <p>The sort field resolution follows a clean architecture pattern: the {@link PageSpec} contains a simple
 * string sort field name (e.g., "LAST_NAME"), and the application layer resolves this to a domain-specific
 * enum implementing {@link SortPropertyProvider}. This keeps the web layer decoupled from domain logic.</p>
 *
 * <h2>Default Behavior</h2>
 * <ul>
 *   <li>Default page index: {@value #FIRST_PAGE} (first page)</li>
 *   <li>Default page size: {@value #DEFAULT_PAGE_SIZE}</li>
 *   <li>Default sort: {@link Sort#unsorted()}</li>
 *   <li>Default sort direction: {@link Sort#DEFAULT_DIRECTION} (ascending)</li>
 * </ul>
 *
 * <h2>Null Handling</h2>
 * <p>All null values are handled gracefully with sensible defaults:</p>
 * <ul>
 *   <li>Null spec → returns default pageable (page 0, size 20, unsorted)</li>
 *   <li>Null pageIndex → uses {@value #FIRST_PAGE}</li>
 *   <li>Null pageSize → uses specified default or {@value #DEFAULT_PAGE_SIZE}</li>
 *   <li>Null sortBy (enum) → returns unsorted</li>
 *   <li>Null sortDirection → uses {@link Sort#DEFAULT_DIRECTION}</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Pagination with Automatic Sort Resolution</h3>
 * <pre>{@code
 * // Web layer receives string sort field from request
 * PageSpec spec = new PageRequestDto(0, 10, "LAST_NAME", SortDirection.ASC);
 *
 * // Pageables automatically resolves "LAST_NAME" to PersonSort.LAST_NAME
 * Pageable pageable = Pageables.pageableOf(spec, PersonSort.class);
 *
 * // Use with Spring Data repository
 * Page<Person> results = personRepository.findAll(pageable);
 * }</pre>
 *
 * <h3>Complete Service Layer Example</h3>
 * <pre>{@code
 * @Service
 * public class PersonService {
 *     @Autowired
 *     private PersonRepository personRepository;
 *
 *     public PageDto<PersonDto> findAll(PageSpec pageSpec) {
 *         // Pageables automatically resolves and validates the sort field
 *         Pageable pageable = Pageables.pageableOf(pageSpec, PersonSort.class);
 *
 *         // Query and convert results
 *         Page<Person> page = personRepository.findAll(pageable);
 *         return Pages.pageDtoOf(page.map(PersonDto::fromEntity));
 *     }
 * }
 *
 * // If you need custom error handling for invalid sort fields:
 * @Service
 * public class PersonServiceWithCustomErrors {
 *     @Autowired
 *     private PersonRepository personRepository;
 *
 *     public PageDto<PersonDto> findAll(PageSpec pageSpec) {
 *         try {
 *             Pageable pageable = Pageables.pageableOf(pageSpec, PersonSort.class);
 *             Page<Person> page = personRepository.findAll(pageable);
 *             return Pages.pageDtoOf(page.map(PersonDto::fromEntity));
 *         } catch (IllegalArgumentException e) {
 *             throw new InvalidSortFieldException("Invalid sort field: " + pageSpec.sortBy(), e);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>With Custom Default Page Size</h3>
 * <pre>{@code
 * // If spec.pageSize() returns null, use 50 instead of default 20
 * PageSpec spec = new PageRequestDto(null, null, "LAST_NAME", SortDirection.ASC);
 * Pageable pageable = Pageables.pageableOf(spec, PersonSort.class, 50);
 * // Results in: page=0, size=50, sort=lastName: ASC
 * }</pre>
 *
 * <h3>Unsorted Results</h3>
 * <pre>{@code
 * // No sorting - sortBy is null in the spec
 * PageSpec spec = new PageRequestDto(0, 20, null, null);
 * Pageable pageable = Pageables.pageableOf(spec, PersonSort.class);
 * // Results in: page=0, size=20, unsorted
 * }</pre>
 *
 * <h3>Sort Enum Definition</h3>
 * <pre>{@code
 * public enum PersonSort implements SortPropertyProvider {
 *     FIRST_NAME("firstName"),
 *     LAST_NAME("lastName"),
 *     EMAIL("email"),
 *     ADDRESS_CITY("address.city"),  // Nested property
 *     CREATED_DATE("createdDate");
 *
 *     private final String sortProperty;
 *
 *     PersonSort(String sortProperty) {
 *         this.sortProperty = sortProperty;
 *     }
 *
 *     @Override
 *     public String getSortProperty() {
 *         return sortProperty;
 *     }
 * }
 * }</pre>
 *
 * @see PageSpec
 * @see SortPropertyProvider
 * @see Pageable
 * @see PageRequest
 */
@UtilityClass
public class Pageables {

// ------------------------------ FIELDS ------------------------------

    /**
     * The index of the first page (zero-based).
     */
    public static final int FIRST_PAGE = 0;

    /**
     * The default number of items per page when not specified.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

// -------------------------- STATIC METHODS --------------------------

    /**
     * Creates a Spring {@link Pageable} from a framework-agnostic {@link PageSpec}, automatically resolving
     * the sort field string to the specified enum type.
     *
     * <p>This method uses {@value #DEFAULT_PAGE_SIZE} as the default page size when the spec's pageSize
     * is null. If you need a different default page size, use {@link #pageableOf(PageSpec, Class, int)}.</p>
     *
     * <p>The sort field resolution works as follows:</p>
     * <ul>
     *   <li>If {@code spec.sortBy()} is null → returns unsorted</li>
     *   <li>Otherwise, attempts to resolve the string to an enum constant via {@code Enum.valueOf()}</li>
     *   <li>If resolution fails (invalid field name) → throws {@link IllegalArgumentException}</li>
     * </ul>
     *
     * <p><strong>Null Handling:</strong></p>
     * <ul>
     *   <li>If {@code spec} is null → returns pageable with page {@value #FIRST_PAGE}, size {@value #DEFAULT_PAGE_SIZE}, unsorted</li>
     *   <li>If {@code spec.pageIndex()} is null → uses {@value #FIRST_PAGE}</li>
     *   <li>If {@code spec.pageSize()} is null → uses {@value #DEFAULT_PAGE_SIZE}</li>
     *   <li>If {@code spec.sortBy()} is null → returns unsorted</li>
     *   <li>If {@code spec.sortDirection()} is null → uses {@link Sort#DEFAULT_DIRECTION}</li>
     *   <li>If {@code sortEnumClass} is null → returns unsorted</li>
     * </ul>
     *
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * // Web layer provides string sort field
     * PageSpec spec = new PageRequestDto(2, 25, "LAST_NAME", SortDirection.DESC);
     *
     * // Pageables automatically resolves "LAST_NAME" to PersonSort.LAST_NAME
     * Pageable pageable = Pageables.pageableOf(spec, PersonSort.class);
     * // Results in: page=2, size=25, sort=lastName: DESC
     * }</pre>
     *
     * @param spec          the page specification, may be null
     * @param sortEnumClass the enum class to resolve sort field names to, may be null for unsorted
     * @param <S>           the sort enum type that implements {@link SortPropertyProvider}
     * @return a Spring {@link Pageable} instance, never null
     * @throws IllegalArgumentException if spec.sortBy() cannot be resolved to a valid enum constant
     * @see #pageableOf(PageSpec, Class, int)
     */
    public static <S extends Enum<S> & SortPropertyProvider> Pageable pageableOf(PageSpec spec, Class<S> sortEnumClass) {
        return pageableOf(spec, sortEnumClass, DEFAULT_PAGE_SIZE);
    }

    /**
     * Creates a Spring {@link Pageable} from a framework-agnostic {@link PageSpec}, automatically resolving
     * the sort field string to the specified enum type, with a custom default page size.
     *
     * <p>This method is useful when your application requires a different default page size than
     * {@value #DEFAULT_PAGE_SIZE}. The custom default is only used when the spec's pageSize is null.</p>
     *
     * <p>The sort field resolution works as follows:</p>
     * <ul>
     *   <li>If {@code spec.sortBy()} is null → returns unsorted</li>
     *   <li>Otherwise, attempts to resolve the string to an enum constant via {@code Enum.valueOf()}</li>
     *   <li>If resolution fails (invalid field name) → throws {@link IllegalArgumentException}</li>
     * </ul>
     *
     * <p><strong>Null Handling:</strong></p>
     * <ul>
     *   <li>If {@code spec} is null → returns pageable with page {@value #FIRST_PAGE}, size {@code defaultPageSize}, unsorted</li>
     *   <li>If {@code spec.pageIndex()} is null → uses {@value #FIRST_PAGE}</li>
     *   <li>If {@code spec.pageSize()} is null → uses {@code defaultPageSize}</li>
     *   <li>If {@code spec.sortBy()} is null → returns unsorted</li>
     *   <li>If {@code spec.sortDirection()} is null → uses {@link Sort#DEFAULT_DIRECTION}</li>
     *   <li>If {@code sortEnumClass} is null → returns unsorted</li>
     * </ul>
     *
     * <p><strong>Examples:</strong></p>
     * <pre>{@code
     * // Example 1: Spec with null pageSize uses custom default
     * PageSpec spec1 = new PageRequestDto(0, null, "EMAIL", SortDirection.ASC);
     * Pageable pageable1 = Pageables.pageableOf(spec1, PersonSort.class, 50);
     * // Results in: page=0, size=50, sort=email: ASC
     *
     * // Example 2: Spec with explicit pageSize overrides default
     * PageSpec spec2 = new PageRequestDto(1, 100, "CREATED", SortDirection.DESC);
     * Pageable pageable2 = Pageables.pageableOf(spec2, PersonSort.class, 50);
     * // Results in: page=1, size=100, sort=createdDate: DESC (uses spec's size, not default)
     *
     * // Example 3: Null spec
     * Pageable pageable3 = Pageables.pageableOf(null, PersonSort.class, 75);
     * // Results in: page=0, size=75, unsorted
     *
     * // Example 4: Invalid sort field throws exception
     * PageSpec spec4 = new PageRequestDto(0, 20, "INVALID_FIELD", SortDirection.ASC);
     * Pageable pageable4 = Pageables.pageableOf(spec4, PersonSort.class);
     * // Throws: IllegalArgumentException: No enum constant PersonSort.INVALID_FIELD
     * }</pre>
     *
     * @param spec            the page specification, may be null
     * @param sortEnumClass   the enum class to resolve sort field names to, may be null for unsorted
     * @param defaultPageSize the default page size to use when spec is null or spec.pageSize() is null; must be positive
     * @param <S>             the sort enum type that implements {@link SortPropertyProvider}
     * @return a Spring {@link Pageable} instance, never null
     * @throws IllegalArgumentException if spec.sortBy() cannot be resolved to a valid enum constant
     * @see #pageableOf(PageSpec, Class)
     */
    public static <S extends Enum<S> & SortPropertyProvider> Pageable pageableOf(PageSpec spec, Class<S> sortEnumClass, int defaultPageSize) {
        if (spec == null) {
            return PageRequest.of(FIRST_PAGE, defaultPageSize, Sort.unsorted());
        }
        final int pageNumber = Optional.ofNullable(spec.pageIndex()).orElse(FIRST_PAGE);
        final int pageSize = Optional.ofNullable(spec.pageSize()).orElse(defaultPageSize);

        final Sort.Direction direction = sortDirectionOf(spec);

        final Sort sort = resolveSortEnum(spec.sortBy(), sortEnumClass)
                .map(SortPropertyProvider::getSortProperty)
                .map(property -> Sort.by(direction, property))
                .orElseGet(Sort::unsorted);

        return PageRequest.of(pageNumber, pageSize, sort);
    }

    private static <S extends Enum<S> & SortPropertyProvider> Optional<S> resolveSortEnum(String sortBy, Class<S> sortEnumClass) {
        if (sortBy == null || sortEnumClass == null) {
            return Optional.empty();
        }
        return Optional.of(Enum.valueOf(sortEnumClass, sortBy.toUpperCase()));
    }

    private static Sort.Direction sortDirectionOf(PageSpec spec) {
        return Optional.ofNullable(spec.sortDirection())
                .map(e -> e == SortDirection.ASC ? ASC : DESC)
                .orElse(Sort.DEFAULT_DIRECTION);
    }

}
