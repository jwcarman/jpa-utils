package org.jwcarman.jpa.spring.page;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jwcarman.jpa.pagination.PageDto;
import org.jwcarman.jpa.pagination.PageSpec;
import org.jwcarman.jpa.pagination.SortDirection;
import org.jwcarman.jpa.pagination.SortPropertyProvider;
import org.jwcarman.jpa.pagination.UnknownSortByValueException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jwcarman.jpa.spring.page.Paginator.DEFAULT_MAX_PAGE_SIZE;
import static org.jwcarman.jpa.spring.page.Paginator.DEFAULT_PAGE_SIZE;
import static org.jwcarman.jpa.spring.page.Paginator.FIRST_PAGE;
import static org.springframework.data.domain.Sort.Order.asc;
import static org.springframework.data.domain.Sort.Order.desc;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PaginatorTest {

    private static final Paginator DEFAULTS = Paginator.of(Person.class, PersonSort.class);

    private static PageSpec spec(Integer pageIndex, Integer pageSize, String sortBy, SortDirection sortDirection) {
        return new TestPageSpec(pageIndex, pageSize, sortBy, sortDirection);
    }

    private static List<Sort.Order> ordersOf(Pageable pageable) {
        return pageable.getSort().toList();
    }

    @Nested
    class Sorting {

        @Test
        void sorts_ascending_by_the_requested_property_then_the_id_tiebreaker() {
            Pageable pageable = DEFAULTS.pageableOf(spec(0, 10, "FIRST_NAME", SortDirection.ASC));

            assertThat(ordersOf(pageable)).containsExactly(asc("firstName"), asc("id"));
        }

        @Test
        void sorts_descending_by_the_requested_property_with_an_ascending_tiebreaker() {
            Pageable pageable = DEFAULTS.pageableOf(spec(0, 10, "LAST_NAME", SortDirection.DESC));

            assertThat(ordersOf(pageable)).containsExactly(desc("lastName"), asc("id"));
        }

        @Test
        void sorts_ascending_when_no_direction_is_given() {
            Pageable pageable = DEFAULTS.pageableOf(spec(0, 10, "FIRST_NAME", null));

            assertThat(ordersOf(pageable)).containsExactly(asc("firstName"), asc("id"));
        }

        @Test
        void sorts_by_the_tiebreaker_alone_when_no_sort_by_is_given() {
            Pageable pageable = DEFAULTS.pageableOf(spec(0, 10, null, null));

            assertThat(ordersOf(pageable)).containsExactly(asc("id"));
        }

        @Test
        void does_not_repeat_the_tiebreaker_when_the_request_sorts_by_it() {
            Pageable pageable = DEFAULTS.pageableOf(spec(0, 10, "ID", SortDirection.DESC));

            assertThat(ordersOf(pageable)).containsExactly(desc("id"));
        }

        @Test
        void rejects_an_unknown_sort_by_value() {
            PageSpec spec = spec(0, 10, "INVALID_FIELD", SortDirection.ASC);

            assertThatThrownBy(() -> DEFAULTS.pageableOf(spec))
                    .isInstanceOf(UnknownSortByValueException.class)
                    .hasMessageContaining("INVALID_FIELD")
                    .satisfies(e -> assertThat(((UnknownSortByValueException) e).getExpectedValues())
                            .containsExactly("FIRST_NAME", "LAST_NAME", "ID"));
        }

        @Test
        void ignores_whitespace_around_sort_by() {
            Pageable pageable = DEFAULTS.pageableOf(spec(0, 10, "  LAST_NAME ", SortDirection.DESC));

            assertThat(ordersOf(pageable)).containsExactly(desc("lastName"), asc("id"));
        }

        @Test
        void treats_an_empty_sort_by_as_absent() {
            assertThat(ordersOf(DEFAULTS.pageableOf(spec(0, 10, "", null)))).containsExactly(asc("id"));
        }

        @Test
        void treats_a_blank_sort_by_as_absent() {
            assertThat(ordersOf(DEFAULTS.pageableOf(spec(0, 10, " \t ", null)))).containsExactly(asc("id"));
        }

        @Test
        void matches_sort_by_values_case_sensitively() {
            PageSpec spec = spec(0, 10, "first_name", SortDirection.ASC);

            assertThatThrownBy(() -> DEFAULTS.pageableOf(spec))
                    .isInstanceOf(UnknownSortByValueException.class);
        }
    }

    @Nested
    class Tiebreaker {

        @Test
        void uses_a_configured_property() {
            Paginator paginator = Paginator.of(Member.class, PersonSort.class, cfg -> cfg.tiebreaker("identityId"));

            Pageable pageable = paginator.pageableOf(spec(0, 10, "FIRST_NAME", SortDirection.ASC));

            assertThat(ordersOf(pageable)).containsExactly(asc("firstName"), asc("identityId"));
        }

        @Test
        void uses_every_property_of_a_composite_tiebreaker_in_order() {
            Paginator paginator = Paginator.of(LineItem.class, PersonSort.class, cfg -> cfg.tiebreaker("orderId", "lineNo"));

            Pageable pageable = paginator.pageableOf(spec(0, 10, "LAST_NAME", SortDirection.DESC));

            assertThat(ordersOf(pageable)).containsExactly(desc("lastName"), asc("orderId"), asc("lineNo"));
        }

        @Test
        void skips_composite_properties_the_request_already_sorts_by() {
            Paginator paginator = Paginator.of(Person.class, PersonSort.class, cfg -> cfg.tiebreaker("lastName", "id"));

            Pageable pageable = paginator.pageableOf(spec(0, 10, "LAST_NAME", SortDirection.DESC));

            assertThat(ordersOf(pageable)).containsExactly(desc("lastName"), asc("id"));
        }

        @Test
        void can_be_disabled() {
            Paginator paginator = Paginator.of(Person.class, PersonSort.class, Paginator.Config::noTiebreaker);

            assertThat(ordersOf(paginator.pageableOf(spec(0, 10, "FIRST_NAME", SortDirection.ASC))))
                    .containsExactly(asc("firstName"));
            assertThat(paginator.pageableOf(spec(0, 10, null, null)).getSort().isUnsorted()).isTrue();
        }

        @Test
        void rejects_an_empty_property_list() {
            assertThatThrownBy(() -> Paginator.of(Person.class, PersonSort.class, cfg -> cfg.tiebreaker()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("noTiebreaker()");
        }

        @Test
        void rejects_a_blank_property() {
            assertThatThrownBy(() -> Paginator.of(Person.class, PersonSort.class, cfg -> cfg.tiebreaker("id", " ")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_null_property() {
            assertThatThrownBy(() -> Paginator.of(Person.class, PersonSort.class, cfg -> cfg.tiebreaker("id", null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Default_sort {

        private final Paginator paginator = Paginator.of(Person.class, PersonSort.class, cfg -> cfg.defaultSort(PersonSort.LAST_NAME));

        @Test
        void is_used_when_no_sort_by_is_given() {
            Pageable pageable = paginator.pageableOf(spec(0, 10, null, null));

            assertThat(ordersOf(pageable)).containsExactly(asc("lastName"), asc("id"));
        }

        @Test
        void is_used_when_sort_by_is_blank() {
            Pageable pageable = paginator.pageableOf(spec(0, 10, "  ", SortDirection.DESC));

            assertThat(ordersOf(pageable)).containsExactly(desc("lastName"), asc("id"));
        }

        @Test
        void follows_the_requested_direction() {
            Pageable pageable = paginator.pageableOf(spec(0, 10, null, SortDirection.DESC));

            assertThat(ordersOf(pageable)).containsExactly(desc("lastName"), asc("id"));
        }

        @Test
        void is_overridden_by_an_explicit_sort_by() {
            Pageable pageable = paginator.pageableOf(spec(0, 10, "FIRST_NAME", SortDirection.ASC));

            assertThat(ordersOf(pageable)).containsExactly(asc("firstName"), asc("id"));
        }

        @Test
        void is_not_a_fallback_for_an_unknown_sort_by() {
            PageSpec spec = spec(0, 10, "BOGUS", SortDirection.ASC);

            assertThatThrownBy(() -> paginator.pageableOf(spec))
                    .isInstanceOf(UnknownSortByValueException.class);
        }

        @Test
        void is_used_for_a_null_spec() {
            assertThat(ordersOf(paginator.pageableOf(null))).containsExactly(asc("lastName"), asc("id"));
        }

        @Test
        void rejects_null() {
            assertThatThrownBy(() -> Paginator.of(Person.class, PersonSort.class, cfg -> cfg.defaultSort(null)))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Page_number {

        @Test
        void defaults_to_the_first_page_when_missing() {
            assertThat(DEFAULTS.pageableOf(spec(null, 10, null, null)).getPageNumber()).isEqualTo(FIRST_PAGE);
        }

        @Test
        void clamps_a_negative_index_to_the_first_page() {
            assertThat(DEFAULTS.pageableOf(spec(-5, 10, null, null)).getPageNumber()).isEqualTo(FIRST_PAGE);
        }

        @Test
        void uses_the_requested_index() {
            Pageable pageable = DEFAULTS.pageableOf(spec(3, 10, null, null));

            assertThat(pageable.getPageNumber()).isEqualTo(3);
            assertThat(pageable.getOffset()).isEqualTo(30);
        }
    }

    @Nested
    class Page_size {

        @Test
        void defaults_when_missing() {
            assertThat(DEFAULTS.pageableOf(spec(0, null, null, null)).getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE);
        }

        @Test
        void uses_a_configured_default_when_missing() {
            Paginator paginator = Paginator.of(Person.class, PersonSort.class, cfg -> cfg.defaultPageSize(25));

            assertThat(paginator.pageableOf(spec(0, null, null, null)).getPageSize()).isEqualTo(25);
        }

        @Test
        void clamps_zero_to_one() {
            assertThat(DEFAULTS.pageableOf(spec(0, 0, null, null)).getPageSize()).isEqualTo(1);
        }

        @Test
        void clamps_a_negative_size_to_one() {
            assertThat(DEFAULTS.pageableOf(spec(0, -10, null, null)).getPageSize()).isEqualTo(1);
        }

        @Test
        void allows_exactly_the_maximum() {
            assertThat(DEFAULTS.pageableOf(spec(0, DEFAULT_MAX_PAGE_SIZE, null, null)).getPageSize())
                    .isEqualTo(DEFAULT_MAX_PAGE_SIZE);
        }

        @Test
        void defaults_the_maximum_to_one_hundred() {
            assertThat(DEFAULT_MAX_PAGE_SIZE).isEqualTo(100);
            assertThat(DEFAULTS.pageableOf(spec(0, 101, null, null)).getPageSize()).isEqualTo(100);
        }

        @Test
        void allows_a_raised_maximum() {
            Paginator paginator = Paginator.of(Person.class, PersonSort.class, cfg -> cfg.maxPageSize(1000));

            assertThat(paginator.pageableOf(spec(0, 1000, null, null)).getPageSize()).isEqualTo(1000);
        }

        @Test
        void clamps_to_the_maximum() {
            assertThat(DEFAULTS.pageableOf(spec(0, Integer.MAX_VALUE, null, null)).getPageSize())
                    .isEqualTo(DEFAULT_MAX_PAGE_SIZE);
        }

        @Test
        void clamps_to_a_configured_maximum() {
            Paginator paginator = Paginator.of(Person.class, PersonSort.class, cfg -> cfg.defaultPageSize(10).maxPageSize(50));

            assertThat(paginator.pageableOf(spec(0, 51, null, null)).getPageSize()).isEqualTo(50);
        }

        @Test
        void rejects_a_default_below_one() {
            assertThatThrownBy(() -> Paginator.of(Person.class, PersonSort.class, cfg -> cfg.defaultPageSize(0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("defaultPageSize");
        }

        @Test
        void rejects_a_default_above_the_maximum() {
            assertThatThrownBy(() -> Paginator.of(Person.class, PersonSort.class, cfg -> cfg.maxPageSize(10)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("defaultPageSize");
        }

        @Test
        void rejects_a_maximum_below_one() {
            assertThatThrownBy(() -> Paginator.of(Person.class, PersonSort.class, cfg -> cfg.maxPageSize(0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxPageSize");
        }
    }

    @Nested
    class Null_spec {

        @Test
        void requests_the_first_page_with_the_default_size_and_the_tiebreaker() {
            Pageable pageable = DEFAULTS.pageableOf(null);

            assertThat(pageable.getPageNumber()).isEqualTo(FIRST_PAGE);
            assertThat(pageable.getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE);
            assertThat(ordersOf(pageable)).containsExactly(asc("id"));
        }

        @Test
        void uses_a_configured_default_size() {
            Paginator paginator = Paginator.of(Person.class, PersonSort.class, cfg -> cfg.defaultPageSize(7));

            assertThat(paginator.pageableOf(null).getPageSize()).isEqualTo(7);
        }
    }

    @Nested
    class Creation {

        @Test
        void rejects_a_null_entity_type() {
            assertThatThrownBy(() -> Paginator.of(null, PersonSort.class))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("entityType");
        }

        @Test
        void rejects_a_null_sort_enum_class() {
            assertThatThrownBy(() -> Paginator.<PersonSort>of(Person.class, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("sortEnumClass");
        }

        @Test
        void rejects_a_null_customizer() {
            assertThatThrownBy(() -> Paginator.of(Person.class, PersonSort.class, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("customizer");
        }
    }

    @Nested
    class Property_validation {

        @Test
        void accepts_properties_inherited_from_a_superclass() {
            Paginator paginator = Paginator.of(Member.class, PersonSort.class);

            assertThat(ordersOf(paginator.pageableOf(spec(0, 10, "LAST_NAME", null))))
                    .containsExactly(asc("lastName"), asc("id"));
        }

        @Test
        void accepts_nested_property_paths() {
            Paginator paginator = Paginator.of(Customer.class, CustomerSort.class);

            assertThat(ordersOf(paginator.pageableOf(spec(0, 10, "CITY", null))))
                    .containsExactly(asc("address.city"), asc("id"));
        }

        @Test
        void rejects_a_sort_property_the_entity_does_not_have() {
            assertThatThrownBy(() -> Paginator.of(Person.class, TypoSort.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("TypoSort.LAST_NAME sorts by \"lastNmae\"")
                    .hasMessageContaining(Person.class.getName())
                    .hasMessageContaining("skipPropertyValidation()");
        }

        @Test
        void checks_every_constant_not_just_the_requested_one() {
            assertThatThrownBy(() -> Paginator.of(Person.class, MostlyGoodSort.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("MostlyGoodSort.EMAIL sorts by \"email\"")
                    .hasMessageNotContaining("FIRST_NAME")
                    .hasMessageNotContaining("LAST_NAME");
        }

        @Test
        void rejects_a_nested_path_that_does_not_resolve() {
            assertThatThrownBy(() -> Paginator.of(Customer.class, BadNestedSort.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("address.zip");
        }

        @Test
        void rejects_a_tiebreaker_the_entity_does_not_have() {
            assertThatThrownBy(() -> Paginator.of(Person.class, PersonSort.class, cfg -> cfg.tiebreaker("identityId")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tiebreaker \"identityId\"")
                    .hasMessageContaining(Person.class.getName());
        }

        @Test
        void rejects_the_default_id_tiebreaker_when_the_entity_has_no_id() {
            assertThatThrownBy(() -> Paginator.of(NoIdView.class, NoIdSort.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tiebreaker \"id\"");
        }

        @Test
        void rejects_a_blank_sort_property() {
            assertThatThrownBy(() -> Paginator.of(Person.class, BlankSort.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("BlankSort.NOTHING has no sort property");
        }

        @Test
        void can_be_skipped_for_queries_that_sort_by_columns() {
            Paginator paginator = Paginator.of(Person.class, TypoSort.class, cfg -> cfg
                    .tiebreaker("person_id")
                    .skipPropertyValidation());

            assertThat(ordersOf(paginator.pageableOf(spec(0, 10, "LAST_NAME", null))))
                    .containsExactly(asc("lastNmae"), asc("person_id"));
        }
    }

    @Nested
    class Page_dto_conversion {

        @Test
        void converts_an_empty_page() {
            PageDto<String> dto = DEFAULTS.pageDtoOf(Page.<String>empty());

            assertThat(dto.data()).isEmpty();
            assertThat(dto.pagination().pageIndex()).isZero();
            assertThat(dto.pagination().pageSize()).isZero();
            assertThat(dto.pagination().totalElementCount()).isZero();
            assertThat(dto.pagination().totalPageCount()).isEqualTo(1L);
            assertThat(dto.pagination().hasNext()).isFalse();
            assertThat(dto.pagination().hasPrevious()).isFalse();
        }

        @Test
        void converts_content_and_pagination() {
            Page<String> page = new PageImpl<>(List.of("d", "e"), PageRequest.of(1, 3), 8);

            PageDto<String> dto = DEFAULTS.pageDtoOf(page);

            assertThat(dto.data()).containsExactly("d", "e");
            assertThat(dto.pagination().pageIndex()).isEqualTo(1);
            assertThat(dto.pagination().pageSize()).isEqualTo(3);
            assertThat(dto.pagination().totalElementCount()).isEqualTo(8L);
            assertThat(dto.pagination().totalPageCount()).isEqualTo(3L);
            assertThat(dto.pagination().hasNext()).isTrue();
            assertThat(dto.pagination().hasPrevious()).isTrue();
        }

        @Test
        void maps_each_element() {
            Page<String> page = new PageImpl<>(List.of("one", "three"), PageRequest.of(0, 2), 5);

            PageDto<Integer> dto = DEFAULTS.pageDtoOf(page, String::length);

            assertThat(dto.data()).containsExactly(3, 5);
            assertThat(dto.pagination().totalElementCount()).isEqualTo(5L);
            assertThat(dto.pagination().hasNext()).isTrue();
        }
    }

    @Nested
    class Paginate {

        @Test
        void passes_the_built_pageable_to_the_query() {
            List<Pageable> received = new ArrayList<>();

            DEFAULTS.paginate(spec(2, 5, "LAST_NAME", SortDirection.DESC), pageable -> {
                received.add(pageable);
                return Page.<String>empty(pageable);
            });

            assertThat(received).singleElement().satisfies(pageable -> {
                assertThat(pageable.getPageNumber()).isEqualTo(2);
                assertThat(pageable.getPageSize()).isEqualTo(5);
                assertThat(ordersOf(pageable)).containsExactly(desc("lastName"), asc("id"));
            });
        }

        @Test
        void converts_the_query_result() {
            PageDto<String> dto = DEFAULTS.paginate(spec(0, 2, null, null),
                    pageable -> new PageImpl<>(List.of("a", "b"), pageable, 3));

            assertThat(dto.data()).containsExactly("a", "b");
            assertThat(dto.pagination().totalPageCount()).isEqualTo(2L);
        }

        @Test
        void maps_the_query_result() {
            PageDto<String> dto = DEFAULTS.paginate(spec(0, 2, null, null),
                    pageable -> new PageImpl<>(List.of(1, 22), pageable, 2),
                    number -> "#" + number);

            assertThat(dto.data()).containsExactly("#1", "#22");
        }

        @Test
        void does_not_run_the_query_for_an_unknown_sort_by() {
            List<Pageable> received = new ArrayList<>();
            PageSpec spec = spec(0, 2, "BOGUS", null);

            assertThatThrownBy(() -> DEFAULTS.paginate(spec, pageable -> {
                received.add(pageable);
                return Page.<String>empty();
            })).isInstanceOf(UnknownSortByValueException.class);
            assertThat(received).isEmpty();
        }
    }

    enum PersonSort implements SortPropertyProvider {
        FIRST_NAME("firstName"),
        LAST_NAME("lastName"),
        ID("id");

        private final String sortProperty;

        PersonSort(String sortProperty) {
            this.sortProperty = sortProperty;
        }

        @Override
        public String getSortProperty() {
            return sortProperty;
        }
    }

    static class Person {
        private UUID id;
        private String firstName;
        private String lastName;
    }

    static class Member extends Person {
        private UUID identityId;
    }

    static class LineItem extends Person {
        private UUID orderId;
        private int lineNo;
    }

    static class Address {
        private String city;
    }

    static class Customer {
        private UUID id;
        private Address address;
    }

    static class NoIdView {
        private String name;
    }

    enum CustomerSort implements SortPropertyProvider {
        CITY;

        @Override
        public String getSortProperty() {
            return "address.city";
        }
    }

    enum BadNestedSort implements SortPropertyProvider {
        ZIP;

        @Override
        public String getSortProperty() {
            return "address.zip";
        }
    }

    enum MostlyGoodSort implements SortPropertyProvider {
        FIRST_NAME("firstName"),
        LAST_NAME("lastName"),
        EMAIL("email");

        private final String sortProperty;

        MostlyGoodSort(String sortProperty) {
            this.sortProperty = sortProperty;
        }

        @Override
        public String getSortProperty() {
            return sortProperty;
        }
    }

    enum TypoSort implements SortPropertyProvider {
        LAST_NAME;

        @Override
        public String getSortProperty() {
            return "lastNmae";
        }
    }

    enum NoIdSort implements SortPropertyProvider {
        NAME;

        @Override
        public String getSortProperty() {
            return "name";
        }
    }

    enum BlankSort implements SortPropertyProvider {
        NOTHING;

        @Override
        public String getSortProperty() {
            return " ";
        }
    }

    record TestPageSpec(Integer pageIndex, Integer pageSize, String sortBy, SortDirection sortDirection)
            implements PageSpec {
    }
}
