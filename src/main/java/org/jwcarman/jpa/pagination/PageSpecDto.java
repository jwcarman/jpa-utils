package org.jwcarman.jpa.pagination;

import java.util.Optional;

public record PageSpecDto<S extends Enum<S> & SortPropertyProvider>(
        Optional<Integer> pageIndex,
        Optional<Integer> pageSize,
        Optional<S> sortBy,
        Optional<SortDirection> sortDirection) implements PageSpec<S> {
}
