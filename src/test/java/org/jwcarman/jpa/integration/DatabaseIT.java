package org.jwcarman.jpa.integration;

import org.jwcarman.jpa.pagination.PageSpec;
import org.jwcarman.jpa.pagination.SortDirection;
import org.jwcarman.jpa.spring.page.Pageables;
import org.jwcarman.jpa.spring.search.SearchableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for pagination, sorting, and search functionality with real databases.
 * <p>
 * This test verifies that the JPA utilities work correctly with different database dialects
 * and SQL implementations. It tests end-to-end functionality including:
 * </p>
 * <ul>
 *   <li>Entity persistence and retrieval</li>
 *   <li>Pagination with different page sizes</li>
 *   <li>Sorting by different fields and directions</li>
 *   <li>Case-insensitive search across multiple fields</li>
 *   <li>SQL wildcard escaping</li>
 * </ul>
 */
@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = {TestConfig.class})
public abstract class DatabaseIT {

    @Autowired
    protected TestPersonRepository personRepository;

    @BeforeEach
    void setUp() {
        personRepository.deleteAll();

        // Create test data with predictable ordering
        personRepository.save(new TestPerson("Alice", "Anderson", "alice@example.com"));
        personRepository.save(new TestPerson("Bob", "Brown", "bob@example.com"));
        personRepository.save(new TestPerson("Charlie", "Chen", "charlie@example.com"));
        personRepository.save(new TestPerson("Diana", "Davis", "diana@example.com"));
        personRepository.save(new TestPerson("Eve", "Evans", "eve@example.com"));
        personRepository.save(new TestPerson("Frank", "Fisher", "frank@example.com"));
        personRepository.save(new TestPerson("Grace", "Garcia", "grace@example.com"));
        personRepository.save(new TestPerson("Henry", "Harris", "henry@example.com"));
        personRepository.save(new TestPerson("Ivy", "Ibrahim", "ivy@example.com"));
        personRepository.save(new TestPerson("Jack", "Jackson", "jack@example.com"));

        // Add some with special characters for wildcard testing
        personRepository.save(new TestPerson("TestUser", "WithUnderscore", "test_user@example.com"));
        personRepository.save(new TestPerson("TestUser2", "WithPercent", "test%user@example.com"));
    }

    @Test
    void shouldPaginateResults() {
        Pageable pageable = Pageables.pageableOf(
                new TestPageSpec(0, 5, null, null),
                TestPersonSort.class
        );

        Page<TestPerson> page = personRepository.findAll(pageable);

        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(12);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isFalse();
    }

    @Test
    void shouldSortByFirstNameAscending() {
        Pageable pageable = Pageables.pageableOf(
                new TestPageSpec(0, 20, "FIRST_NAME", SortDirection.ASC),
                TestPersonSort.class
        );

        Page<TestPerson> page = personRepository.findAll(pageable);

        assertThat(page.getContent()).hasSize(12);
        assertThat(page.getContent().get(0).getFirstName()).isEqualTo("Alice");
        assertThat(page.getContent().get(1).getFirstName()).isEqualTo("Bob");
    }

    @Test
    void shouldSortByLastNameDescending() {
        Pageable pageable = Pageables.pageableOf(
                new TestPageSpec(0, 20, "LAST_NAME", SortDirection.DESC),
                TestPersonSort.class
        );

        Page<TestPerson> page = personRepository.findAll(pageable);

        assertThat(page.getContent()).hasSize(12);
        // Verify descending order with known data
        assertThat(page.getContent().get(0).getLastName()).isEqualTo("WithUnderscore");
        assertThat(page.getContent().get(1).getLastName()).isEqualTo("WithPercent");
        assertThat(page.getContent().get(2).getLastName()).isEqualTo("Jackson");
    }

    @ParameterizedTest
    @CsvSource({
            "alice,              firstName, Alice",
            "brown,              lastName,  Brown",
            "charlie@example.com, email,     charlie@example.com",
            "DIANA,              firstName, Diana"
    })
    void shouldSearchAndFindExactMatch(String searchTerm, String fieldName, String expectedValue) {
        Page<TestPerson> page = personRepository.search(searchTerm, Pageable.unpaged());

        assertThat(page.getTotalElements()).isEqualTo(1);

        TestPerson result = page.getContent().get(0);
        String actualValue = switch (fieldName) {
            case "firstName" -> result.getFirstName();
            case "lastName" -> result.getLastName();
            case "email" -> result.getEmail();
            default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
        };

        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Test
    void shouldSearchWithPartialMatch() {
        Page<TestPerson> page = personRepository.search("ar", Pageable.unpaged());

        // Should match "Charlie", "Garcia", "Harris"
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    @ParameterizedTest
    @CsvSource({
            "test_user, test_user@example.com",
            "test%user, test%user@example.com"
    })
    void shouldEscapeWildcardCharacters(String searchTerm, String expectedEmail) {
        Page<TestPerson> page = personRepository.search(searchTerm, Pageable.unpaged());

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getEmail()).isEqualTo(expectedEmail);
    }

    @Test
    void shouldCombineSearchAndPagination() {
        Pageable pageable = Pageables.pageableOf(
                new TestPageSpec(0, 5, "FIRST_NAME", SortDirection.ASC),
                TestPersonSort.class
        );

        Page<TestPerson> page = personRepository.search("e", pageable);

        // Should find names containing 'e' (Alice, Charlie, Eve, Grace, Henry, etc.)
        assertThat(page.getTotalElements()).isGreaterThan(0);
        assertThat(page.getContent()).hasSizeLessThanOrEqualTo(5);

        // Should be sorted by first name ascending
        for (int i = 0; i < page.getContent().size() - 1; i++) {
            String current = page.getContent().get(i).getFirstName();
            String next = page.getContent().get(i + 1).getFirstName();
            assertThat(current.compareTo(next)).isLessThanOrEqualTo(0);
        }
    }

    @Test
    void shouldHandleEmptySearchResults() {
        Page<TestPerson> page = personRepository.search("nonexistent", Pageable.unpaged());

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void shouldHandleNullSearch() {
        Page<TestPerson> page = personRepository.search(null, Pageable.unpaged());

        // Null search should return all results
        assertThat(page.getTotalElements()).isEqualTo(12);
    }

    @Test
    void shouldRespectMaxPageSize() {
        Pageable pageable = Pageables.pageableOf(
                new TestPageSpec(0, 5000, null, null),
                TestPersonSort.class
        );

        // Should be clamped to MAX_PAGE_SIZE (1000)
        assertThat(pageable.getPageSize()).isEqualTo(Pageables.MAX_PAGE_SIZE);
    }

    @Test
    void shouldHandleMultiplePageNavigation() {
        // First page
        Pageable firstPage = Pageables.pageableOf(
                new TestPageSpec(0, 3, "FIRST_NAME", SortDirection.ASC),
                TestPersonSort.class
        );
        Page<TestPerson> page1 = personRepository.findAll(firstPage);

        assertThat(page1.getContent()).hasSize(3);
        assertThat(page1.hasNext()).isTrue();

        // Second page
        Pageable secondPage = Pageables.pageableOf(
                new TestPageSpec(1, 3, "FIRST_NAME", SortDirection.ASC),
                TestPersonSort.class
        );
        Page<TestPerson> page2 = personRepository.findAll(secondPage);

        assertThat(page2.getContent()).hasSize(3);
        assertThat(page2.hasPrevious()).isTrue();

        // Verify no overlap
        assertThat(page1.getContent()).doesNotContainAnyElementsOf(page2.getContent());
    }

    record TestPageSpec(
            Integer pageIndex,
            Integer pageSize,
            String sortBy,
            SortDirection sortDirection
    ) implements PageSpec {
    }
}
