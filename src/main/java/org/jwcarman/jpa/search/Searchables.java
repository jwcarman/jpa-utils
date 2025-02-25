package org.jwcarman.jpa.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@UtilityClass
public class Searchables {

// -------------------------- STATIC METHODS --------------------------

    public static Predicate createSearchPredicate(String searchTerm, Root<?> root, CriteriaBuilder builder) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return builder.conjunction(); // No filtering if searchTerm is empty/null
        }

        final String pattern = "%" + sanitizeSearchTerm(searchTerm).toLowerCase() + "%";

        final var attributes = root.getModel().getSingularAttributes().stream()
                .filter(attribute -> String.class.equals(attribute.getJavaType()))
                .filter(Searchables::isSearchable)
                .toList();

        if (attributes.isEmpty()) {
            return builder.conjunction();
        } else {
            return builder.or(attributes.stream()
                    .map(attribute -> like(root, builder, attribute, pattern))
                    .toArray(Predicate[]::new));
        }
    }

    private static Predicate like(Root<?> root, CriteriaBuilder builder, SingularAttribute<?, ?> attribute, String pattern) {
        final var path = root.get(attribute.getName()).as(String.class);
        final var lower = builder.lower(path);
        return builder.like(lower, pattern, '\\');
    }

    private static boolean isSearchable(SingularAttribute<?, ?> attribute) {
        return switch (attribute.getJavaMember()) {
            case Method m -> m.isAnnotationPresent(Searchable.class);
            case Field f -> f.isAnnotationPresent(Searchable.class);
            default -> false;
        };
    }

    private static String sanitizeSearchTerm(String searchTerm) {
        return searchTerm
                .replace("\\", "\\\\")  // Escape backslash
                .replace("%", "\\%")    // Escape wildcard '%'
                .replace("_", "\\_");   // Escape wildcard '_'
    }
}
