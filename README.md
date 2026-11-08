# jpa-utils

[![CI](https://github.com/jwcarman/jpa-utils/actions/workflows/maven.yml/badge.svg)](https://github.com/jwcarman/jpa-utils/actions/workflows/maven.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-25%2B-orange)](https://openjdk.org/)
[![Maven Central](https://img.shields.io/maven-central/v/org.jwcarman.jpa/jpa-utils)](https://central.sonatype.com/artifact/org.jwcarman.jpa/jpa-utils)

[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)

Stable paging, UUID v7 entities, and annotation-driven search for
[Jakarta Persistence](https://jakarta.ee/specifications/persistence/) and Spring Data JPA.

**📖 Documentation: [jwcarman.github.io/jpa-utils](https://jwcarman.github.io/jpa-utils/) · API reference: [jwcarman.github.io/jpa-utils/api](https://jwcarman.github.io/jpa-utils/api/)**

- **Stable paging.** Every sort ends with a unique tiebreaker, so a row never repeats or vanishes across pages.
- **One object per listing.** A `Paginator` turns a `PageSpec` into a `Pageable`, and a `Page` back into a `PageDto`.
- **Mistakes fail at startup.** Sort properties and tiebreakers are checked against the entity when the paginator is
  built, not on every request.
- **Entities with stable identity.** `BaseEntity` assigns a UUID v7 at construction, adds optimistic locking, and has
  an `equals`/`hashCode` that holds for lazy proxies.
- **Search by annotation.** Mark fields `@Searchable`; a search matches word by word across them, case-insensitively,
  so `john smith` finds John Smith.
- **A framework-free web contract.** `PageSpec` in, `PageDto` out; `PageParams` binds them in Spring MVC.

## Requirements

- Java 25+
- Jakarta Persistence 3.2 (Hibernate 7)
- Spring Data JPA 2026.0+ / Spring Framework 7 (Spring Boot 4.x) for the Spring integration, which is optional

## Quick Start

> [!NOTE]
> The snippets name the latest release. `main` tracks ahead of it; anything the
> [Changelog](CHANGELOG.md) lists under *Unreleased* is not in that release yet.

```xml
<dependency>
    <groupId>org.jwcarman.jpa</groupId>
    <artifactId>jpa-utils</artifactId>
    <version>0.1.0</version>
</dependency>
```

An entity, its sort options, and a repository:

```java
@Entity
public class Person extends BaseEntity {
    @Searchable private String firstName;
    @Searchable private String lastName;
    // ...
}

public enum PersonSort implements SortPropertyProvider {
    FIRST_NAME("firstName"),
    LAST_NAME("lastName");
    // constructor and getSortProperty() return the property
}

public interface PersonRepository extends SearchableRepository<Person, UUID> {
}
```

A service and a controller:

```java
@Service
@RequiredArgsConstructor
public class PersonService {

    private static final Paginator PEOPLE = Paginator.of(Person.class, PersonSort.class, cfg -> cfg
            .defaultSort(PersonSort.LAST_NAME));

    private final PersonRepository people;

    public PageDto<PersonDto> search(String query, PageSpec spec) {
        return PEOPLE.paginate(spec, pageable -> people.search(query, pageable), PersonDto::of);
    }
}

@RestController
@RequiredArgsConstructor
public class PersonController {

    private final PersonService people;

    @GetMapping("/api/people")
    public PageDto<PersonDto> search(@RequestParam(required = false) String query, PageParams page) {
        return people.search(query, page);
    }
}
```

```
GET /api/people?query=smith&pageIndex=0&pageSize=20&sortBy=FIRST_NAME&sortDirection=DESC
```

That request sorts by `first_name DESC, id ASC`; without `sortBy` it sorts by last name. The
[Getting Started](https://jwcarman.github.io/jpa-utils/guides/getting-started/) guide walks through it in full.

## Upgrading from 0.0.x

0.1.0 replaces `Pageables` and `Pages` with `Paginator` and moves to Java 25 / Spring Boot 4. See
[Migrating to 0.1.0](https://jwcarman.github.io/jpa-utils/guides/migrating-to-0.1/).

## Tested databases

PostgreSQL, MySQL, MariaDB, CockroachDB and Oracle, on every build, via Testcontainers.

## License

Apache License 2.0; see [LICENSE](LICENSE).
