package org.jwcarman.jpa.pagination;

/**
 * Functional interface for providing JPA property paths from sort enum constants.
 *
 * <p>This interface is the foundation of type-safe, enum-based sorting in this library. By implementing
 * this interface, your sort enums can map user-friendly constant names to actual JPA entity property paths,
 * including nested properties using dot notation.</p>
 *
 * <h2>Design Benefits</h2>
 * <ul>
 *   <li><strong>Type Safety:</strong> Compile-time checking of sort field names</li>
 *   <li><strong>IDE Support:</strong> Autocomplete and refactoring for sort fields</li>
 *   <li><strong>Flexibility:</strong> Map enum names to different property paths</li>
 *   <li><strong>Nested Properties:</strong> Support for dot notation (e.g., "address.city")</li>
 *   <li><strong>Documentation:</strong> Self-documenting sort options through enum constants</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Simple Properties</h3>
 * <pre>{@code
 * public enum PersonSort implements SortPropertyProvider {
 *     FIRST_NAME("firstName"),
 *     LAST_NAME("lastName"),
 *     EMAIL("email");
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
 * <h3>Nested Properties</h3>
 * <pre>{@code
 * public enum OrderSort implements SortPropertyProvider {
 *     ORDER_NUMBER("orderNumber"),
 *     TOTAL("total"),
 *     CUSTOMER_NAME("customer.name"),           // Nested property
 *     CUSTOMER_EMAIL("customer.email"),         // Nested property
 *     SHIPPING_ADDRESS_CITY("shippingAddress.city"),  // Deeply nested
 *     CREATED_DATE("createdDate");
 *
 *     private final String sortProperty;
 *
 *     OrderSort(String sortProperty) {
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
 * <h3>Properties with Different Naming</h3>
 * <pre>{@code
 * public enum ProductSort implements SortPropertyProvider {
 *     NAME("name"),
 *     PRICE("price"),
 *     // Enum constant name differs from property name for better API clarity
 *     CREATED("createdDate"),
 *     MODIFIED("modifiedDate"),
 *     CATEGORY("category.name");
 *
 *     private final String sortProperty;
 *
 *     ProductSort(String sortProperty) {
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
 * <h3>Integration with Pageables</h3>
 * <pre>{@code
 * @Service
 * public class PersonService {
 *
 *     @Autowired
 *     private PersonRepository personRepository;
 *
 *     public PageDto<PersonDto> findAll(PageSpec pageSpec) {
 *         // Pageables automatically resolves sort field string to PersonSort enum
 *         // Then calls getSortProperty() to get the actual JPA property path
 *         Pageable pageable = Pageables.pageableOf(pageSpec, PersonSort.class);
 *
 *         Page<Person> page = personRepository.findAll(pageable);
 *         return Pages.pageDtoOf(page.map(PersonDto::fromEntity));
 *     }
 * }
 *
 * // When user requests: ?sortBy=LAST_NAME
 * // 1. Pageables resolves "LAST_NAME" to PersonSort.LAST_NAME
 * // 2. Calls PersonSort.LAST_NAME.getSortProperty() → returns "lastName"
 * // 3. Creates Sort.by(Direction.ASC, "lastName")
 * }</pre>
 *
 * <h3>Using with Records (Compact Syntax)</h3>
 * <pre>{@code
 * public enum ArticleSort implements SortPropertyProvider {
 *     TITLE("title"),
 *     AUTHOR("author"),
 *     PUBLISHED_DATE("publishedDate");
 *
 *     private final String sortProperty;
 *
 *     ArticleSort(String sortProperty) {
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
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Use UPPER_SNAKE_CASE for enum constant names (Java convention)</li>
 *   <li>Use camelCase for JPA property paths (JPA convention)</li>
 *   <li>Document any non-obvious property mappings</li>
 *   <li>Keep sort options focused on commonly needed fields</li>
 *   <li>Consider adding JavaDoc to enum constants for complex sorts</li>
 * </ul>
 *
 * @see PageSpec
 * @see org.jwcarman.jpa.spring.page.Pageables
 * @see SortDirection
 */
@FunctionalInterface
public interface SortPropertyProvider {

    /**
     * Returns the JPA entity property path for sorting.
     *
     * <p>This method provides the actual property name or path used in JPA queries.
     * The path can be a simple property name or a nested property using dot notation.</p>
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li>Simple property: {@code "firstName"}</li>
     *   <li>Simple property: {@code "price"}</li>
     *   <li>Nested property: {@code "address.city"}</li>
     *   <li>Deeply nested: {@code "order.customer.address.zipCode"}</li>
     * </ul>
     *
     * @return the JPA property path for sorting, never null
     */
    String getSortProperty();
}
