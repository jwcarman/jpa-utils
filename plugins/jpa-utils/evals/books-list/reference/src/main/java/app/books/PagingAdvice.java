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
