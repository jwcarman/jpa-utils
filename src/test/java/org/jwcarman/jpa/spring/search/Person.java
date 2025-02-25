package org.jwcarman.jpa.spring.search;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jwcarman.jpa.search.Searchable;
import org.jwcarman.jpa.spring.audit.AuditableEntity;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Person extends AuditableEntity {

    @Searchable
    private String firstName;

    @Searchable
    private String lastName;

    public Person(UUID id, String firstName, String lastName) {
        super(id);
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }


}
