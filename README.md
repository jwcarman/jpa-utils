# JPA Utilities
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
![Maven Central Version](https://img.shields.io/maven-central/v/org.jwcarman.jpa/jpa-utils)

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
- [Complete Example](#complete-example)
- [Requirements](#requirements)
- [License](#license)

## Features

- **🔑 UUID-based Entity IDs**: Stable, time-based UUID v7 identifiers with optimistic locking
- **📄 Framework-Agnostic Pagination**: Clean architecture design decoupling web layer from domain
- **🔍 Annotation-Driven Search**: Mark fields as `@Searchable` for automatic LIKE query generation
- **🕒 Auditing Support**: Automatic tracking of creation/modification timestamps and users
- **🌱 Spring Integration**: Optional utilities for Spring Data JPA and Spring Web
- **🎯 Type-Safe Sorting**: Enum-based sort field definitions with automatic resolution
- **✨ Clean Architecture**: Proper dependency direction (web → app → domain)

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.jwcarman.jpa</groupId>
    <artifactId>jpa-utils</artifactId>
    <version>1.0-SNAPSHOT</version>
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

    @Autowired
    private PersonRepository personRepository;

    public PageDto<PersonDto> search(String query, PageSpec pageSpec) {
        Pageable pageable = Pageables.pageableOf(pageSpec, PersonSort.class);
        Page<Person> page = personRepository.search(query, pageable);
        return Pages.pageDtoOf(page.map(PersonDto::fromEntity));
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
- **Final equals/hashCode**: Based on UUID for stable behavior

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

#### Converting with `Pageables` and `Pages`

**`Pageables`** - Converts `PageSpec` to Spring's `Pageable`:

```java
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public PageDto<ProductDto> findAll(PageSpec pageSpec) {
        // Automatically resolves sort field string to ProductSort enum
        Pageable pageable = Pageables.pageableOf(pageSpec, ProductSort.class);

        // Or with custom default page size
        // Pageable pageable = Pageables.pageableOf(pageSpec, ProductSort.class, 50);

        Page<Product> page = productRepository.findAll(pageable);
        return Pages.pageDtoOf(page.map(ProductDto::fromEntity));
    }
}
```

**Default Values:**
- Page index: 0 (first page)
- Page size: 20
- Sort: unsorted
- Sort direction: ASC

**`Pages`** - Converts Spring's `Page` to framework-agnostic `PageDto`:

```java
Page<Product> springPage = productRepository.findAll(pageable);
PageDto<ProductDto> pageDto = Pages.pageDtoOf(
    springPage.map(product -> new ProductDto(product))
);
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

    @Autowired
    private ProductRepository productRepository;

    public PageDto<ProductDto> search(String query, PageSpec pageSpec) {
        Pageable pageable = Pageables.pageableOf(pageSpec, ProductSort.class);
        Page<Product> page = productRepository.search(query, pageable);
        return Pages.pageDtoOf(page.map(ProductDto::fromEntity));
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

    @Autowired
    private BookRepository bookRepository;

    public PageDto<BookDto> search(String query, PageSpec pageSpec) {
        Pageable pageable = Pageables.pageableOf(pageSpec, BookSort.class);
        Page<Book> page = bookRepository.search(query, pageable);
        return Pages.pageDtoOf(page.map(BookDto::fromEntity));
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

- **Java**: 21 or higher
- **Jakarta Persistence API**: 3.1.0
- **Spring Framework** (optional): 6.2.2 or higher
- **Spring Data JPA** (optional): 2024.1.2 or higher

## Database Compatibility

This library is thoroughly tested against multiple database platforms using [Testcontainers](https://testcontainers.com/) to ensure compatibility across different SQL dialects and implementations.

**Tested Databases:**
- ✅ **PostgreSQL 17** - Open source, standards-compliant
- ✅ **MySQL 9.1** - Most widely deployed open source database
- ✅ **MariaDB 11.6** - MySQL fork with enhancements
- ✅ **CockroachDB v24.3** - Distributed SQL, PostgreSQL-compatible
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
