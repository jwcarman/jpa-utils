# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-11-08

The first minor-version release. It moves to Java 25 and Spring Boot 4, replaces the static `Pageables`/`Pages`
utilities with a configured `Paginator`, and makes paging stable. See
[Migrating to 0.1.0](https://jwcarman.github.io/jpa-utils/guides/migrating-to-0.1/).

### Breaking changes

- **Java 25 / Spring Boot 4 baseline.** Compiled against Java 25, Spring Framework 7, Spring Data 2026.0,
  Hibernate 7 and Jakarta Persistence 3.2 (previously Java 21, Spring Framework 6.2, Hibernate 6.6). Applications on
  Spring Boot 3.x should stay on 0.0.x.
- **`Pageables` and `Pages` are replaced by `Paginator`.**

  | 0.0.x | 0.1.0 |
  |-------|-------|
  | `Pageables.pageableOf(spec, XSort.class)` | `Paginator.of(X.class, XSort.class).pageableOf(spec)` |
  | `Pageables.pageableOf(spec, XSort.class, n)` | `Paginator.of(X.class, XSort.class, cfg -> cfg.defaultPageSize(n)).pageableOf(spec)` |
  | `Pages.pageDtoOf(page)` | `paginator.pageDtoOf(page)` |
  | `Pages.pageDtoOf(page.map(XDto::of))` | `paginator.pageDtoOf(page, XDto::of)` |
  | `Pageables.MAX_PAGE_SIZE` | `Paginator.DEFAULT_MAX_PAGE_SIZE` |

- **Every sort ends with a unique tiebreaker** (default property `id`), and a request without `sortBy` is sorted by
  it instead of being unsorted. An entity whose identifier is not named `id` must configure
  `cfg -> cfg.tiebreaker("theirId")` (or `noTiebreaker()`).
- **Sort properties are validated when a `Paginator` is built.** A sort enum constant or tiebreaker naming a property
  the entity doesn't have fails at construction with `IllegalArgumentException`. Native queries that sort by column
  names need `cfg -> cfg.skipPropertyValidation()`.
- **Search matches word by word.** The term is split on whitespace and a row matches when every word appears in at
  least one `@Searchable` field, in any order, so `john smith` finds first name John, last name Smith. Before, the
  whole term had to appear in one field. Anything that matched before still matches. At most 10 words are used
  (`Searchables.MAX_SEARCH_WORDS`).
- **The default maximum page size is 100** (was 1000). Raise it per paginator with `cfg -> cfg.maxPageSize(n)`.
- **An out-of-range default page size is rejected** when the `Paginator` is built instead of being clamped.
- **A blank `sortBy` counts as absent**, so the default sort applies instead of `UnknownSortByValueException`.
  Surrounding whitespace is ignored.
- **`BaseEntity.equals` and `hashCode` are no longer `final`** (see Fixed). Subclasses still shouldn't override them.

### Added

- **`Paginator`**, configured once per entity type and sort enum, immutable and thread-safe:
  `pageableOf(spec)`, `pageDtoOf(page[, mapper])` and `paginate(spec, query[, mapper])`. Configuration:
  `tiebreaker(String...)`, `noTiebreaker()`, `defaultSort(S)`, `defaultPageSize(int)`, `maxPageSize(int)`,
  `skipPropertyValidation()`.
- **Stable paging.** The tiebreaker makes the sort a total order, so rows can't repeat or vanish across pages when
  sort values tie. Without it, PostgreSQL, MySQL and CockroachDB returned duplicate rows across pages in the
  integration tests.
- **Startup validation of sort properties**, using Spring Data's property-path rules (nested and inherited
  properties included), with messages naming the enum constant, the path and the entity.
- **`SearchableRepository.searchFor(term)`**: the repository's search as a `Specification`, to combine with other
  conditions: `articles.findAll(articles.searchFor(term).and(isPublished()), pageable)`.
- **Case-insensitive `sortDirection`** (`asc`, `DESC`, ` Desc `) via `SortDirectionConverter`, registered
  automatically in Spring Boot servlet applications by `PagingWebAutoConfiguration`. Spring's enum binding is
  case-sensitive, so `?sortDirection=desc` used to be a 400.

### Fixed

- **`BaseEntity` equality with Hibernate lazy proxies.** `equals`/`hashCode` were `final` and read the `id` field,
  which a proxy does not hold, so a `getReference(...)` proxy was not equal to the loaded entity and hashed
  differently. They now read `getId()`.
- **`BaseEntity` ids are strictly increasing within a JVM.** Entities now share one UUID v7 generator; a new
  generator per entity ordered ids only to the millisecond.
- **Search without Spring.** `slf4j-api` is now a direct dependency; it previously arrived only through the optional
  Spring Data dependency, so searching without Spring failed with `NoClassDefFoundError`.

### Documentation

- A documentation site at https://jwcarman.github.io/jpa-utils/ with guides for getting started, entities, paging,
  stable paging, property validation, search, the web layer, databases, and migrating to 0.1.0, plus each release's
  Javadoc under `/api/`. The README is now a short overview that links to it.

### Requirements

- Java 25+
- Jakarta Persistence 3.2 (Hibernate 7)
- For the Spring integration: Spring Data JPA 2026.0+ / Spring Framework 7 (Spring Boot 4.x)

## [0.0.12] and earlier

Releases 0.0.1 through 0.0.12 predate this changelog; see the
[GitHub releases](https://github.com/jwcarman/jpa-utils/releases).

[Unreleased]: https://github.com/jwcarman/jpa-utils/compare/0.1.0...HEAD
[0.1.0]: https://github.com/jwcarman/jpa-utils/releases/tag/0.1.0
[0.0.12]: https://github.com/jwcarman/jpa-utils/releases/tag/0.0.12
