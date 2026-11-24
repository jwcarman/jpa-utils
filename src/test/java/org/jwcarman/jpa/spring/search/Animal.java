/*
 * Copyright © 2025 James Carman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jwcarman.jpa.spring.search;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.NoArgsConstructor;
import org.jwcarman.jpa.search.MatchMode;
import org.jwcarman.jpa.search.Searchable;

@Entity
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Animal {
  private String id = UUID.randomUUID().toString();
  private String name;
  private String species;
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

  @Searchable(match = MatchMode.EXACT)
  public String getSpecies() {
    return species;
  }

  public void setSpecies(String species) {
    this.species = species;
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
