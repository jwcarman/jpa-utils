package org.jwcarman.jpa.pagination;

import java.util.Optional;

/**
 * A specification for a page of data.
 */
public interface PageSpec<S extends Enum<S> & SortPropertyProvider> {

    /**
     * The page index, 0-based.
     *
     * @return the page index
     */
    Optional<Integer> pageIndex();

    /**
     * The size of the page.
     *
     * @return the size of the page
     */
    Optional<Integer> pageSize();

    /**
     * The field to sort by.
     *
     * @return the field to sort by
     */
    Optional<S> sortBy();

    /**
     * The direction to sort by.
     *
     * @return the direction to sort by
     */
    Optional<SortDirection> sortDirection();
}
