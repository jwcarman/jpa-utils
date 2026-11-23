package org.jwcarman.jpa.spring.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Search with Hibernate set to write values into the SQL text ({@code value_handling_mode=INLINE}) instead of binding
 * them. Not recommended for browser input: safety then rests on Hibernate quoting each literal the way the database
 * reads it. These tests pin that it does on H2, and that escaping still finds literal characters.
 */
@DataJpaTest(showSql = false, properties = {
        "spring.jpa.properties.hibernate.criteria.value_handling_mode=INLINE",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=org.jwcarman.jpa.spring.search.CapturingStatementInspector"
})
@ContextConfiguration(classes = {SpringBootConfig.class})
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchableInlineValuesTest {

    @Autowired
    private LocationRepository locations;

    @BeforeEach
    void setUp() {
        locations.save(new Location("O'Hare", "Chicago", "IL"));
        locations.save(new Location("back\\slash", "b\\x", "a\\b"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"' OR '1'='1", "'; DROP TABLE location; --", "\\'||1--", "\\' OR 1=1 --",
            "'/**/OR/**/1=1--", "%' OR '1'='1", "il' --", "a\\b' OR 'x'='x"})
    void matches_nothing_and_changes_nothing(String term) {
        assertThat(locations.search(term, Pageable.unpaged())).isEmpty();
        assertThat(locations.count()).isEqualTo(2);
    }

    @Test
    void really_writes_the_words_into_the_sql() {
        CapturingStatementInspector.clear();

        locations.search("zqx", Pageable.unpaged());

        // Otherwise a mistyped or renamed property would leave every test here passing under the default BIND.
        assertThat(CapturingStatementInspector.statements())
                .anyMatch(sql -> sql.startsWith("select") && sql.contains("'%zqx%'"));
    }

    @Test
    void still_finds_a_quote_literally() {
        assertThat(locations.search("o'h", Pageable.unpaged())).extracting(Location::getName).containsExactly("O'Hare");
    }

    @Test
    void still_finds_a_backslash_literally_in_every_mode() {
        assertThat(locations.search("k\\s b\\ a\\b", Pageable.unpaged())).extracting(Location::getName)
                .containsExactly("back\\slash");
    }
}
