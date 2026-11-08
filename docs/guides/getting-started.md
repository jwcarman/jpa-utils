# Getting Started

This guide builds a searchable, sortable, paged `GET /api/people` endpoint: an entity, a sort enum, a repository,
a service and a controller.

## Requirements

- Java 25+
- Jakarta Persistence 3.2 (Hibernate 7)
- For the Spring integration (`Paginator`, `SearchableRepository`, `PageParams`, `AuditableEntity`):
  Spring Data JPA 2026.0+ / Spring Framework 7, as in Spring Boot 4.x

## Add the dependency

!!! note "Version"
    The snippets name the latest release. This site tracks `main`; anything the
    [Changelog](https://github.com/jwcarman/jpa-utils/blob/main/CHANGELOG.md) lists under *Unreleased* is not in
    that release yet.

```xml
<dependency>
    <groupId>org.jwcarman.jpa</groupId>
    <artifactId>jpa-utils</artifactId>
    <version>0.1.0</version>
</dependency>
```

The Spring dependencies are optional in jpa-utils' POM: a Spring Boot application already has them, and
an application without Spring can use the entity, pagination contracts and search predicate on their own.

## 1. The entity

Extend [`BaseEntity`](entities.md) for a UUID v7 identifier, optimistic locking and a correct
`equals`/`hashCode`. Mark the `String` fields that free-text search should match with `@Searchable`.

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Person extends BaseEntity {

    @Searchable
    private String firstName;

    @Searchable
    private String lastName;

    @Searchable
    private String email;

    public Person(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }
}
```

## 2. The sort enum

Clients sort by name (`sortBy=LAST_NAME`), never by a JPA property path. An enum implementing
`SortPropertyProvider` maps each accepted name to the property it sorts by.

```java
public enum PersonSort implements SortPropertyProvider {
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    EMAIL("email");

    private final String sortProperty;

    PersonSort(String sortProperty) {
        this.sortProperty = sortProperty;
    }

    @Override
    public String getSortProperty() {
        return sortProperty;
    }
}
```

## 3. The repository

`SearchableRepository` is a `JpaRepository` plus `JpaSpecificationExecutor` with one extra method,
`search(String term, Pageable pageable)`, which matches the `@Searchable` fields.

```java
public interface PersonRepository extends SearchableRepository<Person, UUID> {
}
```

## 4. The service

A [`Paginator`](paging.md) is configured once, for the entity and its sort enum, and reused. `paginate` builds
the `Pageable` from the request, runs your query with it, and maps each entity into the response.

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
```

The service takes a `PageSpec`, the framework-free request contract, so it doesn't depend on Spring MVC.

## 5. The controller

`PageParams` is a `PageSpec` that Spring MVC binds from the `pageIndex`, `pageSize`, `sortBy` and
`sortDirection` query parameters, all optional.

```java
@RestController
@RequestMapping("/api/people")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService people;

    @GetMapping
    public PageDto<PersonDto> search(@RequestParam(required = false) String query, PageParams page) {
        return people.search(query, page);
    }
}
```

## Try it

```
GET /api/people?query=smith&pageIndex=0&pageSize=20&sortBy=FIRST_NAME&sortDirection=DESC
```

```json
{
  "data": [
    { "id": "019a6b0c-3c1e-7a51-9d2e-5b0f4c8e2a17", "firstName": "Zoe", "lastName": "Smith", "email": "zoe@example.com" }
  ],
  "pagination": {
    "pageIndex": 0,
    "pageSize": 20,
    "totalElementCount": 1,
    "totalPageCount": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

The query that ran sorts by `first_name DESC, id ASC`: the requested sort, then the [tiebreaker](stable-paging.md)
that keeps pages stable. A request without `sortBy` sorts by last name, the configured default.
