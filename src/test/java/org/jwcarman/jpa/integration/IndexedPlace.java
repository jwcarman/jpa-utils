package org.jwcarman.jpa.integration;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jwcarman.jpa.entity.BaseEntity;
import org.jwcarman.jpa.search.MatchMode;
import org.jwcarman.jpa.search.Searchable;

/**
 * An entity whose searchable fields are all index-friendly ({@code PREFIX} and {@code EXACT}), with no
 * {@code CONTAINS} field, so a search over it can be answered from indexes.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndexedPlace extends BaseEntity {

    @Searchable(match = MatchMode.PREFIX)
    private String city;

    @Searchable(match = MatchMode.EXACT)
    private String state;

    public IndexedPlace(String city, String state) {
        this.city = city;
        this.state = state;
    }
}
