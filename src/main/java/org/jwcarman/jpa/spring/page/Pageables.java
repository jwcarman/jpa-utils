package org.jwcarman.jpa.spring.page;

import lombok.experimental.UtilityClass;
import org.jwcarman.jpa.pagination.PageSpec;
import org.jwcarman.jpa.pagination.SortDirection;
import org.jwcarman.jpa.pagination.SortPropertyProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static java.util.Optional.ofNullable;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;


@UtilityClass
public class Pageables {

// ------------------------------ FIELDS ------------------------------

    public static final int FIRST_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;

// -------------------------- STATIC METHODS --------------------------

    /**
     * Create a pageable from the given page specification. If the spec is null, a default pageable is returned with
     * page index 0, default page size 20, and unsorted.
     *
     * @param spec the page specification
     * @param <S>  the sort enum type
     * @return the pageable
     */
    public static <S extends Enum<S> & SortPropertyProvider> Pageable pageableOf(PageSpec<S> spec) {
        return pageableOf(spec, DEFAULT_PAGE_SIZE);
    }

    /**
     * Create a pageable from the given page specification. If the spec is null, a default pageable is returned with
     * page index 0, the given default page size, and unsorted.
     *
     * @param spec            the page specification
     * @param defaultPageSize the default page size to use if the spec does not specify one
     * @param <S>             the sort enum type
     * @return the pageable
     */
    public static <S extends Enum<S> & SortPropertyProvider> Pageable pageableOf(PageSpec<S> spec, int defaultPageSize) {
        if (spec == null) {
            return PageRequest.of(FIRST_PAGE, defaultPageSize, Sort.unsorted());
        }
        final int pageNumber = ofNullable(spec.pageIndex()).orElse(FIRST_PAGE);
        final int pageSize = ofNullable(spec.pageSize()).orElse(defaultPageSize);

        final Sort.Direction direction = sortDirectionOf(spec);

        final Sort sort = ofNullable(spec.sortBy())
                .map(SortPropertyProvider::getSortProperty)
                .map(property -> Sort.by(direction, property))
                .orElseGet(Sort::unsorted);

        return PageRequest.of(pageNumber, pageSize, sort);
    }

    private static <S extends Enum<S> & SortPropertyProvider> Sort.Direction sortDirectionOf(PageSpec<S> spec) {
        return ofNullable(spec.sortDirection())
                .map(e -> e == SortDirection.ASC ? ASC : DESC)
                .orElse(Sort.DEFAULT_DIRECTION);
    }

}
