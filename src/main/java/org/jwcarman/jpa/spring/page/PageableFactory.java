package org.jwcarman.jpa.spring.page;

import org.jwcarman.jpa.pagination.PageSpec;
import org.jwcarman.jpa.pagination.SortPropertyProvider;
import org.springframework.data.domain.Pageable;

public interface PageableFactory {
    <S extends Enum<S> & SortPropertyProvider> Pageable createPageable(PageSpec<S> spec);
}
