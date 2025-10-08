package org.jwcarman.jpa.integration;

import org.jwcarman.jpa.spring.search.SearchableRepository;

import java.util.UUID;

public interface TestPersonRepository extends SearchableRepository<TestPerson, UUID> {
}
