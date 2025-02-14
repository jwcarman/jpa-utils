package org.jwcarman.jpa.spring.search;

import org.jwcarman.jpa.spring.page.PageSpec;

import java.util.Optional;

/**
 * A specification for searching.
 */
public interface SearchSpec extends PageSpec {

    /**
     * The search term.
     *
     * @return the search term
     */
    Optional<String> searchTerm();
}
