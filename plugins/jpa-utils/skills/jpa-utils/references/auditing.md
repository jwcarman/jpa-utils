# Auditing: who and when, and stale edits

Over the listing in `listing.md`: each book records who created it and who last changed it, and
when; `PUT /api/books/{id}` changes one, refusing a stale `version` with `409`. The user is the
request's `X-User` header, `system` without one; take yours from wherever the app knows the user
(Spring Security: `SecurityContextHolder`). Every file is complete; files not shown are as in
`listing.md`.

How it works: `AuditableEntity` carries `@EntityListeners(AuditingEntityListener.class)`; Spring
Data fills the four fields on insert and update once `@EnableJpaAuditing` is on, asking the
`AuditorAware<String>` bean for the user. `createdDate` and `createdBy` are `updatable = false`.
The `version` from `BaseEntity` goes up on every update; compare the client's to it before changing
the row, because a loaded entity always carries the current one.

`src/main/java/app/audit/AuditConfig.java`

```java
package app.audit;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@EnableJpaAuditing
public class AuditConfig {

  static final String USER_HEADER = "X-User";
  static final String NO_USER = "system";

  @Bean
  public AuditorAware<String> auditor() {
    return () ->
        Optional.of(
            Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(a -> ((ServletRequestAttributes) a).getRequest().getHeader(USER_HEADER))
                .filter(user -> !user.isBlank())
                .orElse(NO_USER));
  }
}
```

`src/main/java/app/books/Book.java`

```java
package app.books;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import org.jwcarman.jpa.search.Searchable;
import org.jwcarman.jpa.spring.audit.AuditableEntity;

@Entity
public class Book extends AuditableEntity {

  @Searchable
  @Column(nullable = false, length = 100)
  private String title;

  @Searchable
  @Column(nullable = false, length = 100)
  private String author;

  @Column(nullable = false)
  private LocalDate published;

  protected Book() {}

  public Book(String title, String author, LocalDate published) {
    this.title = title;
    this.author = author;
    this.published = published;
  }

  public void change(String title, String author, LocalDate published) {
    this.title = title;
    this.author = author;
    this.published = published;
  }

  public String getTitle() {
    return title;
  }

  public String getAuthor() {
    return author;
  }

  public LocalDate getPublished() {
    return published;
  }
}
```

`src/main/java/app/books/BookChange.java`

```java
package app.books;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record BookChange(
    @NotBlank @Size(max = 100) String title,
    @NotBlank @Size(max = 100) String author,
    @NotNull LocalDate published,
    @NotNull Long version) {}
```

`src/main/java/app/books/BookDto.java`

```java
package app.books;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BookDto(
    UUID id,
    String title,
    String author,
    LocalDate published,
    String createdBy,
    Instant createdDate,
    String modifiedBy,
    Instant modifiedDate,
    Long version) {

  static BookDto of(Book book) {
    return new BookDto(
        book.getId(),
        book.getTitle(),
        book.getAuthor(),
        book.getPublished(),
        book.getCreatedBy(),
        book.getCreatedDate(),
        book.getModifiedBy(),
        book.getModifiedDate(),
        book.getVersion());
  }
}
```

`src/main/java/app/books/BookService.java`

```java
package app.books;

import java.util.UUID;
import org.jwcarman.jpa.pagination.PageDto;
import org.jwcarman.jpa.pagination.PageSpec;
import org.jwcarman.jpa.spring.page.Paginator;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

  private static final Paginator BOOKS =
      Paginator.of(Book.class, BookSort.class, cfg -> cfg.defaultSort(BookSort.TITLE));

  private final BookRepository books;

  public BookService(BookRepository books) {
    this.books = books;
  }

  @Transactional
  public BookDto add(NewBook book) {
    return BookDto.of(books.saveAndFlush(new Book(book.title(), book.author(), book.published())));
  }

  @Transactional
  public BookDto change(UUID id, BookChange change) {
    Book book = books.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    if (!book.getVersion().equals(change.version())) {
      throw new ObjectOptimisticLockingFailureException(Book.class, id);
    }
    book.change(change.title(), change.author(), change.published());
    return BookDto.of(books.saveAndFlush(book));
  }

  @Transactional(readOnly = true)
  public PageDto<BookDto> list(String search, PageSpec page) {
    return BOOKS.paginate(page, pageable -> books.search(search, pageable), BookDto::of);
  }
}
```

`src/main/java/app/books/BookNotFoundException.java`

```java
package app.books;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BookNotFoundException extends RuntimeException {

  public BookNotFoundException(UUID id) {
    super("No book " + id);
  }
}
```

`src/main/java/app/books/BookController.java`

```java
package app.books;

import jakarta.validation.Valid;
import java.util.UUID;
import org.jwcarman.jpa.pagination.PageDto;
import org.jwcarman.jpa.spring.web.PageParams;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

  private final BookService books;

  public BookController(BookService books) {
    this.books = books;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BookDto add(@Valid @RequestBody NewBook book) {
    return books.add(book);
  }

  @PutMapping("/{id}")
  public BookDto change(@PathVariable UUID id, @Valid @RequestBody BookChange change) {
    return books.change(id, change);
  }

  @GetMapping
  public PageDto<BookDto> list(@RequestParam(required = false) String search, PageParams page) {
    return books.list(search, page);
  }
}
```

`src/main/java/app/books/PagingAdvice.java`

```java
package app.books;

import org.jwcarman.jpa.pagination.UnknownSortByValueException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PagingAdvice {

  @ExceptionHandler(UnknownSortByValueException.class)
  public ProblemDetail unknownSortBy(UnknownSortByValueException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ProblemDetail stale(OptimisticLockingFailureException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The book has changed since it was read");
  }
}
```

## Pitfalls

- `@EnableJpaAuditing` missing: the four fields stay `null` and nothing fails.
- Use `saveAndFlush` when returning the entity after a change: an update's `modifiedDate`,
  `modifiedBy` and new `version` are set when Hibernate flushes, so a DTO built before the flush
  shows the old ones.
- Don't copy the client's `version` onto the loaded entity and hope Hibernate refuses it: compare,
  and throw `ObjectOptimisticLockingFailureException`, mapped to `409`.
- Keep the timestamps as `Instant` with their fractions: truncating to seconds makes two changes in
  the same second look simultaneous.
