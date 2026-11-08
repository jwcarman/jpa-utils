package org.jwcarman.jpa.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Member;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchablesTest {

    @Test
    void skips_a_string_attribute_with_no_java_member() {
        assertNoSearchableAttributes(NoMemberEntity.class, null);
    }

    @Test
    void skips_a_string_attribute_whose_member_is_neither_a_field_nor_a_method() {
        assertNoSearchableAttributes(OtherMemberEntity.class, mock(Member.class));
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
    }

    // Each test uses its own entity class: Searchables caches attribute names per class.
    private static void assertNoSearchableAttributes(Class<?> entityClass, Member javaMember) {
        SingularAttribute<?, ?> attribute = mock(SingularAttribute.class);
        doReturn(String.class).when(attribute).getJavaType();
        doReturn(javaMember).when(attribute).getJavaMember();

        EntityType<?> entityType = mock(EntityType.class);
        doReturn(Set.of(attribute)).when(entityType).getSingularAttributes();
        doReturn(entityClass.getSimpleName()).when(entityType).getName();

        Root<?> root = mock(Root.class);
        doReturn(entityClass).when(root).getJavaType();
        doReturn(entityType).when(root).getModel();

        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Predicate conjunction = mock(Predicate.class);
        when(builder.conjunction()).thenReturn(conjunction);

        assertThat(Searchables.createSearchPredicate("term", root, builder)).isSameAs(conjunction);
        verify(builder, never()).or(any(Predicate[].class));
    }

    static class NoMemberEntity {
    }

    static class OtherMemberEntity {
    }
}
