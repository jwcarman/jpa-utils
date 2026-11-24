# A paged, sorted, searchable list

The whole of a books list: `POST /api/books` adds one (`201`), `GET /api/books` lists them with
`pageIndex`, `pageSize`, `sortBy` (`TITLE`, `AUTHOR`, `PUBLISHED`; default `TITLE`),
`sortDirection` and `search`. Every file is complete and builds as it stands; copy them into your
package and rename. Constructors are written out (no Lombok needed).


`src/main/java/app/books/Book.java`

```java
package app.books;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import org.jwcarman.jpa.entity.BaseEntity;
import org.jwcarman.jpa.search.Searchable;

@Entity
public class Book extends BaseEntity {

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

`src/main/java/app/books/BookSort.java`

```java
package app.books;

import org.jwcarman.jpa.pagination.SortPropertyProvider;

public enum BookSort implements SortPropertyProvider {
  TITLE("title"),
  AUTHOR("author"),
  PUBLISHED("published");

  private final String sortProperty;

  BookSort(String sortProperty) {
    this.sortProperty = sortProperty;
  }

  @Override
  public String getSortProperty() {
    return sortProperty;
  }
}
```

`src/main/java/app/books/BookRepository.java`

```java
package app.books;

import java.util.UUID;
import org.jwcarman.jpa.spring.search.SearchableRepository;

public interface BookRepository extends SearchableRepository<Book, UUID> {}
```

`src/main/java/app/books/NewBook.java`

```java
package app.books;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record NewBook(
    @NotBlank @Size(max = 100) String title,
    @NotBlank @Size(max = 100) String author,
    @NotNull LocalDate published) {}
```

`src/main/java/app/books/BookDto.java`

```java
package app.books;

import java.time.LocalDate;
import java.util.UUID;

public record BookDto(UUID id, String title, String author, LocalDate published) {

  static BookDto of(Book book) {
    return new BookDto(book.getId(), book.getTitle(), book.getAuthor(), book.getPublished());
  }
}
```

`src/main/java/app/books/BookService.java`

```java
package app.books;

import org.jwcarman.jpa.pagination.PageDto;
import org.jwcarman.jpa.pagination.PageSpec;
import org.jwcarman.jpa.spring.page.Paginator;
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
    return BookDto.of(books.save(new Book(book.title(), book.author(), book.published())));
  }

  @Transactional(readOnly = true)
  public PageDto<BookDto> list(String search, PageSpec page) {
    return BOOKS.paginate(page, pageable -> books.search(search, pageable), BookDto::of);
  }
}
```

`src/main/java/app/books/BookController.java`

```java
package app.books;

import jakarta.validation.Valid;
import org.jwcarman.jpa.pagination.PageDto;
import org.jwcarman.jpa.spring.web.PageParams;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
}
```

## Pitfalls

- Don't write your own search `Specification` or a `LIKE` query: `search` already splits words,
  ignores case and escapes wildcards. Write one only if the contract asks for something else (a
  whole-phrase match), and say why in a comment.
- Don't turn the tiebreaker off (`noTiebreaker()`): pages over tied sort values then repeat and skip
  rows, which a small test with distinct values never shows.
- Without `PagingAdvice`, an unknown `sortBy` answers 500.
