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
