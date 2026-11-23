package org.jwcarman.jpa.integration;

import org.jwcarman.jpa.spring.search.SearchableRepository;

import java.util.UUID;

public interface IndexedPlaceRepository extends SearchableRepository<IndexedPlace, UUID> {
}
