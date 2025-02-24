package org.jwcarman.jpa.pagination;

import java.util.List;

/**
 * A DTO representing a page of data.
 * @param data The page data
 * @param pagination The pagination information
 * @param <T> the type of data in the page
 */
public record PageDto<T>(List<T> data, PaginationDto pagination) {
}
