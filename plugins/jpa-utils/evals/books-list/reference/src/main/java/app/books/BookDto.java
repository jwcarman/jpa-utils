package app.books;

import java.time.LocalDate;
import java.util.UUID;

public record BookDto(UUID id, String title, String author, LocalDate published) {

  static BookDto of(Book book) {
    return new BookDto(book.getId(), book.getTitle(), book.getAuthor(), book.getPublished());
  }
}
