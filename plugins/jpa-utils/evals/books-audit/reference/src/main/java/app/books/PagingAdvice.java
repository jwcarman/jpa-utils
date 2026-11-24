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
