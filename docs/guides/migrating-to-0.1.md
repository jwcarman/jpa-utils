# Migrating to 0.1.0

0.1.0 is the first minor-version release, and it breaks the 0.0.x API. Most applications change a few lines per
listing.

## Platform

0.1.0 targets Java 25, Spring Boot 4.x (Spring Framework 7, Spring Data 2026.0), Hibernate 7 and Jakarta
Persistence 3.2. Applications on Spring Boot 3.x should stay on 0.0.x.

## Pageables and Pages are replaced by Paginator

The static `Pageables` and `Pages` utilities are gone. Create one `Paginator` per listing instead:

| 0.0.x | 0.1.0 |
|-------|-------|
| `Pageables.pageableOf(spec, XSort.class)` | `Paginator.of(X.class, XSort.class).pageableOf(spec)` |
| `Pageables.pageableOf(spec, XSort.class, 50)` | `Paginator.of(X.class, XSort.class, cfg -> cfg.defaultPageSize(50)).pageableOf(spec)` |
| `Pages.pageDtoOf(page)` | `paginator.pageDtoOf(page)` |
| `Pages.pageDtoOf(page.map(XDto::of))` | `paginator.pageDtoOf(page, XDto::of)` |
| `Pageables.MAX_PAGE_SIZE` | `Paginator.DEFAULT_MAX_PAGE_SIZE` |

A typical service before and after:

```java
// 0.0.x
public PageDto<PersonDto> search(String query, PageSpec spec) {
    Pageable pageable = Pageables.pageableOf(spec, PersonSort.class);
    Page<Person> page = people.search(query, pageable);
    return Pages.pageDtoOf(page.map(PersonDto::of));
}

// 0.1.0
private static final Paginator PEOPLE = Paginator.of(Person.class, PersonSort.class);

public PageDto<PersonDto> search(String query, PageSpec spec) {
    return PEOPLE.paginate(spec, pageable -> people.search(query, pageable), PersonDto::of);
}
```

## Behavior changes

**Every sort ends with a tiebreaker.** Pageables now sort by the requested property and then `id`, and a request
without `sortBy` sorts by `id` instead of being unsorted. See [Stable Paging](stable-paging.md). If your entity's
identifier isn't named `id`, configure `tiebreaker(...)`; the paginator fails at construction until you do.

**Sort properties are validated at construction.** A sort enum constant or tiebreaker naming a property the entity
doesn't have now fails when the paginator is built, instead of on each request. Native queries that sort by column
names need `skipPropertyValidation()`. See [Property Validation](property-validation.md).

**The default maximum page size is 100** (it was 1000). Raise it per listing with `cfg -> cfg.maxPageSize(n)`.

**A blank `sortBy` counts as absent.** `?sortBy=` gets the default sort instead of `UnknownSortByValueException`.

**An out-of-range default page size is rejected** when the paginator is built, instead of being clamped.

**`BaseEntity.equals` and `hashCode` are no longer `final`.** They now read `getId()`, which fixes equality with
Hibernate lazy proxies. Subclasses still shouldn't override them.

## Checklist

1. Move to Java 25 and Spring Boot 4.x.
2. Replace each `Pageables`/`Pages` call with a `Paginator` (table above).
3. For entities whose id isn't `id`, add `cfg -> cfg.tiebreaker("theirId")`.
4. For native queries sorting by columns, add `cfg -> cfg.skipPropertyValidation()`.
5. Where clients legitimately need more than 100 rows a page, add `cfg -> cfg.maxPageSize(n)`.
6. Run the tests: a misconfigured paginator fails as soon as its class loads.
