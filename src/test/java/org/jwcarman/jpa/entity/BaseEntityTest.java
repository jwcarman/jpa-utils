package org.jwcarman.jpa.entity;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {
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
    }
}