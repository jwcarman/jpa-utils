package org.jwcarman.jpa.spring.page;

import org.junit.jupiter.api.Test;
import org.jwcarman.jpa.pagination.PageSpec;
import org.jwcarman.jpa.pagination.SortDirection;
import org.jwcarman.jpa.pagination.SortPropertyProvider;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jwcarman.jpa.spring.page.Pageables.DEFAULT_PAGE_SIZE;
import static org.jwcarman.jpa.spring.page.Pageables.FIRST_PAGE;

class PageablesTest {

    @Test
    void shouldCreatePageableFromNullSpecWithDefaultPageSize() {
        final var pageable = Pageables.pageableOf(null);
        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isEqualTo(FIRST_PAGE);
        assertThat(pageable.getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
        assertThat(pageable.getOffset()).isZero();
    }

    @Test
    void shouldCreatePageableFromNullSpecWithCustomDefaultPageSize() {
        final var pageable = Pageables.pageableOf(null, 10);
        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isEqualTo(FIRST_PAGE);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
        assertThat(pageable.getOffset()).isZero();
    }

    @Test
    void shouldCreateUnsortedWithNoSortBy() {
        final var spec = new PageSpecDto<PersonSort>(0, 10, null, null);

        final var pageable = Pageables.pageableOf(spec);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldCreateAscendingSorted() {
        final var spec = new PageSpecDto<>(0, 10, PersonSort.FIRST_NAME, SortDirection.ASC);

        final var pageable = Pageables.pageableOf(spec);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "firstName"));
    }

    @Test
    void shouldCreateDescendingSorted() {
        final var spec = new PageSpecDto<>(0, 10, PersonSort.LAST_NAME, SortDirection.DESC);

        final var pageable = Pageables.pageableOf(spec);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "lastName"));
    }

    @Test
    void shouldCreateDefaultSorted() {
        final var spec = new PageSpecDto<>(0, 10, PersonSort.LAST_NAME, null);

        final var pageable = Pageables.pageableOf(spec);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.DEFAULT_DIRECTION, "lastName"));
    }

    @Test
    void shouldUseDefaultPageSizeWhenSpecPageSizeIsNull() {
        final var spec = new PageSpecDto<PersonSort>(0, null, null, null);

        final var pageable = Pageables.pageableOf(spec);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldUseCustomDefaultPageSizeWhenSpecPageSizeIsNull() {
        final var spec = new PageSpecDto<PersonSort>(0, null, null, null);

        final var pageable = Pageables.pageableOf(spec, 15);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(15);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldUseFirstPageWhenSpecPageIndexIsNull() {
        final var spec = new PageSpecDto<PersonSort>(null, 10, null, null);

        final var pageable = Pageables.pageableOf(spec);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isEqualTo(FIRST_PAGE);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    private enum PersonSort implements SortPropertyProvider {
        FIRST_NAME("firstName"),
        LAST_NAME("lastName");

        private final String sortProperty;

        PersonSort(String sortProperty) {
            this.sortProperty = sortProperty;
        }

        @Override
        public String getSortProperty() {
            return sortProperty;
        }
    }

    public record PageSpecDto<S extends Enum<S> & SortPropertyProvider>(
            Integer pageIndex,
            Integer pageSize,
            S sortBy,
            SortDirection sortDirection) implements PageSpec<S> {
    }
}