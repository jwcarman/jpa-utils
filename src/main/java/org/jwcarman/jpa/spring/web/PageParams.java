package org.jwcarman.jpa.spring.web;

import org.jwcarman.jpa.pagination.PageSpec;
import org.jwcarman.jpa.pagination.SortDirection;
import org.springframework.lang.Nullable;

/**
 * Spring Web query parameter binding class for pagination requests.
 *
 * <p>This class implements {@link PageSpec} and is designed to automatically bind query parameters
 * to pagination settings in Spring REST controllers. All fields are optional, allowing flexible
 * pagination requests. Simply add as a method parameter - Spring will automatically bind the
 * query parameters.</p>
 *
 * <h2>Usage in REST Controllers</h2>
 *
 * <h3>Basic Usage</h3>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/persons")
 * public class PersonController {
 *
 *     @Autowired
 *     private PersonService personService;
 *
 *     @GetMapping
 *     public PageDto<PersonDto> getPersons(PageParams pageParams) {
 *         // Spring automatically binds query parameters to pageParams
 *         return personService.findAll(pageParams);
 *     }
 * }
 *
 * // Example URLs:
 * // GET /api/persons?pageIndex=0&pageSize=10&sortBy=LAST_NAME&sortDirection=ASC
 * // GET /api/persons?pageIndex=1&pageSize=25&sortBy=CREATED&sortDirection=DESC
 * // GET /api/persons (all parameters optional, uses defaults)
 * }</pre>
 *
 * <h3>With Search Parameters</h3>
 * <pre>{@code
 * @GetMapping
 * public PageDto<PersonDto> searchPersons(
 *         @RequestParam(required = false) String query,
 *         PageParams pageParams) {
 *     return personService.search(query, pageParams);
 * }
 *
 * // Example URLs:
 * // GET /api/persons?query=john&pageIndex=0&pageSize=20&sortBy=LAST_NAME&sortDirection=ASC
 * // GET /api/persons?query=smith&pageSize=10
 * }</pre>
 *
 * <h3>Service Layer Integration</h3>
 * <pre>{@code
 * @Service
 * public class PersonService {
 *
 *     @Autowired
 *     private PersonRepository personRepository;
 *
 *     public PageDto<PersonDto> findAll(PageSpec pageSpec) {
 *         // Pageables automatically resolves sort field
 *         Pageable pageable = Pageables.pageableOf(pageSpec, PersonSort.class);
 *         Page<Person> page = personRepository.findAll(pageable);
 *         return Pages.pageDtoOf(page.map(PersonDto::fromEntity));
 *     }
 * }
 * }</pre>
 *
 * <h2>Query Parameter Mapping</h2>
 * <p>The following query parameters are automatically bound:</p>
 * <ul>
 *   <li>{@code pageIndex} - Zero-based page number (e.g., 0, 1, 2...)</li>
 *   <li>{@code pageSize} - Number of items per page (e.g., 10, 20, 50...)</li>
 *   <li>{@code sortBy} - Sort field name as string (e.g., "LAST_NAME", "CREATED")</li>
 *   <li>{@code sortDirection} - Sort direction: "ASC" or "DESC"</li>
 * </ul>
 *
 * <h2>Testing</h2>
 * <p>Create instances using the canonical constructor:</p>
 * <pre>{@code
 * @Test
 * void shouldPaginateResults() {
 *     PageParams params = new PageParams(0, 25, "LAST_NAME", SortDirection.DESC);
 *
 *     PageDto<PersonDto> result = personService.findAll(params);
 *
 *     assertThat(result.pagination().pageIndex()).isZero();
 *     assertThat(result.pagination().pageSize()).isEqualTo(25);
 * }
 *
 * @Test
 * void shouldUseDefaults() {
 *     PageParams params = new PageParams(null, null, null, null);
 *
 *     PageDto<PersonDto> result = personService.findAll(params);
 *
 *     // Uses Pageables defaults (page 0, size 20, unsorted)
 *     assertThat(result.pagination().pageIndex()).isZero();
 *     assertThat(result.pagination().pageSize()).isEqualTo(20);
 * }
 * }</pre>
 *
 * <h2>Custom Parameter Names</h2>
 * <p>If you need different URL parameter names (e.g., {@code page} instead of {@code pageIndex}),
 * use individual {@code @RequestParam} annotations and build the {@code PageParams} manually:</p>
 * <pre>{@code
 * @GetMapping
 * public PageDto<PersonDto> getPersons(
 *         @RequestParam(required = false) Integer page,
 *         @RequestParam(required = false) Integer size,
 *         @RequestParam(required = false) String sortBy,
 *         @RequestParam(required = false) SortDirection sortDirection) {
 *
 *     PageParams params = new PageParams(page, size, sortBy, sortDirection);
 *
 *     return personService.findAll(params);
 * }
 *
 * // Example URLs:
 * // GET /api/persons?page=0&size=10&sortBy=LAST_NAME&sortDirection=ASC
 * }</pre>
 *
 * @see PageSpec
 * @param pageIndex      the zero-based page number, may be null (defaults to 0)
 * @param pageSize       the number of items per page, may be null (defaults to 20)
 * @param sortBy         the sort field name as string (e.g., "LAST_NAME"), may be null for unsorted
 * @param sortDirection  the sort direction (ASC or DESC), may be null (defaults to ASC)
 * @see org.jwcarman.jpa.spring.page.Pageables
 * @see org.jwcarman.jpa.spring.page.Pages
 */
public record PageParams(
    @Nullable Integer pageIndex,
    @Nullable Integer pageSize,
    @Nullable String sortBy,
    @Nullable SortDirection sortDirection
) implements PageSpec {
}
