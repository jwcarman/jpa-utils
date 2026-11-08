# Web Layer

## Binding requests with PageParams

`PageParams` is a record implementing `PageSpec`. Spring MVC binds its components from query parameters, all
optional:

```java
@GetMapping("/api/products")
public PageDto<ProductDto> list(PageParams page) {
    return products.list(page);
}
```

```
GET /api/products
GET /api/products?pageIndex=2&pageSize=50
GET /api/products?sortBy=PRICE&sortDirection=DESC
GET /api/products?pageIndex=0&pageSize=25&sortBy=NAME&sortDirection=ASC
```

The controller passes it on as a `PageSpec`, so services don't depend on Spring MVC.

### Different parameter names

Bind your own names and build a `PageParams`:

```java
@GetMapping("/api/products")
public PageDto<ProductDto> list(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) SortDirection order) {
    return products.list(new PageParams(page, size, sort, order));
}
```

## The response

`PageDto` serializes as the page's content plus its position:

```json
{
  "data": [ { "id": "019a6b0c-3c1e-7a51-9d2e-5b0f4c8e2a17", "name": "Widget", "price": 9.99 } ],
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

## Invalid sort values

A `sortBy` that names no constant of the sort enum throws `UnknownSortByValueException`, which carries the value
sent (`getProvidedValue()`) and the accepted ones (`getExpectedValues()`). It is the client's mistake, so map it
to 400:

```java
@RestControllerAdvice
public class PagingAdvice {

    @ExceptionHandler(UnknownSortByValueException.class)
    public ProblemDetail unknownSortBy(UnknownSortByValueException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setProperty("providedValue", e.getProvidedValue());
        problem.setProperty("expectedValues", e.getExpectedValues());
        return problem;
    }
}
```

`GET /api/products?sortBy=COLOR` then returns:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Unknown sort by value \"COLOR\", expecting one of NAME, PRICE, CREATED, CATEGORY.",
  "providedValue": "COLOR",
  "expectedValues": ["NAME", "PRICE", "CREATED", "CATEGORY"]
}
```

Matching is case-sensitive (`sortBy=name` is rejected); surrounding whitespace is ignored, and a blank `sortBy`
counts as absent, so it gets the default sort rather than a 400.

## Page size limits

Every paginator clamps the requested page size to its maximum, 100 unless configured, so a client can't ask for a
million rows in one request. Set the maximum per listing:

```java
private static final Paginator EXPORT_ROWS = Paginator.of(Row.class, RowSort.class, cfg -> cfg.maxPageSize(1000));
```

To reject an oversized request instead of clamping it, validate the parameter in the controller:

```java
@GetMapping("/api/products")
public PageDto<ProductDto> list(@RequestParam(required = false) @Max(100) Integer pageSize, PageParams page) {
    return products.list(page);
}
```
