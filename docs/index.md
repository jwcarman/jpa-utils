---
hide:
  - navigation
  - toc
---

<div class="jw-hero" markdown>

# jpa-utils

<p class="jw-hero__tagline">Paging that never repeats a row, entities that never change identity.</p>

Java 25+ · Jakarta Persistence 3.2 · Spring Data JPA (Spring Boot 4.x) for the Spring integration

<div class="jw-pages" role="img" aria-label="Three pages of people sorted by last name. Several rows tie on the last name Smith and are split across pages 1 and 2 in id order, so each appears exactly once.">
  <div class="jw-page">
    <span class="jw-page__label">page 1</span>
    <span class="jw-row"><b>Jones</b><i>#04</i></span>
    <span class="jw-row is-tied"><b>Smith</b><i>#01</i></span>
    <span class="jw-row is-tied"><b>Smith</b><i>#03</i></span>
  </div>
  <div class="jw-page">
    <span class="jw-page__label">page 2</span>
    <span class="jw-row is-tied"><b>Smith</b><i>#07</i></span>
    <span class="jw-row is-tied"><b>Smith</b><i>#09</i></span>
    <span class="jw-row"><b>Taylor</b><i>#02</i></span>
  </div>
  <div class="jw-page">
    <span class="jw-page__label">page 3</span>
    <span class="jw-row"><b>Walker</b><i>#05</i></span>
    <span class="jw-row"><b>Young</b><i>#08</i></span>
  </div>
</div>
<p class="jw-pages__caption">ORDER BY last_name ASC, id ASC</p>

<p class="jw-hero__actions">
  <a class="md-button md-button--primary" href="guides/getting-started/">Get started</a>
  <a class="md-button" href="https://github.com/jwcarman/jpa-utils">View on GitHub</a>
</p>

</div>

jpa-utils is the plumbing every JPA application rewrites: a base entity with a stable identifier, a way to turn
a web request into a sorted, bounded page and back into a response, and simple text search across the fields you
mark. It keeps the web layer free of JPA types and the domain free of web types.

```java
private static final Paginator PEOPLE = Paginator.of(Person.class, PersonSort.class, cfg -> cfg
        .defaultSort(PersonSort.LAST_NAME));

public PageDto<PersonDto> search(String query, PageSpec spec) {
    return PEOPLE.paginate(spec, pageable -> people.search(query, pageable), PersonDto::of);
}
```

## Why

Paging by a column that is not unique is subtly broken in plain SQL: rows that tie can come back in a different
order for each page's query, so a row appears twice or never. Entity identity is subtly broken too: an `equals`
based on a database-assigned id changes when the entity is saved, and one that reads fields directly fails for
Hibernate's lazy proxies. jpa-utils gets these right once, with tests against five databases, so applications
don't each discover them in production.

## What you get

<div class="grid cards" markdown>

-   **Stable paging**

    ---

    Every sort ends with a unique tiebreaker (the entity's `id` by default), so a row lands on exactly one page.
    See [Stable Paging](guides/stable-paging.md).

-   **One object per listing**

    ---

    A `Paginator` is configured once (sort enum, default sort, page sizes) and turns a request into a
    `Pageable` and a `Page` into a `PageDto`. See [Paging and Sorting](guides/paging.md).

-   **Mistakes fail at startup**

    ---

    Every sort property and tiebreaker is checked against the entity when the paginator is built, not on every
    request. See [Property Validation](guides/property-validation.md).

-   **Entities with stable identity**

    ---

    `BaseEntity` assigns a UUID v7 at construction, carries an optimistic-locking version, and has an
    `equals`/`hashCode` that holds before and after save and for lazy proxies. See [Entities](guides/entities.md).

-   **Search by annotation**

    ---

    Mark `String` fields `@Searchable`; `SearchableRepository.search(term, pageable)` matches every word of the
    term across them, in any order, so `john smith` finds John Smith. See [Search](guides/search.md).

-   **A framework-free web contract**

    ---

    `PageSpec` in, `PageDto` out, with `PageParams` binding query parameters in Spring MVC. Clients send
    `sortBy=LAST_NAME`, never a JPA property path. See [Web Layer](guides/web.md).

</div>

## Packages

| Package | What it is |
|---------|------------|
| `org.jwcarman.jpa.entity` | `BaseEntity`: UUID v7 id, version, proxy-safe identity |
| `org.jwcarman.jpa.pagination` | Framework-free contracts: `PageSpec`, `PageDto`, `SortPropertyProvider`, `SortDirection` |
| `org.jwcarman.jpa.search` | `@Searchable` and `Searchables`, the Criteria API predicate builder |
| `org.jwcarman.jpa.spring.page` | `Paginator`, between `PageSpec`/`PageDto` and Spring Data's `Pageable`/`Page` |
| `org.jwcarman.jpa.spring.search` | `SearchableRepository`, a `JpaRepository` with `search(term, pageable)` |
| `org.jwcarman.jpa.spring.audit` | `AuditableEntity`: created/modified date and user via Spring Data auditing |
| `org.jwcarman.jpa.spring.web` | `PageParams`, a `PageSpec` that Spring MVC binds from query parameters |

Spring is optional: the `entity`, `pagination` and `search` packages need only Jakarta Persistence.

## Where next

- [Getting Started](guides/getting-started.md): dependency, entity, sort enum, repository, service, controller
- [Paging and Sorting](guides/paging.md): everything a `Paginator` can be configured to do
- [Stable Paging](guides/stable-paging.md): why ties break paging, and the tiebreaker that fixes it
- [Migrating to 0.1.0](guides/migrating-to-0.1.md): from `Pageables`/`Pages` and 0.0.x
