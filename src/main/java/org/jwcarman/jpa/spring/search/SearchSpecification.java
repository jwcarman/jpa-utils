package org.jwcarman.jpa.spring.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A specification for searching.
 * @param <E> The entity type
 */
public class SearchSpecification<E> implements Specification<E> {

    private final String pattern;

    public SearchSpecification(String searchTerm) {
        this.pattern = "%" + sanitizeSearchTerm(searchTerm).toLowerCase() + "%";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Predicate toPredicate(Root<E> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return builder.or(
                root.getModel().getSingularAttributes().stream()
                        .filter(attribute -> String.class.equals(attribute.getJavaType()))
                        .filter(attribute -> switch (attribute.getJavaMember()) {
                            case Method m -> m.isAnnotationPresent(Searchable.class);
                            case Field f -> f.isAnnotationPresent(Searchable.class);
                            default -> false;
                        })
                        .map(attribute -> builder.like(builder.lower(root.get((SingularAttribute<? super E, String>) attribute)), pattern, '\\'))
                        .toArray(Predicate[]::new)
        );
    }

    private static String sanitizeSearchTerm(String searchTerm) {
        return searchTerm
                .replace("\\", "\\\\")  // Escape the escape character
                .replace("%", "\\%")     // Escape the % wildcard
                .replace("_", "\\_");    // Escape the _ wildcard
    }
}