package org.jwcarman.jpa.search;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.jwcarman.jpa.search.Searchables.SearchableAttribute;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Member;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchablesTest {

    @Nested
    class Search_predicate {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t \n"})
        void is_the_conjunction_for_a_null_or_blank_term(String term) {
            CriteriaBuilder builder = mock(CriteriaBuilder.class);
            Predicate conjunction = mock(Predicate.class);
            when(builder.conjunction()).thenReturn(conjunction);

            assertThat(Searchables.createSearchPredicate(term, mock(Root.class), builder)).isSameAs(conjunction);
            verify(builder, never()).or(any(Predicate[].class));
        }

        @Test
        void is_the_conjunction_when_a_string_attribute_has_no_java_member() {
            assertNoSearchableAttributes(null);
        }

        @Test
        void is_the_conjunction_when_a_string_attribute_s_member_is_neither_a_field_nor_a_method() {
            assertNoSearchableAttributes(mock(Member.class));
        }

        private static void assertNoSearchableAttributes(Member javaMember) {
            EntityType<?> entityType = entityType(attribute("name", String.class, javaMember));
            Root<?> root = mock(Root.class);
            doReturn(entityType).when(root).getModel();

            CriteriaBuilder builder = mock(CriteriaBuilder.class);
            Predicate conjunction = mock(Predicate.class);
            when(builder.conjunction()).thenReturn(conjunction);

            assertThat(Searchables.createSearchPredicate("term", root, builder)).isSameAs(conjunction);
            verify(builder, never()).or(any(Predicate[].class));
        }
    }

    @Nested
    class Searchable_attributes {

        private final Logger logger = (Logger) LoggerFactory.getLogger(Searchables.class);
        private final ListAppender<ILoggingEvent> logged = new ListAppender<>();

        @BeforeEach
        void captureLogging() {
            logged.start();
            logger.addAppender(logged);
        }

        @AfterEach
        void releaseLogging() {
            logger.detachAppender(logged);
        }

        @Test
        void include_an_annotated_string_field_with_its_match_mode() throws NoSuchFieldException {
            EntityType<?> type = entityType(attribute("city", String.class, Sample.class.getDeclaredField("city")));

            assertThat(Searchables.searchableAttributesOf(type))
                    .containsExactly(new SearchableAttribute("city", MatchMode.PREFIX));
        }

        @Test
        void include_an_annotated_string_getter_with_its_match_mode() throws NoSuchMethodException {
            EntityType<?> type = entityType(attribute("title", String.class, Sample.class.getDeclaredMethod("getTitle")));

            assertThat(Searchables.searchableAttributesOf(type))
                    .containsExactly(new SearchableAttribute("title", MatchMode.EXACT));
        }

        @Test
        void exclude_an_annotated_attribute_that_is_not_a_string() throws NoSuchFieldException {
            EntityType<?> type = entityType(attribute("count", Integer.class, Sample.class.getDeclaredField("count")));

            assertThat(Searchables.searchableAttributesOf(type)).isEmpty();
        }

        @Test
        void exclude_an_unannotated_string_attribute() throws NoSuchFieldException {
            EntityType<?> type = entityType(attribute("plain", String.class, Sample.class.getDeclaredField("plain")));

            assertThat(Searchables.searchableAttributesOf(type)).isEmpty();
        }

        @Test
        void are_reported_with_a_warning_when_there_are_none() throws NoSuchFieldException {
            Searchables.searchableAttributesOf(
                    entityType(attribute("plain", String.class, Sample.class.getDeclaredField("plain"))));

            assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage)
                    .containsExactly("No searchable attributes found for Sample");
        }

        @Test
        void are_not_reported_when_there_are_some() throws NoSuchFieldException {
            Searchables.searchableAttributesOf(
                    entityType(attribute("city", String.class, Sample.class.getDeclaredField("city"))));

            assertThat(logged.list).isEmpty();
        }
    }

    @Nested
    class Words_of_a_search_term {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t \n"})
        void are_none_for_a_null_or_blank_term(String term) {
            assertThat(Searchables.wordsOf(term)).isEmpty();
        }

        @Test
        void split_on_any_run_of_whitespace() {
            assertThat(Searchables.wordsOf("  john \t smith\n")).containsExactly("john", "smith");
        }

        @Test
        void are_lowercased() {
            assertThat(Searchables.wordsOf("John SMITH")).containsExactly("john", "smith");
        }

        @Test
        void drop_duplicates_keeping_the_first_occurrence() {
            assertThat(Searchables.wordsOf("smith John smith")).containsExactly("smith", "john");
        }

        @Test
        void stop_at_the_maximum() {
            assertThat(Searchables.wordsOf("w1 w2 w3 w4 w5 w6 w7 w8 w9 w10 w11 w12"))
                    .hasSize(Searchables.MAX_SEARCH_WORDS)
                    .startsWith("w1")
                    .endsWith("w10");
        }

        @Test
        void keep_wildcard_characters_for_escaping_later() {
            assertThat(Searchables.wordsOf("50% off_")).containsExactly("50%", "off_");
        }

        @ParameterizedTest
        @ValueSource(strings = {"john smith", "john smith", "john　smith", "john smith", "john\u0085smith"})
        void split_on_unicode_spaces_and_line_separators(String term) {
            assertThat(Searchables.wordsOf(term)).containsExactly("john", "smith");
        }

        @ParameterizedTest
        @ValueSource(strings = {"john\u0000smith", "john\u001Bsmith", "john​smith", "﻿john​smith​"})
        void split_on_control_and_invisible_format_characters(String term) {
            assertThat(Searchables.wordsOf(term)).containsExactly("john", "smith");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "می‌خواهم",  // Persian "I want": a zero-width non-joiner is part of the word
                "क्‍ष",                          // Devanagari with a zero-width joiner
                "👩‍💻"                     // an emoji sequence joined by a zero-width joiner
        })
        void keep_zero_width_joiners_inside_a_word(String word) {
            assertThat(Searchables.wordsOf(word)).containsExactly(word);
        }

        @ParameterizedTest
        @ValueSource(strings = {"\u0000", "​", "﻿", " ", "​ ﻿\u0000"})
        void are_none_for_a_term_of_only_separators(String term) {
            assertThat(Searchables.wordsOf(term)).isEmpty();
        }

        @Test
        void are_cut_to_the_maximum_length() {
            assertThat(Searchables.wordsOf("x".repeat(10_000)))
                    .containsExactly("x".repeat(Searchables.MAX_WORD_LENGTH));
        }

        @Test
        void are_cut_by_code_point_so_no_surrogate_pair_is_split() {
            String emoji = "😀";

            assertThat(Searchables.wordsOf(emoji.repeat(Searchables.MAX_WORD_LENGTH + 1)))
                    .containsExactly(emoji.repeat(Searchables.MAX_WORD_LENGTH));
        }

        @Test
        void keep_a_word_of_exactly_the_maximum_length() {
            assertThat(Searchables.wordsOf("y".repeat(Searchables.MAX_WORD_LENGTH)))
                    .containsExactly("y".repeat(Searchables.MAX_WORD_LENGTH));
        }

        @Test
        void drop_duplicates_after_cutting() {
            String longWord = "z".repeat(Searchables.MAX_WORD_LENGTH);

            assertThat(Searchables.wordsOf(longWord + "a " + longWord + "b")).containsExactly(longWord);
        }
    }

    private static SingularAttribute<?, ?> attribute(String name, Class<?> javaType, Member javaMember) {
        SingularAttribute<?, ?> attribute = mock(SingularAttribute.class);
        doReturn(name).when(attribute).getName();
        doReturn(javaType).when(attribute).getJavaType();
        doReturn(javaMember).when(attribute).getJavaMember();
        return attribute;
    }

    private static EntityType<?> entityType(SingularAttribute<?, ?>... attributes) {
        EntityType<?> entityType = mock(EntityType.class);
        doReturn(new LinkedHashSet<>(List.of(attributes))).when(entityType).getSingularAttributes();
        doReturn("Sample").when(entityType).getName();
        return entityType;
    }

    /**
     * Supplies real annotated members; never instantiated.
     */
    static class Sample {

        @Searchable(match = MatchMode.PREFIX)
        String city;

        @Searchable
        Integer count;

        String plain;

        String title;

        @Searchable(match = MatchMode.EXACT)
        String getTitle() {
            return title;
        }
    }
}
