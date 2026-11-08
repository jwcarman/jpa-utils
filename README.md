# JPA Utilities

[![CI](https://github.com/jwcarman/jpa-utils/actions/workflows/maven.yml/badge.svg)](https://github.com/jwcarman/jpa-utils/actions/workflows/maven.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/dynamic/xml?url=https://raw.githubusercontent.com/jwcarman/jpa-utils/main/pom.xml&query=//*[local-name()='java.version']/text()&label=Java&color=orange)](https://openjdk.org/)
[![Maven Central](https://img.shields.io/maven-central/v/org.jwcarman.jpa/jpa-utils)](https://central.sonatype.com/artifact/org.jwcarman.jpa/jpa-utils)

[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)

A collection of utilities for building REST APIs with [Jakarta Persistence API](https://jakarta.ee/specifications/persistence/), featuring framework-agnostic pagination, annotation-driven search, and optional Spring Data JPA integration.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Core Modules](#core-modules)
  - [Base Entity](#base-entity)
  - [Pagination](#pagination)
  - [Search](#search)
  - [Auditing](#auditing)
- [Spring Integration](#spring-integration)
  - [REST Controller Integration](#rest-controller-integration)
  - [Searchable Repository](#searchable-repository)
- [Error Handling](#error-handling)
- [Security Considerations](#security-considerations)
- [Performance Optimization](#performance-optimization)
- [Complete Example](#complete-example)
- [Requirements](#requirements)
- [Database Compatibility](#database-compatibility)
- [License](#license)

## Features

- **🔑 UUID-based Entity IDs**: Stable, time-based UUID v7 identifiers with optimistic locking
- **📄 Framework-Agnostic Pagination**: Clean architecture design decoupling web layer from domain
- **🔍 Annotation-Driven Search**: Mark fields as `@Searchable` for automatic LIKE query generation
- **🕒 Auditing Support**: Automatic tracking of creation/modification timestamps and users
- **🌱 Spring Integration**: Optional utilities for Spring Data JPA and Spring Web
- **🎯 Type-Safe Sorting**: Enum-based sort field definitions with automatic resolution
- **🧱 Stable Paging**: A unique tiebreaker on every sort, so rows never repeat or vanish across pages
- **✨ Clean Architecture**: Proper dependency direction (web → app → domain)

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.jwcarman.jpa</groupId>
    <artifactId>jpa-utils</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Quick Start

### 1. Define Your Entity

```java
@Entity
public class Person extends BaseEntity {
    @Searchable
    private String firstName;

    @Searchable
    private String lastName;

    private String email;

    protected Person() {}

    public Person(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // Getters and setters...
}
```

### 2. Define Sort Fields

```java
public enum PersonSort implements SortPropertyProvider {
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    EMAIL("email");

    private final String sortProperty;

    PersonSort(String sortProperty) {
        this.sortProperty = sortProperty;
    }

    @Override
    public String getSortProperty() {
        return sortProperty;
    }
}
```

### 3. Create a Repository

```java
@Repository
public interface PersonRepository extends SearchableRepository<Person, UUID> {
}
```

### 4. Build a REST Controller

```java
@RestController
@RequestMapping("/api/persons")
public class PersonController {

    @Autowired
    private PersonService personService;

    @GetMapping
    public PageDto<PersonDto> search(
            @RequestParam(required = false) String query,
            PageParams pageParams) {
        return personService.search(query, pageParams);
    }
}
```

### 5. Implement the Service

```java
@Service
public class PersonService {

    private static final Paginator PAGINATOR = Paginator.of(Person.class, PersonSort.class);

    @Autowired
    private PersonRepository personRepository;

    public PageDto<PersonDto> search(String query, PageSpec pageSpec) {
        return PAGINATOR.paginate(pageSpec, pageable -> personRepository.search(query, pageable), PersonDto::fromEntity);
    }
}
```

**Example Request:**
```
GET /api/persons?query=john&pageIndex=0&pageSize=20&sortBy=LAST_NAME&sortDirection=ASC
```

## Core Modules

### Base Entity

`BaseEntity` provides UUID-based primary keys and optimistic locking for all entities.

#### Key Features
- **UUID v7 Identifiers**: Time-based epoch UUIDs for better database locality
- **Stable IDs**: Generated at construction time, before persistence
- **Optimistic Locking**: Built-in `@Version` field
- **Proxy-safe equals/hashCode**: Based on the UUID, read through `getId()` so Hibernate lazy proxies equal the loaded entity

#### Usage

```java
@Entity
public class Product extends BaseEntity {
    private String name;
    private BigDecimal price;

    protected Product() {}

    public Product(String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }
}
```

### Pagination

Framework-agnostic pagination contracts that maintain clean architecture principles.

#### Core Interfaces

**`PageSpec`** - Pagination specification
```java
public interface PageSpec {
    Integer pageIndex();        // Zero-based page number
    Integer pageSize();         // Items per page
    String sortBy();           // Sort field name (e.g., "LAST_NAME")
    SortDirection sortDirection(); // ASC or DESC
}
```

**`SortPropertyProvider`** - Maps sort enum constants to JPA properties
```java
public interface SortPropertyProvider {
    String getSortProperty();  // Returns JPA property path
}
```

**`PageDto`** - Response containing page data and metadata
```java
public record PageDto<T>(
    List<T> data,              // Page content
    PaginationDto pagination   // Metadata (pageIndex, pageSize, totalPages, etc.)
) {}
```

#### Example Sort Enum

```java
public enum ProductSort implements SortPropertyProvider {
    NAME("name"),
    PRICE("price"),
    CREATED_DATE("createdDate"),
    CATEGORY_NAME("category.name");  // Nested property

    private final String sortProperty;

    ProductSort(String sortProperty) {
        this.sortProperty = sortProperty;
    }

    @Override
    public String getSortProperty() {
        return sortProperty;
    }
}
```

### Search

Annotation-driven search functionality using JPA Criteria API.

#### `@Searchable` Annotation

Mark fields that should be included in search queries:

```java
@Entity
public class Article extends BaseEntity {
    @Searchable
    private String title;

    @Searchable
    private String summary;

    private String content;  // Not searchable

    // ...
}
```

#### `Searchables` Utility

Automatically builds case-insensitive LIKE predicates for all `@Searchable` String fields:

```java
// Automatically searches across title and summary fields
Specification<Article> spec = (root, query, cb) ->
    Searchables.createSearchPredicate("spring boot", root, cb);

Page<Article> results = articleRepository.findAll(spec, pageable);
```

**Features:**
- Case-insensitive LIKE queries
- Automatic SQL wildcard escaping (`%`, `_`, `\`)
- OR logic across all searchable fields
- Null-safe (returns all results when search term is null/blank)

### Auditing

`AuditableEntity` extends `BaseEntity` with automatic tracking of creation and modification metadata.

#### Fields

- `createdDate` - When the entity was created
- `modifiedDate` - When the entity was last modified
- `createdBy` - Username/identifier who created the entity
- `modifiedBy` - Username/identifier who last modified the entity

#### Usage

```java
@Entity
public class Order extends AuditableEntity {
    private BigDecimal total;

    protected Order() {}

    public Order(BigDecimal total) {
        this.total = total;
    }
}
```

#### Spring Configuration

Enable auditing and provide user information:

```java
@Configuration
@EnableJpaAuditing
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName);
    }
}
```

## Spring Integration

### REST Controller Integration

#### Using `PageParams`

`PageParams` is a record that implements `PageSpec` and automatically binds Spring Web query parameters:

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public PageDto<ProductDto> getProducts(PageParams pageParams) {
        // Spring automatically binds these query parameters:
        // - pageIndex
        // - pageSize
        // - sortBy
        // - sortDirection
        return productService.findAll(pageParams);
    }
}
```

**All parameters are optional:**
```
GET /api/products                                     # Uses defaults
GET /api/products?pageIndex=0&pageSize=10            # Pagination only
GET /api/products?sortBy=PRICE&sortDirection=DESC    # Sorting only
GET /api/products?pageIndex=1&pageSize=25&sortBy=NAME&sortDirection=ASC  # Full
```

#### Custom Parameter Names

If you need different URL parameter names:

```java
@GetMapping
public PageDto<ProductDto> getProducts(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) SortDirection sortDirection) {

    PageParams params = new PageParams(page, size, sortBy, sortDirection);
    return productService.findAll(params);
}

// URL: /api/products?page=0&size=10&sortBy=NAME&sortDirection=ASC
```

#### Paging with `Paginator`

A `Paginator` turns a `PageSpec` into Spring's `Pageable`, and a Spring `Page` back into a `PageDto`. Configure one per
entity type and sort enum and reuse it; it is immutable and thread-safe, so a `static final` constant is the usual home:

```java
@Service
public class ProductService {

    private static final Paginator PAGINATOR = Paginator.of(Product.class, ProductSort.class);

    @Autowired
    private ProductRepository productRepository;

    public PageDto<ProductDto> findAll(PageSpec pageSpec) {
        // Builds the Pageable, runs the query, and maps each entity to a DTO
        return PAGINATOR.paginate(pageSpec, productRepository::findAll, ProductDto::fromEntity);
    }
}
```

The `query` argument is any function from `Pageable` to `Page`, so `findAll`, `search`, Specifications, and custom
`@Query` methods all work. When you need the pieces separately, use them directly:

```java
Pageable pageable = PAGINATOR.pageableOf(pageSpec);
Page<Product> page = productRepository.findAll(pageable);
PageDto<ProductDto> dto = PAGINATOR.pageDtoOf(page, ProductDto::fromEntity);
```

**Configuration** (all optional):

```java
private static final Paginator PAGINATOR = Paginator.of(Product.class, ProductSort.class, cfg -> cfg
        .defaultSort(ProductSort.NAME)   // used when the request has no (or a blank) sortBy
        .defaultPageSize(25)             // default: 20
        .maxPageSize(500)                // default: 100; larger requests are clamped
        .tiebreaker("sku"));             // default: "id"
```

**Property validation:** the first argument to `Paginator.of` is the entity the queries return. When the paginator
is built, every sort enum constant's property and every tiebreaker property is checked against it (nested paths
like `address.city` included), so a typo fails at startup with a message naming the enum constant and the entity,
instead of every request failing with a `PropertyReferenceException`. If a query sorts by something other than entity
properties (for example a native query sorting by column names), opt out with `cfg -> cfg.skipPropertyValidation()`.

**Default Values:**
- Page index: 0 (first page)
- Page size: 20
- Sort: the configured default sort, if any; otherwise only the tiebreaker
- Sort direction: ASC

#### Stable Ordering (Tiebreaker)

SQL only orders rows whose sort values differ. Rows that tie (say, many products in the same category) can come back
in any order, and each page is a separate query, so without a unique final sort key a row can appear on two pages
or on none. `Paginator` prevents this by appending a unique *tiebreaker* property, ascending, after the requested sort:

```
?sortBy=CATEGORY&sortDirection=DESC   →   ORDER BY category DESC, id ASC
(no sortBy)                           →   ORDER BY id ASC
```

The default tiebreaker is `id`, the identifier of `BaseEntity`. If your entity's unique property has a different
name, configure it; for composite keys, list every property; to opt out, use `noTiebreaker()`:

```java
Paginator.of(Member.class, MemberSort.class, cfg -> cfg.tiebreaker("identityId"));
Paginator.of(LineItem.class, LineItemSort.class, cfg -> cfg.tiebreaker("orderId", "lineNo"));
Paginator.of(Event.class, EventSort.class, cfg -> cfg.noTiebreaker());
```

### Searchable Repository

`SearchableRepository` combines `JpaRepository` with built-in search support:

```java
@Repository
public interface ProductRepository extends SearchableRepository<Product, UUID> {
    // Inherits search() method automatically
}
```

**Usage in Service:**

```java
@Service
public class ProductService {

    private static final Paginator PAGINATOR = Paginator.of(Product.class, ProductSort.class);

    @Autowired
    private ProductRepository productRepository;

    public PageDto<ProductDto> search(String query, PageSpec pageSpec) {
        return PAGINATOR.paginate(pageSpec,
                pageable -> productRepository.search(query, pageable),
                ProductDto::fromEntity);
    }
}
```

**Controller:**

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public PageDto<ProductDto> search(
            @RequestParam(required = false) String query,
            PageParams pageParams) {
        return productService.search(query, pageParams);
    }
}

// Example: GET /api/products?query=laptop&pageIndex=0&pageSize=20&sortBy=PRICE&sortDirection=ASC
```

## Error Handling

### Invalid Sort Fields

When an invalid sort field is provided, `Paginator` throws `UnknownSortByValueException`. This typically happens when a user provides a sort field name that doesn't match any of your defined enum constants.

**Service Layer Handling:**

```java
@Service
public class ProductService {

    private static final Paginator PAGINATOR = Paginator.of(Product.class, ProductSort.class);

    @Autowired
    private ProductRepository productRepository;

    public PageDto<ProductDto> findAll(PageSpec pageSpec) {
        try {
            return PAGINATOR.paginate(pageSpec, productRepository::findAll, ProductDto::fromEntity);
        } catch (UnknownSortByValueException e) {
            // e.getProvidedValue() - the invalid value the user sent
            // e.getExpectedValues() - array of valid enum constant names
            throw new BadRequestException("Invalid sort field: " + e.getProvidedValue());
        }
    }
}
```

**Spring Boot Controller Advice:**

Map the exception to HTTP 400 Bad Request using RFC 7807 `ProblemDetail`:

```java
@RestControllerAdvice
public class UnknownSortByValueExceptionAdvice {

    @ExceptionHandler(UnknownSortByValueException.class)
    public ProblemDetail handleUnknownSortByValue(UnknownSortByValueException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
```

**Optional: Add custom properties for debugging:**

```java
@RestControllerAdvice
public class UnknownSortByValueExceptionAdvice {

    @ExceptionHandler(UnknownSortByValueException.class)
    public ProblemDetail handleUnknownSortByValue(UnknownSortByValueException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            e.getMessage()
        );
        problemDetail.setProperty("providedValue", e.getProvidedValue());
        problemDetail.setProperty("expectedValues", e.getExpectedValues());
        return problemDetail;
    }
}
```

**Example Error Response:**

When a user requests `GET /api/products?sortBy=INVALID_FIELD`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Unknown sort by value \"INVALID_FIELD\", expecting one of NAME, PRICE, CREATED_DATE, CATEGORY_NAME."
}
```

**With optional properties:**

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Unknown sort by value \"INVALID_FIELD\", expecting one of NAME, PRICE, CREATED_DATE, CATEGORY_NAME.",
  "providedValue": "INVALID_FIELD",
  "expectedValues": ["NAME", "PRICE", "CREATED_DATE", "CATEGORY_NAME"]
}
```

**Note:** Sort field matching is case-sensitive. If your enum constant is `LAST_NAME`, the client must send `sortBy=LAST_NAME` (not `last_name` or `lastName`).

## Security Considerations

### Search Term Length Validation

For production applications, validate search term length at the controller level to prevent excessively long LIKE patterns from impacting database performance:

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public PageDto<ProductDto> search(
            @RequestParam(required = false)
            @Size(max = 200, message = "Search term must not exceed 200 characters")
            String query,
            PageParams pageParams) {
        return productService.search(query, pageParams);
    }
}
```

Spring Boot will automatically return HTTP 400 Bad Request when the validation fails:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request content.",
  "instance": "/api/products"
}
```

**Recommended Limits:**
- **General search**: 100-200 characters (covers most legitimate search queries)
- **Long-form content search**: 500 characters (for searching articles, descriptions, etc.)
- **Never unlimited**: Always enforce some reasonable maximum

### Page Size Limits

`Paginator` clamps requested page sizes to its maximum (`Paginator.DEFAULT_MAX_PAGE_SIZE`, 100, unless configured) to bound the work a single request can cause. Raise or lower it per paginator with `cfg -> cfg.maxPageSize(n)`, or enforce limits at the controller level:

```java
@GetMapping
public PageDto<ProductDto> getProducts(
        @RequestParam(required = false)
        @Min(1)
        @Max(100)
        Integer pageSize,
        PageParams pageParams) {
    // Custom validation: max 100 items per page for this endpoint
    return productService.findAll(pageParams);
}
```

## Performance Optimization

### Query Plan Caching

The library is already optimized for query plan caching with minimal configuration needed. Hibernate automatically uses bind parameters for String literals (the default `AUTO` mode), which means search queries are already benefiting from query plan reuse.

**What's Already Optimized:**

String literals in search patterns are automatically bound as parameters:
```sql
-- All searches use the same query plan (automatic with Hibernate's AUTO mode)
WHERE LOWER(firstName) LIKE ?1 OR LOWER(lastName) LIKE ?2
-- Parameters: ?1='%john%', ?2='%john%' (or '%jane%', etc.)
```

The library also caches searchable field metadata (via reflection) internally, so field discovery only happens once per entity type.

**Optional: Full BIND Mode (Future-Proofing)**

If you plan to extend the library with numeric or date field searching, you can configure Hibernate to use bind parameters for **all** literal types:

```properties
# Use bind parameters for all literals, including numbers (default: AUTO uses bind for Strings only)
spring.jpa.properties.hibernate.criteria.literal_handling_mode=BIND

# Optional: Adjust query plan cache size (default: 2048)
spring.jpa.properties.hibernate.query.plan_cache_max_size=2048
```

**Hibernate Literal Handling Modes:**
- `AUTO` (default): Uses bind parameters for Strings, inlines numeric values
- `BIND`: Uses bind parameters for all literal types (Strings, numbers, dates)
- `INLINE`: Inlines all literals (not recommended - poor caching, potential SQL injection risk)

**Performance Impact:**
- String-based `@Searchable` searches already benefit from query plan caching (no configuration needed)
- `BIND` mode provides benefits if you add numeric or date field searching in the future
- High-traffic search endpoints will see query compilation overhead reduced by 500%+ compared to inline literals

## Complete Example

### Entity

```java
@Entity
public class Book extends AuditableEntity {

    @Searchable
    private String title;

    @Searchable
    private String author;

    private String isbn;

    @Searchable
    private String publisher;

    private BigDecimal price;

    protected Book() {}

    public Book(String title, String author, String isbn,
                String publisher, BigDecimal price) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publisher = publisher;
        this.price = price;
    }

    // Getters and setters...
}
```

### Sort Enum

```java
public enum BookSort implements SortPropertyProvider {
    TITLE("title"),
    AUTHOR("author"),
    PUBLISHER("publisher"),
    PRICE("price"),
    CREATED("createdDate");

    private final String sortProperty;

    BookSort(String sortProperty) {
        this.sortProperty = sortProperty;
    }

    @Override
    public String getSortProperty() {
        return sortProperty;
    }
}
```

### Repository

```java
@Repository
public interface BookRepository extends SearchableRepository<Book, UUID> {
}
```

### Service

```java
@Service
public class BookService {

    private static final Paginator PAGINATOR = Paginator.of(Book.class, BookSort.class);

    @Autowired
    private BookRepository bookRepository;

    public PageDto<BookDto> search(String query, PageSpec pageSpec) {
        return PAGINATOR.paginate(pageSpec, pageable -> bookRepository.search(query, pageable), BookDto::fromEntity);
    }
}
```

### Controller

```java
@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    @GetMapping
    public PageDto<BookDto> search(
            @RequestParam(required = false) String query,
            PageParams pageParams) {
        return bookService.search(query, pageParams);
    }
}
```

### Example Requests

```bash
# Get first page of all books, sorted by title
GET /api/books?pageIndex=0&pageSize=20&sortBy=TITLE&sortDirection=ASC

# Search for "spring" in title, author, or publisher
GET /api/books?query=spring&pageIndex=0&pageSize=10

# Get page 2, sorted by price descending
GET /api/books?pageIndex=1&pageSize=25&sortBy=PRICE&sortDirection=DESC

# Search with custom page size
GET /api/books?query=java&pageSize=50
```

### Example Response

```json
{
  "data": [
    {
      "id": "01234567-89ab-cdef-0123-456789abcdef",
      "title": "Spring in Action",
      "author": "Craig Walls",
      "isbn": "978-1617294945",
      "publisher": "Manning",
      "price": 44.99,
      "createdDate": "2024-01-15T10:30:00Z",
      "modifiedDate": "2024-01-15T10:30:00Z"
    }
  ],
  "pagination": {
    "pageIndex": 0,
    "pageSize": 20,
    "totalElementCount": 127,
    "totalPageCount": 7,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

## Requirements

- **Java**: 25 or higher
- **Jakarta Persistence API**: 3.2
- **Spring Framework** (optional): 7.0 or higher
- **Spring Data JPA** (optional): 2026.0 or higher (Spring Boot 4.x)

## Database Compatibility

This library is thoroughly tested against multiple database platforms using [Testcontainers](https://testcontainers.com/) to ensure compatibility across different SQL dialects and implementations.

**Tested Databases:**
- ✅ **PostgreSQL 17.6** - Open source, standards-compliant
- ✅ **MySQL 9.2** - Most widely deployed open source database
- ✅ **MariaDB 11.6** - MySQL fork with enhancements
- ✅ **CockroachDB v25.3** - Distributed SQL, PostgreSQL-compatible
- ✅ **Oracle Database Free 23** - Enterprise standard
- ✅ **H2** - Embedded database for testing

**Test Coverage:**
- 75+ integration tests across 5 database platforms
- Comprehensive validation of pagination, sorting, and search functionality
- SQL wildcard escaping and edge case handling
- Multi-field search across different SQL dialects

All integration tests run automatically in CI/CD to ensure consistent behavior across all supported databases.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
