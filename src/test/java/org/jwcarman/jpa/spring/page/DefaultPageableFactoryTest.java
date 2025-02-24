package org.jwcarman.jpa.spring.page;

import lombok.Data;
import org.junit.jupiter.api.Test;
import org.jwcarman.jpa.pagination.PageSpecDto;
import org.jwcarman.jpa.pagination.SortDirection;
import org.jwcarman.jpa.pagination.SortPropertyProvider;
import org.springframework.data.domain.Sort;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPageableFactoryTest {
    @Test
    void shouldInitializeWithDefaultPageSize() {
        final var factory = new DefaultPageableFactory(23);
        assertThat(factory.getDefaultPageSize()).isEqualTo(23);
    }

    @Test
    void shouldCreatePageableFromNullSpec() {
        final var factory = new DefaultPageableFactory(10);
        final var pageable = factory.createPageable(null);
        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());
        assertThat(pageable.getOffset()).isZero();
    }

    @Test
    void shouldCreateUnsortedWithNoSortBy() {
        final var factory = new DefaultPageableFactory(10);
        final var spec = new PageSpecDto<PersonSort>(Optional.of(0), Optional.of(10), Optional.empty(), Optional.empty());

        final var pageable = factory.createPageable(spec);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.unsorted());

    }

    @Test
    void shouldCreateAscendingSorted() {
        final var factory = new DefaultPageableFactory(10);
        final var spec = new PageSpecDto<>(Optional.of(0), Optional.of(10), Optional.of(PersonSort.FIRST_NAME), Optional.of(SortDirection.ASC));

        final var pageable = factory.createPageable(spec);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "firstName"));
    }

    @Test
    void shouldCreateDescendingSorted() {
        final var factory = new DefaultPageableFactory(10);
        final var spec = new PageSpecDto<>(Optional.of(0), Optional.of(10), Optional.of(PersonSort.LAST_NAME), Optional.of(SortDirection.DESC));

        final var pageable = factory.createPageable(spec);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "lastName"));
    }

    @Test
    void shouldCreateDefaultSorted() {
        final var factory = new DefaultPageableFactory(10);
        final var spec = new PageSpecDto<>(Optional.of(0), Optional.of(10), Optional.of(PersonSort.LAST_NAME), Optional.empty());

        final var pageable = factory.createPageable(spec);

        assertThat(pageable).isNotNull();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.DEFAULT_DIRECTION, "lastName"));
    }


    @Data
    private static class Person {
        private String firstName;
        private String lastName;
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
}