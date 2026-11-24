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
