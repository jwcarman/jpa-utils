package org.jwcarman.jpa.spring.page;

import org.jwcarman.jpa.pagination.PageDto;
import org.jwcarman.jpa.pagination.PaginationDto;
import org.springframework.data.domain.Page;

public class Pages {

// -------------------------- STATIC METHODS --------------------------

    public static <T> PageDto<T> pageDtoOf(Page<T> page) {
        return new PageDto<>(page.getContent(), paginationDtoOf(page));
    }

    private static <T> PaginationDto paginationDtoOf(Page<T> page) {
        return new PaginationDto(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext(), page.hasPrevious());
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private Pages() {
        throw new UnsupportedOperationException("Utility class");
    }

}
