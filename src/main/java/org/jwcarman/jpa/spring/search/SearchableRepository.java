package org.jwcarman.jpa.spring.search;

import org.jwcarman.jpa.search.Searchables;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * A repository that supports searching.
 *
 * @param <S> The entity type
 * @param <I> The id type
 */
@NoRepositoryBean
public interface SearchableRepository<S, I> extends JpaRepository<S, I>, JpaSpecificationExecutor<S> {

// -------------------------- STATIC METHODS --------------------------

    private static <E> Specification<E> searchSpecification(String searchTerm) {
        return (root, _, criteriaBuilder) ->
                Searchables.createSearchPredicate(searchTerm, root, criteriaBuilder);
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Search for entities matching the search term.
     *
     * @param searchTerm the search term to use, can be null or empty
     * @param pageable   the pageable to request a paged result, can be {@link Pageable#unpaged()}, must not be null.
     * @return a page of entities matching the search term (if any) or all entities if the search term is null or empty.
     */
    default Page<S> search(String searchTerm, Pageable pageable) {
        return findAll(searchSpecification(searchTerm), pageable);
    }

}
