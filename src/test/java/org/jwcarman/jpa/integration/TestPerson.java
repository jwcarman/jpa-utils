package org.jwcarman.jpa.integration;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jwcarman.jpa.entity.BaseEntity;
import org.jwcarman.jpa.search.Searchable;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class TestPerson extends BaseEntity {

    @Searchable
    private String firstName;

    @Searchable
    private String lastName;

    @Searchable
    private String email;

    public TestPerson(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }
}
