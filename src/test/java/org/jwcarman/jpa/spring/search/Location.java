package org.jwcarman.jpa.spring.search;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jwcarman.jpa.entity.BaseEntity;
import org.jwcarman.jpa.search.MatchMode;
import org.jwcarman.jpa.search.Searchable;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location extends BaseEntity {

    @Searchable
    private String name;

    @Searchable(match = MatchMode.PREFIX)
    private String city;

    @Searchable(match = MatchMode.EXACT)
    private String state;

    public Location(String name, String city, String state) {
        this.name = name;
        this.city = city;
        this.state = state;
    }
}
