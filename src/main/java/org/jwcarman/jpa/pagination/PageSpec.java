package org.jwcarman.jpa.pagination;

/**
 * A specification for a page of data.
 */
public interface PageSpec<S extends Enum<S> & SortPropertyProvider> {

    /**
     * The page index, 0-based.
     *
     * @return the page index
     */
    Integer pageIndex();

    /**
     * The size of the page.
     *
     * @return the size of the page
     */
    Integer pageSize();

    /**
     * The field to sort by.
     *
     * @return the field to sort by
     */
    S sortBy();

    /**
     * The direction to sort by.
     *
     * @return the direction to sort by
     */
    SortDirection sortDirection();
}
