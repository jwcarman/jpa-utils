package org.jwcarman.jpa.spring.search;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = {JpaRepositoriesAutoConfiguration.class, SpringBootConfig.class})
class SearchableRepositoryTest {

    @Autowired
    private PersonRepository repository;

    @Test
    void searchShouldReturnMatchingRecords() {
        final var person = new Person("Joe", "Shmoe");
        assertThat(person.getId()).isNotNull();
        assertThat(person.getVersion()).isNull();

        repository.save(person);

        Page<Person> page = repository.search("moe", Pageable.unpaged());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void nullSearchShouldReturnAllRecords() {
        final var person = new Person("Joe", "Shmoe");
        assertThat(person.getId()).isNotNull();
        assertThat(person.getVersion()).isNull();

        repository.save(person);

        Page<Person> page = repository.search(null, Pageable.unpaged());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void blankSearchTermShouldReturnAllRecords() {
        final var person = new Person("Joe", "Shmoe");
        assertThat(person.getId()).isNotNull();
        assertThat(person.getVersion()).isNull();

        repository.save(person);

        Page<Person> page = repository.search("", Pageable.unpaged());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void emptySearchTermShouldReturnAllRecords() {
        final var person = new Person(UUID.randomUUID(), "Joe", "Shmoe");
        assertThat(person.getId()).isNotNull();
        assertThat(person.getVersion()).isNull();

        repository.save(person);

        Page<Person> page = repository.search(" ", Pageable.unpaged());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}