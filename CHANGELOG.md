# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Security considerations documentation for search term length validation
- Error handling documentation with ProblemDetail (RFC 7807) examples
- `MIN_PAGE_SIZE` constant to eliminate magic numbers
- `clampPageSize()` helper method for consistent page size validation
- Comprehensive test coverage for `defaultPageSize` edge cases

### Fixed
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
