# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Breaking Changes
- **Java 25 / Spring Boot 4 baseline**: compiled against Java 25, Spring Framework 7, Spring Data 2026.0,
  Hibernate 7 and Jakarta Persistence 3.2 (previously Java 21 / Spring Framework 6.2 / Hibernate 6.6)
- **`Pageables` and `Pages` removed** in favor of `Paginator`:
  `Pageables.pageableOf(spec, XSort.class)` → `Paginator.of(X.class, XSort.class).pageableOf(spec)`;
  `Pageables.pageableOf(spec, XSort.class, n)` → `Paginator.of(X.class, XSort.class, cfg -> cfg.defaultPageSize(n)).pageableOf(spec)`;
  `Pages.pageDtoOf(page)` → `paginator.pageDtoOf(page)`
- **Every `Pageable` now ends with a unique tiebreaker** (default property `id`), and requests without `sortBy`
  are sorted by it instead of unsorted. Entities whose identifier is not named `id` must configure
  `cfg -> cfg.tiebreaker("theirId")` (or `noTiebreaker()`), otherwise queries fail with `PropertyReferenceException`
- An out-of-range default page size is now rejected when the `Paginator` is built instead of being clamped
- The default maximum page size is now 100 (was 1000); raise it per paginator with `cfg -> cfg.maxPageSize(n)`
- A blank `sortBy` (e.g. `?sortBy=`) is treated as absent, so the default sort applies, instead of raising
  `UnknownSortByValueException`; surrounding whitespace is ignored

### Added
- `Paginator`: configured once per entity type and sort enum (`Paginator.of(X.class, XSort.class, cfg -> ...)`),
  immutable and thread-safe
  - `pageableOf(PageSpec)`, `pageDtoOf(Page)`, `pageDtoOf(Page, mapper)`,
    `paginate(PageSpec, query)` and `paginate(PageSpec, query, mapper)`
  - Config: `tiebreaker(String...)`, `noTiebreaker()`, `defaultSort(S)`, `defaultPageSize(int)`, `maxPageSize(int)`,
    `skipPropertyValidation()`
- Property validation: when a `Paginator` is built, every sort enum constant's property and every tiebreaker property
  is checked against the entity type (Spring Data property-path rules, nested paths included), so a typo or a missing
  `id` fails at startup with `IllegalArgumentException` instead of a `PropertyReferenceException` on every request.
  Native queries that sort by column names opt out with `skipPropertyValidation()`
- Stable paging: the tiebreaker makes the sort a total order, so rows can no longer repeat or vanish across pages
  when sort values tie (reproduced on PostgreSQL, MySQL and CockroachDB before the fix)
- Security considerations documentation for search term length validation
- Error handling documentation with ProblemDetail (RFC 7807) examples
- `MIN_PAGE_SIZE` constant to eliminate magic numbers
- `clampPageSize()` helper method for consistent page size validation
- Comprehensive test coverage for `defaultPageSize` edge cases

### Documentation
- A documentation site at https://jwcarman.github.io/jpa-utils/ (MkDocs Material, the shared jwcarman theme):
  guides for entities, paging, stable paging, property validation, search, the web layer, databases, and
  migrating to 0.1.0; each release's Javadoc under `/api/`. The README is now a short overview that links to it

### Fixed
- `BaseEntity.equals`/`hashCode` with Hibernate lazy proxies: they were `final` and read the `id` field, which a
  proxy (a generated subclass) does not hold, so `getReference(...)` proxies were not equal to the loaded entity and
  hashed differently. They now read `getId()` and are no longer `final`; subclasses should still not override them
- `defaultPageSize` parameter not being clamped when `spec` is null
- Inconsistent page size validation between code paths

### Changed
- Improved error handling examples using `@RestControllerAdvice`
- Enhanced README with security best practices

## [1.0-SNAPSHOT] - Current Development

### Added
- UUID v7-based `BaseEntity` with optimistic locking
- `AuditableEntity` with automatic creation/modification tracking
- Framework-agnostic pagination contracts (`PageSpec`, `PageDto`, `PaginationDto`)
- `@Searchable` annotation for marking searchable entity fields
- `Searchables` utility for automatic LIKE query generation
- `SortPropertyProvider` interface for type-safe sorting
- Spring Data JPA integration (`Pageables`, `Pages`)
- `PageParams` record for automatic Spring Web parameter binding
- `SearchableRepository` combining JpaRepository with built-in search
- `UnknownSortByValueException` for invalid sort field handling
- Automatic page size clamping (MIN: 1, MAX: 1000)
- SQL wildcard escaping for secure search queries
- Multi-database integration tests (PostgreSQL, MySQL, MariaDB, CockroachDB, Oracle, H2)
- Comprehensive Javadoc documentation
- GitHub Actions CI/CD with SonarCloud integration
- Maven Central publishing workflow

### Security
- Page size clamping to prevent memory exhaustion attacks
- Search term wildcard escaping to prevent SQL injection
- Input validation for page index and page size

---

## Release Process

Releases will follow semantic versioning:
- **Major (X.0.0)**: Breaking API changes
- **Minor (x.Y.0)**: New features, backwards compatible
- **Patch (x.y.Z)**: Bug fixes, backwards compatible

[Unreleased]: https://github.com/jwcarman/jpa-utils/compare/v1.0-SNAPSHOT...HEAD
