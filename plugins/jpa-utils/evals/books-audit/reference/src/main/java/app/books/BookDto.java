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
