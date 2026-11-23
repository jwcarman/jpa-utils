package org.jwcarman.jpa.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that PostgreSQL answers {@code PREFIX} and {@code EXACT} searches from indexes on {@code LOWER(column)},
 * and that a single {@code CONTAINS} field forces a table scan.
 * <p>
 * Searches go through the real repository, so the words are bound exactly as in production (Hibernate bind
 * parameters, pgjdbc server-side prepared statements). Index use is read from PostgreSQL's own counters
 * ({@code pg_stat_user_indexes.idx_scan}). Each search runs ten times, past pgjdbc's prepare threshold and
 * PostgreSQL's five custom plans, so a switch to a generic plan that cannot use an index for {@code LIKE ?} would
 * show up as missing index scans.
 * </p>
 */
@Testcontainers
@Tag("integration")
@SpringBootTest(classes = TestConfig.class, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // One connection: the searches and the statistics reads run on the same backend.
        "spring.datasource.hikari.maximum-pool-size=1"
})
// Close the context while the container still runs; left cached, it would close at JVM exit against a stopped
// container and stall the fork's shutdown.
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PostgreSQLIndexIT {

    private static final int ROWS = 20_000;
    private static final int REPETITIONS = 10;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine")
            .withStartupTimeout(Duration.ofSeconds(60))
            .withStartupAttempts(1);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private IndexedPlaceRepository places;

    @Autowired
    private TestPersonRepository people;

    @BeforeAll
    void seedAndIndex() {
        // Cities 'City1'..'City20000'; states 'S1'..'S49', with every 50th row 'OH'.
        final String state = "CASE WHEN n % 50 = 0 THEN 'OH' ELSE 'S' || (n % 50) END";
        jdbc.update("INSERT INTO indexed_place (id, version, city, state) "
                + "SELECT gen_random_uuid(), 0, 'City' || n, " + state
                + " FROM generate_series(1, ?) n", ROWS);
        jdbc.update("INSERT INTO test_person (id, version, first_name, last_name, email, city, state) "
                + "SELECT gen_random_uuid(), 0, 'First' || n, 'Last' || n, 'p' || n || '@example.com', 'City' || n, "
                + state + " FROM generate_series(1, ?) n", ROWS);
        for (String table : new String[]{"indexed_place", "test_person"}) {
            jdbc.execute("CREATE INDEX " + table + "_city_lower ON " + table + " (LOWER(city) text_pattern_ops)");
            jdbc.execute("CREATE INDEX " + table + "_state_lower ON " + table + " (LOWER(state))");
            jdbc.execute("ANALYZE " + table);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"city1234", "oh", "city1234 oh"})
    void uses_both_indexes_when_every_searchable_field_is_indexable(String term) {
        final long city = idxScans("indexed_place_city_lower");
        final long state = idxScans("indexed_place_state_lower");

        for (int i = 0; i < REPETITIONS; i++) {
            places.search(term, Pageable.unpaged());
        }

        assertThat(idxScans("indexed_place_city_lower") - city).isGreaterThanOrEqualTo(REPETITIONS);
        assertThat(idxScans("indexed_place_state_lower") - state).isGreaterThanOrEqualTo(REPETITIONS);
    }

    @Test
    void scans_the_table_when_any_searchable_field_is_contains() {
        final long city = idxScans("test_person_city_lower");
        final long state = idxScans("test_person_state_lower");

        for (int i = 0; i < REPETITIONS; i++) {
            people.search("city1234", Pageable.unpaged());
        }

        assertThat(idxScans("test_person_city_lower")).isEqualTo(city);
        assertThat(idxScans("test_person_state_lower")).isEqualTo(state);
    }

    private long idxScans(String indexName) {
        // This backend's pending statistics reach shared memory only when flushed; force it, then read.
        jdbc.execute("SELECT pg_stat_force_next_flush()");
        final Long scans = jdbc.queryForObject(
                "SELECT idx_scan FROM pg_stat_user_indexes WHERE indexrelname = ?", Long.class, indexName);
        assertThat(scans).as("index %s exists", indexName).isNotNull();
        return scans;
    }
}
