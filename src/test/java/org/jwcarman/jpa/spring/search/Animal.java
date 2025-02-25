package org.jwcarman.jpa.spring.search;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.NoArgsConstructor;
import org.jwcarman.jpa.search.Searchable;

import java.util.UUID;

@Entity
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Animal {
    private String id = UUID.randomUUID().toString();
    private String name;
    private Long version;
    @Id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Searchable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Version
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof Animal animal)) return false;

        return id.equals(animal.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
