package app.books;

import java.util.UUID;
import org.jwcarman.jpa.spring.search.SearchableRepository;

public interface BookRepository extends SearchableRepository<Book, UUID> {}
