package org.jwcarman.jpa.spring.page;

import org.jwcarman.jpa.pagination.PageSpec;
import org.jwcarman.jpa.pagination.SortPropertyProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class DefaultPageableFactory implements PageableFactory {

// ------------------------------ FIELDS ------------------------------

    private static final int FIRST_PAGE = 0;

    private final int defaultPageSize;
    private final Pageable defaultPageable;

// --------------------------- CONSTRUCTORS ---------------------------

    public DefaultPageableFactory(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
        this.defaultPageable = PageRequest.of(FIRST_PAGE, defaultPageSize, Sort.unsorted());
    }

// -------------------------- OTHER METHODS --------------------------

    public <S extends Enum<S> & SortPropertyProvider> Pageable createPageable(PageSpec<S> spec) {
        if (spec == null) {
            return defaultPageable;
        }

        final int pageNumber = spec.pageNumber().orElse(FIRST_PAGE);
        final int pageSize = spec.pageSize().orElse(defaultPageSize);

        final Sort.Direction direction = sortDirectionOf(spec);

        final Sort sort = spec.sortBy()
                .map(SortPropertyProvider::getSortProperty)
                .map(property -> Sort.by(direction, property))
                .orElseGet(Sort::unsorted);

        return PageRequest.of(pageNumber, pageSize, sort);
    }

    private static <S extends Enum<S> & SortPropertyProvider> Sort.Direction sortDirectionOf(PageSpec<S> spec) {
        return spec.sortDirection()
                .map(e -> switch (e) {
                    case ASC -> Sort.Direction.ASC;
                    case DESC -> Sort.Direction.DESC;
                })
                .orElse(Sort.DEFAULT_DIRECTION);
    }

}
