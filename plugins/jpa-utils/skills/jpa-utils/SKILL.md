---
name: jpa-utils
description: Use when a Spring Boot / JPA app depends on jpa-utils (org.jwcarman.jpa:jpa-utils) and you add an entity, a paged, sorted or searchable list endpoint, or auditing (who and when). Covers BaseEntity, AuditableEntity, Paginator, PageParams, PageDto, SortPropertyProvider, @Searchable and SearchableRepository.
---

# jpa-utils

jpa-utils gives a Spring Data JPA app its entity base classes (UUID v7 ids, optimistic locking,
auditing), paged and sorted list endpoints with a fixed JSON shape, and free-text search over
marked fields. This skill covers `0.3.0` (see `jpa-utils.version` or the dependency in `pom.xml`).
Everything here is in the jar: don't unpack it to learn the API, this page and its two references
say what each type does.

## Which reference

| Job | Read |
|---|---|
| An entity with a paged, sorted, searchable list (`GET /api/things?pageIndex=&pageSize=&sortBy=&sortDirection=&search=`) | `references/listing.md` (~1,500 tokens): complete files, copy and rename |
| Who created / last changed a row and when, edits with a stale `version` refused | `references/auditing.md` (~2,300 tokens): complete files over the listing |

## The types, and the rules that go with them

- **`org.jwcarman.jpa.entity.BaseEntity`**: extend it. The id (`UUID`, v7) is assigned in the
  constructor, before the row exists; `getVersion()` is the `@Version` column; `equals`/`hashCode`
  use the id. Don't declare your own `@Id` or `@Version`: it duplicates the columns.
- **`org.jwcarman.jpa.spring.audit.AuditableEntity`**: extends `BaseEntity` with `createdDate`,
  `modifiedDate` (`Instant`), `createdBy`, `modifiedBy` (`String`), filled by Spring Data auditing.
  It does nothing until a `@Configuration` has `@EnableJpaAuditing`; the `*By` fields stay `null`
  without an `AuditorAware<String>` bean.
- **A sort enum** implementing `org.jwcarman.jpa.pagination.SortPropertyProvider`: each constant is a
  `sortBy` value clients send, `getSortProperty()` the entity property it sorts by.
- **`org.jwcarman.jpa.spring.page.Paginator`**: one per listing, a `static final` field:
  `Paginator.of(Book.class, BookSort.class, cfg -> cfg.defaultSort(BookSort.TITLE))`. Defaults:
  page size 20, max 100 (larger requests are clamped), and the entity's `id` appended as the last
  sort key, so paging through tied values never repeats or skips a row; keep it.
  `paginate(spec, pageable -> repository.search(term, pageable), BookDto::of)` returns the page.
- **`org.jwcarman.jpa.spring.web.PageParams`**: the controller parameter. Spring binds
  `pageIndex` (from 0), `pageSize`, `sortBy`, `sortDirection` (`ASC`/`DESC`, any case) from the
  query string, all optional. Pass it on as an `org.jwcarman.jpa.pagination.PageSpec`.
- **`org.jwcarman.jpa.pagination.PageDto<T>`**: the response, `{"data": [...], "pagination":
  {"pageIndex", "pageSize", "totalElementCount", "totalPageCount", "hasNext", "hasPrevious"}}`.
  Return it as is.
- **`org.jwcarman.jpa.pagination.UnknownSortByValueException`**: thrown for a `sortBy` the enum does
  not name. Without a handler it is a 500; map it to `400` in a `@RestControllerAdvice`.
- **`@org.jwcarman.jpa.search.Searchable`** on `String` fields, and a repository extending
  `org.jwcarman.jpa.spring.search.SearchableRepository<Book, UUID>` (a `JpaRepository` plus
  `JpaSpecificationExecutor`). `search(term, pageable)`:
  - splits the term on whitespace; a row matches when **every word** is found in **at least one**
    `@Searchable` field (`tower dark` finds "The Dark Tower" and "Tower of Darkness");
  - ignores case; escapes `%`, `_` and `\`, so they match themselves;
  - a `null` or blank term matches every row, so pass the request's `search` straight through;
  - `@Searchable(match = MatchMode.PREFIX)` or `EXACT` instead of the default `CONTAINS`.
  To add other conditions: `repository.findAll(repository.searchFor(term).and(spec), pageable)`.
- Every sort property is checked against the entity when the `Paginator` is built: a misspelled
  `getSortProperty()` fails when the service class loads (at startup, as
  `ExceptionInInitializerError` / `Error creating bean`); the real message is under **Caused by**.

## Before you finish

Run `./mvnw verify` and read the result; the build is the check.
