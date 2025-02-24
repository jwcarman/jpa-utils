package org.jwcarman.jpa.pagination;

/**
 * A DTO representing pagination information.
 * @param pageIndex The index of the page
 * @param pageSize The size of the page
 * @param totalElementCount The total number of elements to be paginated
 * @param totalPageCount The total number of pages
 * @param hasNext Whether there is a next page
 * @param hasPrevious Whether there is a previous page
 */
public record PaginationDto(int pageIndex, int pageSize, long totalElementCount, long totalPageCount, boolean hasNext, boolean hasPrevious) {
}
