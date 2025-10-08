package org.jwcarman.jpa.spring.search;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = {SpringBootConfig.class})
class SearchableRepositoryTest {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private UnsearchableRepository unsearchableRepository;

    @Test
    void unsearchablesShouldReturnAllResults() {
        final var unsearchable = new Unsearchable("FOO");

        unsearchableRepository.save(unsearchable);

        Page<Unsearchable> page = unsearchableRepository.search("abc", Pageable.unpaged());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void unsearchablesShouldReturnAllResultsOnMultipleSearches() {
        // This test verifies that the caching works correctly for entities without @Searchable fields
        final var unsearchable1 = new Unsearchable("FOO");
        final var unsearchable2 = new Unsearchable("BAR");

        unsearchableRepository.save(unsearchable1);
        unsearchableRepository.save(unsearchable2);

        // First search - may populate cache and log warning
        Page<Unsearchable> page1 = unsearchableRepository.search("abc", Pageable.unpaged());
        assertThat(page1.getTotalElements()).isEqualTo(2);

        // Second search - should use cached result (warning logged only once during cache population)
        Page<Unsearchable> page2 = unsearchableRepository.search("xyz", Pageable.unpaged());
        assertThat(page2.getTotalElements()).isEqualTo(2);
    }

    @Test
    void searchShouldReturnMatchingRecords() {
        final var person = new Person("Joe", "Shmoe");

        personRepository.save(person);
        personRepository.save(new Person("ABC", "abc"));

        Page<Person> page = personRepository.search("moe", Pageable.unpaged());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void nullSearchShouldReturnAllRecords() {
        final var person = new Person("Joe", "Shmoe");
        assertThat(person.getId()).isNotNull();
        assertThat(person.getVersion()).isNull();

        personRepository.save(person);

        Page<Person> page = personRepository.search(null, Pageable.unpaged());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void blankSearchTermShouldReturnAllRecords() {
        final var person = new Person("Joe", "Shmoe");
        assertThat(person.getId()).isNotNull();
        assertThat(person.getVersion()).isNull();

        personRepository.save(person);

        Page<Person> page = personRepository.search("", Pageable.unpaged());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void emptySearchTermShouldReturnAllRecords() {
        final var person = new Person(UUID.randomUUID(), "Joe", "Shmoe");
        assertThat(person.getId()).isNotNull();
        assertThat(person.getVersion()).isNull();

        personRepository.save(person);

        Page<Person> page = personRepository.search(" ", Pageable.unpaged());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchingByPropertyShouldReturnMatchingRecords() {
        final var animal = new Animal();
        animal.setName("Dog");
        assertThat(animal.getId()).isNotNull();
        assertThat(animal.getVersion()).isNull();

        animalRepository.save(animal);

        Page<Animal> page = animalRepository.search("og", Pageable.unpaged());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}