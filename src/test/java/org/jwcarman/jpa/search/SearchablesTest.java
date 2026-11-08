package org.jwcarman.jpa.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

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
