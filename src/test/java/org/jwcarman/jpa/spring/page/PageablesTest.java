package org.jwcarman.jpa.spring.page;

import org.junit.jupiter.api.Test;
import org.jwcarman.jpa.pagination.PageSpec;
import org.jwcarman.jpa.pagination.SortDirection;
import org.jwcarman.jpa.pagination.SortPropertyProvider;
import org.jwcarman.jpa.pagination.UnknownSortByValueException;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jwcarman.jpa.spring.page.Pageables.DEFAULT_PAGE_SIZE;
import static org.jwcarman.jpa.spring.page.Pageables.FIRST_PAGE;

class PageablesTest {

    @Test
    void shouldCreatePageableFromNullSpecWithDefaultPageSize() {
        final var pageable = Pageables.pageableOf(null, PersonSort.class);
        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isEqualTo(FIRST_PAGE);
        assertThat(pageable.getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
        assertThat(pageable.getOffset()).isZero();
    }

    @Test
    void shouldCreatePageableFromNullSpecWithCustomDefaultPageSize() {
        final var pageable = Pageables.pageableOf(null, PersonSort.class, 10);
        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isEqualTo(FIRST_PAGE);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
        assertThat(pageable.getOffset()).isZero();
    }

    @Test
    void shouldCreateUnsortedWithNoSortBy() {
        final var spec = new PageSpecDto(0, 10, null, null);

        final var pageable = Pageables.pageableOf(spec, PersonSort.class);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldCreateAscendingSorted() {
        final var spec = new PageSpecDto(0, 10, "FIRST_NAME", SortDirection.ASC);

        final var pageable = Pageables.pageableOf(spec, PersonSort.class);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "firstName"));
    }

    @Test
    void shouldCreateDescendingSorted() {
        final var spec = new PageSpecDto(0, 10, "LAST_NAME", SortDirection.DESC);

        final var pageable = Pageables.pageableOf(spec, PersonSort.class);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "lastName"));
    }

    @Test
    void shouldCreateDefaultSorted() {
        final var spec = new PageSpecDto(0, 10, "LAST_NAME", null);

        final var pageable = Pageables.pageableOf(spec, PersonSort.class);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.DEFAULT_DIRECTION, "lastName"));
    }

    @Test
    void shouldUseDefaultPageSizeWhenSpecPageSizeIsNull() {
        final var spec = new PageSpecDto(0, null, null, null);

        final var pageable = Pageables.pageableOf(spec, PersonSort.class);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldUseCustomDefaultPageSizeWhenSpecPageSizeIsNull() {
        final var spec = new PageSpecDto(0, null, null, null);

        final var pageable = Pageables.pageableOf(spec, PersonSort.class, 15);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(15);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldUseFirstPageWhenSpecPageIndexIsNull() {
        final var spec = new PageSpecDto(null, 10, null, null);

        final var pageable = Pageables.pageableOf(spec, PersonSort.class);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isEqualTo(FIRST_PAGE);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldThrowExceptionForInvalidSortField() {
        final var spec = new PageSpecDto(0, 10, "INVALID_FIELD", SortDirection.ASC);

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                Pageables.pageableOf(spec, PersonSort.class)
        ))
                .isInstanceOf(UnknownSortByValueException.class)
                .hasMessageContaining("INVALID_FIELD")
                .hasMessageContaining("FIRST_NAME")
                .hasMessageContaining("LAST_NAME");
    }

    @Test
    void shouldThrowExceptionForLowercaseSortField() {
        final var spec = new PageSpecDto(0, 10, "first_name", SortDirection.ASC);

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                Pageables.pageableOf(spec, PersonSort.class)
        ))
                .isInstanceOf(UnknownSortByValueException.class)
                .hasMessageContaining("first_name")
                .hasMessageContaining("FIRST_NAME")
                .hasMessageContaining("LAST_NAME");
    }

    @Test
    void shouldBeCaseSensitiveForSortField() {
        final var spec = new PageSpecDto(0, 10, "First_Name", SortDirection.ASC);

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                Pageables.pageableOf(spec, PersonSort.class)
        ))
                .isInstanceOf(UnknownSortByValueException.class)
                .hasMessageContaining("First_Name");
    }

    @Test
    void shouldHandleNullSortEnumClass() {
        final var spec = new PageSpecDto(0, 10, "LAST_NAME", SortDirection.ASC);

        final var pageable = Pageables.pageableOf(spec, null);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldClampZeroPageSizeToOne() {
        final var spec = new PageSpecDto(0, 0, null, null);

        final var pageable = Pageables.pageableOf(spec, PersonSort.class);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(1);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldClampNegativePageSizeToOne() {
        final var spec = new PageSpecDto(0, -5, null, null);

        final var pageable = Pageables.pageableOf(spec, PersonSort.class);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(1);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldClampNegativePageIndexToZero() {
        final var spec = new PageSpecDto(-1, 10, null, null);

        final var pageable = Pageables.pageableOf(spec, PersonSort.class);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldClampBothNegativePageIndexAndZeroPageSize() {
        final var spec = new PageSpecDto(-5, 0, null, null);

        final var pageable = Pageables.pageableOf(spec, PersonSort.class);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(1);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldClampPageSizeToMaximum() {
        final var spec = new PageSpecDto(0, 5000, null, null);

        final var pageable = Pageables.pageableOf(spec, PersonSort.class);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(Pageables.MAX_PAGE_SIZE);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldClampPageSizeToMaximumEvenWhenExtremelyLarge() {
        final var spec = new PageSpecDto(0, Integer.MAX_VALUE, null, null);

        final var pageable = Pageables.pageableOf(spec, PersonSort.class);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(Pageables.MAX_PAGE_SIZE);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void shouldAllowPageSizeAtExactlyMaximum() {
        final var spec = new PageSpecDto(0, Pageables.MAX_PAGE_SIZE, null, null);

        final var pageable = Pageables.pageableOf(spec, PersonSort.class);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(Pageables.MAX_PAGE_SIZE);
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

    public record PageSpecDto(
            Integer pageIndex,
            Integer pageSize,
            String sortBy,
            SortDirection sortDirection) implements PageSpec {
    }
}