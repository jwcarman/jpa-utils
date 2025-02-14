package org.jwcarman.jpa.spring.search;

import org.jwcarman.jpa.spring.page.Pages;
import org.jwcarman.jpa.spring.page.SortProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * A repository that supports searching.
 *
 * @param <E> The entity type
 * @param <I> The id type
 */
public interface SearchableRepository<E, I> extends JpaRepository<E, I>, JpaSpecificationExecutor<E> {

    /**
     * Search for entities using the given specification.
     *
     * @param spec The search specification
     * @return The page of entities
     */
    default <O extends Enum<O> & SortProvider> Page<E> search(SearchSpec spec, Class<O> sortType) {
        final var pageable = Pages.pageableOf(spec, sortType);
        return spec.searchTerm()
                .filter(str -> !str.isEmpty())
                .map(searchTerm -> findAll(new SearchSpecification<>(searchTerm), pageable))
                .orElse(findAll(pageable));
    }

}
