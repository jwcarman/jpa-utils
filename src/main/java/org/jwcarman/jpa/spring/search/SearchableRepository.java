/*
 * Copyright © 2025 James Carman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
public interface SearchableRepository<S, I>
    extends JpaRepository<S, I>, JpaSpecificationExecutor<S> {

  // -------------------------- OTHER METHODS --------------------------

  /**
   * Search for entities matching the search term. To combine search with other conditions, use
   * {@link #searchFor(String)} with {@code findAll(Specification, Pageable)} instead.
   *
   * @param searchTerm the search term to use, can be null or empty
   * @param pageable the pageable to request a paged result, can be {@link Pageable#unpaged()}, must
   *     not be null.
   * @return a page of entities matching the search term (if any) or all entities if the search term
   *     is null or empty.
   */
  default Page<S> search(String searchTerm, Pageable pageable) {
    return findAll(searchFor(searchTerm), pageable);
  }

  /**
   * A {@link Specification} matching the search term against the entity's {@code @Searchable}
   * fields, for combining search with other conditions:
   *
   * <pre>{@code
   * articles.findAll(articles.searchFor(term).and(isPublished()), pageable);
   * }</pre>
   *
   * <p>It matches exactly what {@link #search(String, Pageable)} matches: every word of the term in
   * at least one searchable field (see {@link Searchables#createSearchPredicate}). A {@code null}
   * or blank term matches every entity. It is an instance method so the entity type comes from the
   * repository and chaining with {@code and}/{@code or} needs no type arguments.
   *
   * @param searchTerm the search term, may be null or blank
   * @return the specification
   */
  default Specification<S> searchFor(String searchTerm) {
    return (root, query, criteriaBuilder) ->
        Searchables.createSearchPredicate(searchTerm, root, criteriaBuilder);
  }
}
