# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Maven-based Java library providing utilities for Jakarta Persistence API (JPA) applications. The library is published to Maven Central as `org.jwcarman.jpa:jpa-utils`.

**Key Technologies:**
- Java 23
- Jakarta Persistence API 3.1.0
- Spring Data JPA (optional integration)
- Maven for build management
- JUnit 5 and AssertJ for testing
- Lombok for reducing boilerplate

## Build and Test Commands

### Build the project
```bash
mvn clean install
```

### Run tests only
```bash
mvn test
```

### Run tests with coverage (CI profile)
```bash
mvn -Pci clean verify
```

### Run a single test class
```bash
mvn test -Dtest=ClassName
```

### Run a single test method
```bash
mvn test -Dtest=ClassName#methodName
```

### Build with SonarCloud analysis
```bash
mvn -Pci verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=jwcarman_jpa-utils
```

## Architecture

### Core Packages Structure

**`org.jwcarman.jpa.entity`**
- `BaseEntity`: MappedSuperclass providing UUID-based primary keys (using time-based epoch UUID v7 generator) and version field for optimistic locking
- Entities extending BaseEntity get stable identifiers at construction time and final equals/hashCode based on UUID

**`org.jwcarman.jpa.pagination`**
- Framework-agnostic pagination contracts
- `PageSpec<S>`: Interface for page specifications with sorting support
- `SortPropertyProvider`: Interface for enums to provide JPA property names for sorting
- `PageDto` and `PaginationDto`: DTOs for pagination responses

**`org.jwcarman.jpa.search`**
- `@Searchable`: Annotation to mark entity fields/getters as searchable
- `Searchables`: Utility class that builds JPA Criteria API predicates for case-insensitive LIKE searches across all @Searchable String fields
- Automatically sanitizes search terms to escape SQL wildcards

**`org.jwcarman.jpa.spring.page`**
- Spring Data JPA integration for pagination
- `Pageables`: Utility to convert framework-agnostic `PageSpec` to Spring's `Pageable` (defaults: page 0, size 20)
- `Pages`: Utility to convert Spring's `Page` to framework-agnostic `PageDto`
- `PageableFactory`: Interface for creating Pageables from DTOs

**`org.jwcarman.jpa.spring.search`**
- `SearchableRepository<S,I>`: Repository interface combining JpaRepository and JpaSpecificationExecutor with built-in search support
- Automatically integrates with `Searchables` utility to enable search across @Searchable fields

**`org.jwcarman.jpa.spring.audit`**
- `AuditableEntity`: Extends BaseEntity with Spring Data JPA auditing support
- Tracks createdDate, modifiedDate, createdBy, modifiedBy
- Requires `@EnableJpaAuditing` and an `AuditorAware` bean in Spring configuration

### Key Design Patterns

**Enum-based Sort Properties**: Sort fields are defined as enums implementing `SortPropertyProvider`, mapping enum constants to JPA property paths. This provides type-safe, IDE-friendly sort specifications.

**Annotation-driven Search**: Fields marked with `@Searchable` are automatically included in search queries, avoiding the need to manually build search specifications.

**Framework-agnostic Core**: The pagination and core entity packages have no Spring dependencies, allowing use in non-Spring JPA applications. Spring integration is provided separately in the `.spring.*` packages.

**UUID v7 Identifiers**: Uses time-based epoch UUIDs (version 7) from the java-uuid-generator library, which are more database-friendly than random UUIDs due to better locality.

## Testing

Tests use:
- Spring Boot Test with H2 in-memory database
- Hibernate as the JPA provider
- Test entities and repositories in `src/test/java/.../spring/search/` package demonstrate usage patterns

## Maven Profiles

- **`ci`**: Activates JaCoCo code coverage reporting for CI builds
- **`release`**: Configures artifact signing, source/javadoc generation, and publishing to Maven Central via Sonatype

## Important Notes

- All entities extending BaseEntity or AuditableEntity must have a protected no-arg constructor for JPA
- The library uses Lombok annotations (`@Getter`, `@UtilityClass`) - ensure annotation processing is enabled
- Spring Data integration is optional (marked as `<optional>true</optional>` in dependencies)
