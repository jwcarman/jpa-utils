package org.jwcarman.jpa.spring.page;

import lombok.experimental.UtilityClass;
import org.jwcarman.jpa.pagination.PageDto;
import org.jwcarman.jpa.pagination.PaginationDto;
import org.springframework.data.domain.Page;

/**
 * Utility class for converting Spring Data's {@link Page} objects to framework-agnostic {@link PageDto}.
 *
 * <p>This class provides the inverse operation of {@link Pageables}, converting Spring Data pagination
 * results into DTOs that can be safely serialized and returned from REST APIs or other interfaces without
 * exposing Spring Data dependencies to clients.</p>
 *
 * <h2>Conversion Details</h2>
 * <p>The conversion extracts the following information from Spring's {@link Page}:</p>
 * <ul>
 *   <li><strong>Data:</strong> The actual page content via {@link Page#getContent()}</li>
 *   <li><strong>Page Index:</strong> Current page number (0-based) via {@link Page#getNumber()}</li>
 *   <li><strong>Page Size:</strong> Number of items per page via {@link Page#getSize()}</li>
 *   <li><strong>Total Elements:</strong> Total count of all items via {@link Page#getTotalElements()}</li>
 *   <li><strong>Total Pages:</strong> Total number of pages via {@link Page#getTotalPages()}</li>
 *   <li><strong>Has Next:</strong> Whether there's a next page via {@link Page#hasNext()}</li>
 *   <li><strong>Has Previous:</strong> Whether there's a previous page via {@link Page#hasPrevious()}</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Repository Query to DTO</h3>
 * <pre>{@code
 * // In a Spring service
 * public PageDto<PersonDto> findPersons(PageSpec pageSpec) {
 *     // Convert PageSpec to Pageable with automatic sort resolution
 *     Pageable pageable = Pageables.pageableOf(pageSpec, PersonSort.class);
 *
 *     // Query repository
 *     Page<Person> page = personRepository.findAll(pageable);
 *
 *     // Map entities to DTOs
 *     Page<PersonDto> dtoPage = page.map(person -> new PersonDto(
 *         person.getId(),
 *         person.getFirstName(),
 *         person.getLastName()
 *     ));
 *
 *     // Convert to framework-agnostic DTO
 *     return Pages.pageDtoOf(dtoPage);
 * }
 * }</pre>
 *
 * <h3>REST Controller Example</h3>
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
 *         // Web layer just passes strings - no domain knowledge needed
 *         PageSpec spec = new PageRequestDto(page, size, sortBy, sortDirection);
 *         return personService.findAll(spec);
 *     }
 * }
 *
 * // Example JSON response:
 * // {
 * //   "data": [
 * //     {"id": "123", "firstName": "John", "lastName": "Doe"},
 * //     {"id": "456", "firstName": "Jane", "lastName": "Smith"}
 * //   ],
 * //   "pagination": {
 * //     "pageIndex": 0,
 * //     "pageSize": 20,
 * //     "totalElementCount": 2,
 * //     "totalPageCount": 1,
 * //     "hasNext": false,
 * //     "hasPrevious": false
 * //   }
 * // }
 * }</pre>
 *
 * <h3>With Custom Specifications</h3>
 * <pre>{@code
 * public PageDto<PersonDto> searchPersons(String searchTerm, PageSpec pageSpec) {
 *     // Automatic sort resolution happens in Pageables
 *     Pageable pageable = Pageables.pageableOf(pageSpec, PersonSort.class);
 *
 *     // Use SearchableRepository with search term
 *     Page<Person> page = personRepository.findAll(
 *         Searchables.search(searchTerm),
 *         pageable
 *     );
 *
 *     return Pages.pageDtoOf(page.map(PersonDto::fromEntity));
 * }
 * }</pre>
 *
 * <h3>Empty Page Handling</h3>
 * <pre>{@code
 * Page<Person> emptyPage = Page.empty();
 * PageDto<PersonDto> dto = Pages.pageDtoOf(emptyPage);
 *
 * // Results in:
 * // - data: empty list
 * // - pageIndex: 0
 * // - pageSize: 0
 * // - totalElementCount: 0
 * // - totalPageCount: 1
 * // - hasNext: false
 * // - hasPrevious: false
 * }</pre>
 *
 * <h2>Benefits</h2>
 * <ul>
 *   <li><strong>Framework Independence:</strong> Client code doesn't depend on Spring Data</li>
 *   <li><strong>Serialization-Friendly:</strong> Simple DTOs that work with any JSON/XML library</li>
 *   <li><strong>Type Safety:</strong> Generic type parameter preserved through conversion</li>
 *   <li><strong>Complete Metadata:</strong> All pagination metadata preserved for UI pagination controls</li>
 * </ul>
 *
 * @see PageDto
 * @see PaginationDto
 * @see Pageables
 * @see Page
 */
@UtilityClass
public class Pages {

    /**
     * Converts a Spring Data {@link Page} to a framework-agnostic {@link PageDto}.
     *
     * <p>This method extracts all data and pagination metadata from the Spring Page object
     * and packages it into a simple DTO structure suitable for serialization and client consumption.</p>
     *
     * <p><strong>What Gets Converted:</strong></p>
     * <ul>
     *   <li>Page content (the actual data items)</li>
     *   <li>Current page index (0-based)</li>
     *   <li>Page size (items per page)</li>
     *   <li>Total element count (across all pages)</li>
     *   <li>Total page count</li>
     *   <li>Navigation flags (hasNext, hasPrevious)</li>
     * </ul>
     *
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * // After querying repository
     * Page<Person> springPage = personRepository.findAll(pageable);
     *
     * // Map to DTOs if needed
     * Page<PersonDto> dtoPage = springPage.map(PersonDto::fromEntity);
     *
     * // Convert to framework-agnostic DTO
     * PageDto<PersonDto> result = Pages.pageDtoOf(dtoPage);
     *
     * // Now you can return this from a REST API
     * return ResponseEntity.ok(result);
     * }</pre>
     *
     * @param page the Spring Data page to convert, must not be null
     * @param <T>  the type of elements in the page
     * @return a {@link PageDto} containing the page data and pagination metadata, never null
     * @see PageDto
     * @see PaginationDto
     */
    public static <T> PageDto<T> pageDtoOf(Page<T> page) {
        return new PageDto<>(page.getContent(), paginationDtoOf(page));
    }

    /**
     * Extracts pagination metadata from a Spring Data {@link Page} into a {@link PaginationDto}.
     *
     * <p>This is a helper method used internally by {@link #pageDtoOf(Page)} to extract
     * pagination information separate from the actual page content.</p>
     *
     * @param page the Spring Data page, must not be null
     * @param <T>  the type of elements in the page
     * @return a {@link PaginationDto} with all pagination metadata
     */
    private static <T> PaginationDto paginationDtoOf(Page<T> page) {
        return new PaginationDto(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext(), page.hasPrevious());
    }
}
