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
