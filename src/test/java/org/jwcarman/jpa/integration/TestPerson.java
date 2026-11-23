package org.jwcarman.jpa.integration;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jwcarman.jpa.entity.BaseEntity;
import org.jwcarman.jpa.search.MatchMode;
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

    @Searchable(match = MatchMode.PREFIX)
    private String city;

    @Searchable(match = MatchMode.EXACT)
    private String state;

    public TestPerson(String firstName, String lastName, String email) {
        this(firstName, lastName, email, null, null);
    }

    public TestPerson(String firstName, String lastName, String email, String city, String state) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.city = city;
        this.state = state;
    }
}
