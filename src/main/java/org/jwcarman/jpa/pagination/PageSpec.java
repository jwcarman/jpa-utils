package org.jwcarman.jpa.pagination;

/**
 * Framework-agnostic specification for requesting a page of data with optional sorting.
 *
 * <p>This interface provides a contract for pagination requests that is independent of any specific
 * persistence framework (like Spring Data JPA). It uses simple strings for sort field names, allowing
 * the application layer to resolve these to domain-specific enums or property paths as needed.</p>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Framework Independence:</strong> No dependency on Spring or any other framework</li>
 *   <li><strong>Layer Decoupling:</strong> Web layer doesn't need to know about domain-specific enums</li>
 *   <li><strong>Null Tolerance:</strong> All methods may return null, allowing partial specifications</li>
 *   <li><strong>Immutability:</strong> Typically implemented as records or immutable classes</li>
 *   <li><strong>Clean Architecture:</strong> Maintains proper dependency direction (web → app → domain)</li>
 * </ul>
 *
 * <h2>Nullability Contract</h2>
 * <p>All methods in this interface may return {@code null}, which is handled gracefully by utilities
 * like {@code Pageables}:</p>
 * <ul>
 *   <li>{@code pageIndex() == null} → defaults to first page (0)</li>
 *   <li>{@code pageSize() == null} → defaults to configured default (typically 20)</li>
 *   <li>{@code sortBy() == null} → results are unsorted</li>
 *   <li>{@code sortDirection() == null} → defaults to ascending when sortBy is present</li>
 * </ul>
 *
 * <h2>Implementation Patterns</h2>
 *
 * <h3>Record Implementation (Recommended)</h3>
 * <pre>{@code
 * public record PageRequestDto(
 *         Integer pageIndex,
 *         Integer pageSize,
 *         String sortBy,
 *         SortDirection sortDirection) implements PageSpec {
 * }
 * }</pre>
 *
 * <h3>Class Implementation with Builder</h3>
 * <pre>{@code
 * @Builder
 * @Getter
 * public class PageRequest implements PageSpec {
 *     private final Integer pageIndex;
 *     private final Integer pageSize;
 *     private final String sortBy;
 *     private final SortDirection sortDirection;
 * }
 * }</pre>
 *
 * <h3>DTO for REST APIs</h3>
 * <pre>{@code
 * // Can be used directly with Spring MVC
 * public record PageRequestDto(
 *         @RequestParam(required = false) Integer page,
 *         @RequestParam(required = false) Integer size,
 *         @RequestParam(required = false) String sortBy,
 *         @RequestParam(required = false) SortDirection sortDirection
 * ) implements PageSpec {
 *
 *     @Override
 *     public Integer pageIndex() {
 *         return page;
 *     }
 *
 *     @Override
 *     public Integer pageSize() {
 *         return size;
 *     }
 * }
 * }</pre>
 *
 * <h2>Sort Field Resolution in Application Layer</h2>
 * <p>The application layer is responsible for resolving string sort field names to domain-specific
 * enums that implement {@link SortPropertyProvider}:</p>
 *
 * <pre>{@code
 * public enum PersonSort implements SortPropertyProvider {
 *     // Simple properties
 *     FIRST_NAME("firstName"),
 *     LAST_NAME("lastName"),
 *     EMAIL("email"),
 *
 *     // Nested properties using dot notation
 *     ADDRESS_CITY("address.city"),
 *     ADDRESS_STATE("address.state"),
 *
 *     // Properties with different naming
 *     CREATED("createdDate"),
 *     MODIFIED("modifiedDate");
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
 * <h2>Usage Examples</h2>
 *
 * <h3>Creating Page Specifications</h3>
 * <pre>{@code
 * // First page, 20 items, sorted by last name ascending
 * PageSpec spec1 = new PageRequestDto(0, 20, "LAST_NAME", SortDirection.ASC);
 *
 * // Second page, 50 items, sorted by creation date descending
 * PageSpec spec2 = new PageRequestDto(1, 50, "CREATED", SortDirection.DESC);
 *
 * // Use defaults for everything (null-safe)
 * PageSpec spec3 = new PageRequestDto(null, null, null, null);
 * // Results in: page 0, default size, unsorted
 * }</pre>
 *
 * <h3>REST Controller Integration</h3>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/persons")
 * public class PersonController {
 *
 *     @Autowired
 *     private PersonService personService;
 *
 *     @GetMapping
 *     public PageDto<PersonDto> getPersons(
 *             @RequestParam(required = false) Integer page,
 *             @RequestParam(required = false) Integer size,
 *             @RequestParam(required = false) String sortBy,
 *             @RequestParam(required = false) SortDirection sortDirection) {
 *
 *         // Create page spec from request parameters - no domain knowledge needed!
 *         PageSpec spec = new PageRequestDto(page, size, sortBy, sortDirection);
 *
 *         return personService.findAll(spec);
 *     }
 * }
 *
 * // Example requests:
 * // GET /api/persons?page=0&size=10&sortBy=LAST_NAME&sortDirection=ASC
 * // GET /api/persons?page=1&size=25&sortBy=CREATED&sortDirection=DESC
 * // GET /api/persons (uses all defaults)
 * }</pre>
 *
 * <h3>Service Layer Usage (Automatic Sort Resolution)</h3>
 * <pre>{@code
 * @Service
 * public class PersonService {
 *
 *     @Autowired
 *     private PersonRepository personRepository;
 *
 *     public PageDto<PersonDto> findAll(PageSpec pageSpec) {
 *         // Pageables automatically resolves sort field string to PersonSort enum
 *         Pageable pageable = Pageables.pageableOf(pageSpec, PersonSort.class);
 *
 *         // Query repository
 *         Page<Person> page = personRepository.findAll(pageable);
 *
 *         // Convert to DTOs and return
 *         return Pages.pageDtoOf(page.map(PersonDto::fromEntity));
 *     }
 *
 *     // If you need custom error handling:
 *     public PageDto<PersonDto> findAllWithCustomErrors(PageSpec pageSpec) {
 *         try {
 *             Pageable pageable = Pageables.pageableOf(pageSpec, PersonSort.class);
 *             Page<Person> page = personRepository.findAll(pageable);
 *             return Pages.pageDtoOf(page.map(PersonDto::fromEntity));
 *         } catch (UnknownSortByValueException e) {
 *             throw new InvalidSortFieldException("Invalid sort field: " + pageSpec.sortBy(), e);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Testing</h3>
 * <pre>{@code
 * @Test
 * void shouldCreatePageSpecForTesting() {
 *     // Create test page specs easily with strings
 *     PageSpec spec = new PageRequestDto(0, 10, "LAST_NAME", SortDirection.ASC);
 *
 *     PageDto<PersonDto> result = personService.findAll(spec);
 *
 *     assertThat(result.pagination().pageIndex()).isZero();
 *     assertThat(result.pagination().pageSize()).isEqualTo(10);
 *     assertThat(result.data()).hasSize(10);
 * }
 * }</pre>
 *
 * <h2>Benefits of String-Based Approach</h2>
 * <ul>
 *   <li><strong>Clean Architecture:</strong> Web layer has no dependency on domain enums</li>
 *   <li><strong>Flexibility:</strong> Can add/change sort fields without touching web layer</li>
 *   <li><strong>Reusability:</strong> Same PageSpec DTO can be used across different entities</li>
 *   <li><strong>Validation Where It Belongs:</strong> Application layer validates and resolves sort fields</li>
 * </ul>
 *
 * @see SortPropertyProvider
 * @see SortDirection
 * @see org.jwcarman.jpa.spring.page.Pageables
 * @see PageDto
 */
public interface PageSpec {

    /**
     * Returns the zero-based page index.
     *
     * <p>The page index indicates which page of results to retrieve, starting from 0 for the first page.
     * For example:</p>
     * <ul>
     *   <li>0 = first page (items 0-19 with size 20)</li>
     *   <li>1 = second page (items 20-39 with size 20)</li>
     *   <li>2 = third page (items 40-59 with size 20)</li>
     * </ul>
     *
     * <p><strong>Null Handling:</strong> If this method returns {@code null}, pagination utilities
     * will default to page 0 (first page).</p>
     *
     * @return the zero-based page index, or null to use the default (0)
     */
    Integer pageIndex();

    /**
     * Returns the number of items to include in each page.
     *
     * <p>The page size determines how many items are returned in a single page. Common values
     * are 10, 20, 25, 50, or 100 depending on the use case.</p>
     *
     * <p><strong>Null Handling:</strong> If this method returns {@code null}, pagination utilities
     * will use their configured default page size (typically 20).</p>
     *
     * <p><strong>Example:</strong></p>
     * <ul>
     *   <li>pageSize=10, pageIndex=0 → returns items 0-9</li>
     *   <li>pageSize=10, pageIndex=1 → returns items 10-19</li>
     *   <li>pageSize=25, pageIndex=2 → returns items 50-74</li>
     * </ul>
     *
     * @return the page size (number of items per page), or null to use the default
     */
    Integer pageSize();

    /**
     * Returns the string name of the field to sort by.
     *
     * <p>This is typically the uppercase name of a sort enum constant (e.g., "LAST_NAME", "CREATED").
     * The application layer is responsible for resolving this string to a domain-specific enum
     * that implements {@link SortPropertyProvider}.</p>
     *
     * <p><strong>Null Handling:</strong> If this method returns {@code null}, the results will be
     * unsorted (or use the database's default ordering).</p>
     *
     * <p><strong>Example Resolution Flow:</strong></p>
     * <pre>{@code
     * // 1. Web layer receives string from request: "LAST_NAME"
     * PageSpec spec = new PageRequestDto(0, 20, "LAST_NAME", SortDirection.ASC);
     *
     * // 2. Application layer resolves to enum
     * PersonSort sortEnum = PersonSort.valueOf(spec.sortBy()); // LAST_NAME
     *
     * // 3. Enum provides JPA property name
     * String property = sortEnum.getSortProperty(); // "lastName"
     * }</pre>
     *
     * @return the sort field name as a string, or null for unsorted results
     * @see SortPropertyProvider
     * @see #sortDirection()
     */
    String sortBy();

    /**
     * Returns the direction to sort the results.
     *
     * <p>This determines whether results are sorted in ascending (A-Z, 0-9, oldest-newest) or
     * descending (Z-A, 9-0, newest-oldest) order.</p>
     *
     * <p><strong>Null Handling:</strong> If this method returns {@code null} but {@link #sortBy()}
     * is non-null, pagination utilities will default to ascending order. If both are null,
     * results are unsorted.</p>
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li>{@code SortDirection.ASC} → A, B, C... or 1, 2, 3... or Jan-01, Jan-02...</li>
     *   <li>{@code SortDirection.DESC} → Z, Y, X... or 9, 8, 7... or Dec-31, Dec-30...</li>
     * </ul>
     *
     * @return the sort direction, or null to use the default (ascending)
     * @see SortDirection
     * @see #sortBy()
     */
    SortDirection sortDirection();
}
