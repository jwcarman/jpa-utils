package org.jwcarman.jpa.spring.search;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jwcarman.jpa.entity.BaseEntity;
import org.jwcarman.jpa.search.Searchable;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Person extends BaseEntity {

    @Searchable
    private String firstName;

    @Searchable
    private String lastName;

    public Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }


}
