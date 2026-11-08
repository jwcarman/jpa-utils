package org.jwcarman.jpa.spring.web;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.jwcarman.jpa.pagination.SortDirection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SortDirectionConverterTest {

    private final SortDirectionConverter converter = new SortDirectionConverter();

    @ParameterizedTest
    @CsvSource({"ASC,ASC", "asc,ASC", "Asc,ASC", "' asc ',ASC", "DESC,DESC", "desc,DESC", "dEsC,DESC"})
    void converts_any_case_and_surrounding_whitespace(String source, SortDirection expected) {
        assertThat(converter.convert(source)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void converts_a_blank_value_to_null(String source) {
        assertThat(converter.convert(source)).isNull();
    }

    @Test
    void rejects_a_value_that_is_not_a_direction() {
        assertThatThrownBy(() -> converter.convert("down"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown sort direction \"down\", expecting one of ASC, DESC (case-insensitive).");
    }
}
