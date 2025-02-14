package org.jwcarman.jpa.spring.page;

import org.springframework.data.domain.Sort;

/**
 * A provider of sort orders.
 */
@FunctionalInterface
public interface SortProvider {

    /**
     * Get the sort order.
     *
     * @param direction The direction
     * @return The sort order
     */
    Sort.Order order(Sort.Direction direction);
}
