package org.jwcarman.jpa.spring.search;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jwcarman.jpa.entity.BaseEntity;

@Entity
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Unsearchable extends BaseEntity {

    @Getter
    private String name;

    public Unsearchable(String name) {
        this.name = name;
    }
}
