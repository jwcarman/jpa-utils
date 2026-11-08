package org.jwcarman.jpa.entity;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {
    @Test
    void ids_from_consecutive_constructions_are_strictly_increasing() {
        // Thousands of entities are created within the same few milliseconds. A shared v7 generator orders ids
        // within a millisecond; a generator per entity would only order them to the millisecond.
        String previous = new Person().getId().toString();
        for (int i = 0; i < 10_000; i++) {
            String next = new Person().getId().toString();
            assertThat(next).isGreaterThan(previous);
            previous = next;
        }
    }

    @Test
    void ids_are_version_7() {
        assertThat(new Person().getId().version()).isEqualTo(7);
    }

    @Test
    void entityShouldEqualItself() {
        final var entity = new Person("Joe", "Shmoe");
        assertThat(entity).isEqualTo(entity);
    }

    @Test
    void entityShouldNotEqualNull() {
        final var entity = new Person("Joe", "Shmoe");
        assertThat(entity).isNotEqualTo(null);
    }

    @Test
    void entityShouldNotEqualDifferentClass() {
        final var entity = new Person("Joe", "Shmoe");
        assertThat(entity).isNotEqualTo(new Object());
    }

    @Test
    void entityShouldHaveIdAtCreation() {
        final var entity = new Person("Joe", "Shmoe");
        assertThat(entity.getId()).isNotNull();
    }

    @Test
    void entityShouldHaveIdAtCreationWithNoArgsConstructor() {
        final var entity = new Person();
        assertThat(entity.getId()).isNotNull();
    }

    @Test
    void entityShouldNotHaveAVersionAtCreation() {
        final var entity = new Person("Joe", "Shmoe");
        assertThat(entity.getVersion()).isNull();
    }

    @Test
    void entityShouldHaveAValidHashCode() {
        final var entity = new Person("Joe", "Shmoe");
        assertThat(entity.hashCode()).isNotZero();
    }

    @Test
    void entitiesOfSameTypeWithSameIdShouldBeEqual() {
        final UUID sharedId = UUID.randomUUID();
        final var person1 = new Person(sharedId, "Joe", "Shmoe");
        final var person2 = new Person(sharedId, "Jane", "Doe");

        assertThat(person1).isEqualTo(person2);
        assertThat(person2).isEqualTo(person1);
    }

    @Test
    void entitiesOfSameTypeWithDifferentIdsShouldNotBeEqual() {
        final var person1 = new Person("Joe", "Shmoe");
        final var person2 = new Person("Jane", "Doe");

        assertThat(person1).isNotEqualTo(person2);
        assertThat(person2).isNotEqualTo(person1);
    }

    @Test
    void entityShouldHandleNullIdInEquality() {
        final var person1 = new Person("Joe", "Shmoe");
        final var person2 = new Person("Jane", "Doe");

        // This shouldn't happen in production (IDs are generated), but test defensive code
        assertThat(person1.equals(person2)).isFalse();
    }

    @Entity
    @Getter
    @NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
    public static class Person extends BaseEntity {

        private String firstName;
        private String lastName;

        public Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public Person(UUID id, String firstName, String lastName) {
            super(id);
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    @Entity
    @Getter
    @NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
    public static class Animal extends BaseEntity {

        private String name;

        public Animal(String name) {
            this.name = name;
        }

        public Animal(UUID id, String name) {
            super(id);
            this.name = name;
        }
    }
}