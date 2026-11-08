# Paging and Sorting

## The contracts

A listing endpoint takes a `PageSpec` and returns a `PageDto`. Both are in `org.jwcarman.jpa.pagination` and
depend on nothing but the JDK, so services and their callers can use them without Spring.

```java
public interface PageSpec {
    Integer pageIndex();           // zero-based; null means the first page
    Integer pageSize();            // null means the paginator's default
    String sortBy();               // a sort enum constant's name, e.g. "LAST_NAME"; null or blank means the default
    SortDirection sortDirection(); // ASC or DESC; null means ASC
}

public record PageDto<T>(List<T> data, PaginationDto pagination) { }

public record PaginationDto(int pageIndex, int pageSize, long totalElementCount,
                            long totalPageCount, boolean hasNext, boolean hasPrevious) { }
```

Every `PageSpec` method may return `null`; a missing value takes its default.

## Sort enums

The values a client may send as `sortBy` are the constant names of an enum implementing `SortPropertyProvider`.
Each constant names the JPA property it sorts by, which may be a nested path:

```java
public enum ProductSort implements SortPropertyProvider {
    NAME("name"),
    PRICE("price"),
    CREATED("createdDate"),
    CATEGORY("category.name");

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

The enum keeps the public API (constant names) apart from the schema (property paths): a property can be renamed
without breaking a client, and a client can't sort by a property you didn't list.

## Paginator

A `Paginator` is built once for an entity type and its sort enum. It is immutable and thread-safe, so a
`static final` constant is the usual place for one.

```java
private static final Paginator PRODUCTS = Paginator.of(Product.class, ProductSort.class);
```

The entity type is used to [validate](property-validation.md) the sort properties when the paginator is built.

### Configuration

Everything is optional; pass a customizer to change the defaults:

```java
private static final Paginator PRODUCTS = Paginator.of(Product.class, ProductSort.class, cfg -> cfg
        .defaultSort(ProductSort.NAME)   // when sortBy is absent or blank
        .defaultPageSize(25)             // when pageSize is absent (default 20)
        .maxPageSize(200)                // larger requests are clamped (default 100)
        .tiebreaker("sku"));             // the unique final sort key (default "id")
```

| Setting | Default | Notes |
|---------|---------|-------|
| `defaultSort(S)` | none | Applies when `sortBy` is absent or blank; the request's direction still applies |
| `defaultPageSize(int)` | 20 | Must be between 1 and the maximum |
| `maxPageSize(int)` | 100 | Requested sizes above it are clamped, as are sizes below 1 |
| `tiebreaker(String...)` | `"id"` | See [Stable Paging](stable-paging.md) |
| `noTiebreaker()` | | Disables the tiebreaker |
| `skipPropertyValidation()` | | See [Property Validation](property-validation.md) |

Settings are checked together when the paginator is built, so an inconsistent configuration (a default page size
above the maximum, say) fails then, not on a request.

### How a request becomes a Pageable

| Request | Result |
|---------|--------|
| `pageIndex` absent or negative | page 0 |
| `pageSize` absent | the default page size |
| `pageSize` below 1 or above the maximum | clamped into range |
| `sortBy` a constant name | that constant's property, then the tiebreaker |
| `sortBy` absent or blank, default sort set | the default sort's property, then the tiebreaker |
| `sortBy` absent or blank, no default sort | the tiebreaker alone |
| `sortBy` names no constant | `UnknownSortByValueException` |
| `sortDirection` absent | ascending |
| the whole spec `null` | as if every value were absent |

`sortBy` ignores surrounding whitespace but is case-sensitive: `LAST_NAME` matches, `last_name` doesn't. The
direction applies to the sort property; the tiebreaker is always ascending.

## Running a query

`paginate` is the whole round trip: build the `Pageable`, run the query, convert the result.

```java
public PageDto<ProductDto> list(PageSpec spec) {
    return PRODUCTS.paginate(spec, products::findAll, ProductDto::of);
}

public PageDto<ProductDto> search(String query, PageSpec spec) {
    return PRODUCTS.paginate(spec, pageable -> products.search(query, pageable), ProductDto::of);
}

public PageDto<ProductDto> inCategory(Category category, PageSpec spec) {
    return PRODUCTS.paginate(spec, pageable -> products.findByCategory(category, pageable), ProductDto::of);
}
```

The query is any `Function<Pageable, Page<E>>`: derived queries, `@Query` methods, Specifications and
`findAll` all fit. Without a mapper, `paginate(spec, query)` returns the entities themselves.

When the query isn't a single call, use the two halves directly:

```java
Pageable pageable = PRODUCTS.pageableOf(spec);
Page<Product> page = products.findAll(activeOnly(), pageable);
return PRODUCTS.pageDtoOf(page, ProductDto::of);
```

`pageDtoOf(page)` without a mapper converts a page as it is.
