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
