package org.jwcarman.jpa.spring.page;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Pages {

// ------------------------------ FIELDS ------------------------------

    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 25;

    private static final PageRequest DEFAULT_PAGEABLE = PageRequest.of(DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, Sort.unsorted());

// -------------------------- STATIC METHODS --------------------------

    public static <T> PageDto<T> dtoOf(Page<T> page) {
        return new PageDto<>(page.getContent(), paginationDtoOf(page));
    }

    private static <T> PaginationDto paginationDtoOf(Page<T> page) {
        return new PaginationDto(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext(), page.hasPrevious());
    }

    public static <O extends Enum<O> & SortProvider> Pageable pageableOf(PageSpec spec, Class<O> orderType) {
        if (spec == null) {
            return DEFAULT_PAGEABLE;
        }

        final int pageNumber = spec.pageIndex().orElse(DEFAULT_PAGE_NUMBER);
        final int pageSize = spec.pageSize().orElse(DEFAULT_PAGE_SIZE);
        final Sort.Direction direction = spec.sortDirection()
                .map(sortDirection -> enumValue("sort direction", Sort.Direction.class, sortDirection))
                .orElse(Sort.Direction.ASC);
        final Sort sort = spec.sortBy()
                .map(sortBy -> enumValue("sort by", orderType, sortBy))
                .map(e -> e.order(direction))
                .map(Sort::by)
                .orElseGet(Sort::unsorted);
        return PageRequest.of(pageNumber, pageSize, sort);
    }

    private static <E extends Enum<E>> E enumValue(String typeName, Class<E> enumType, String value) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid %s value '%s' (expecting one of: %s)", typeName, value, enumValues(enumType)));
        }
    }

    private static String enumValues(Class<? extends Enum<?>> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(Enum::name)
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(", "));
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private Pages() {
        // Prevents instantiation (utility class)
    }

}
