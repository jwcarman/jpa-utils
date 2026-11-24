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
package org.jwcarman.jpa.integration;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jwcarman.jpa.entity.BaseEntity;
import org.jwcarman.jpa.search.MatchMode;
import org.jwcarman.jpa.search.Searchable;

/**
 * An entity whose searchable fields are all index-friendly ({@code PREFIX} and {@code EXACT}), with
 * no {@code CONTAINS} field, so a search over it can be answered from indexes.
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
