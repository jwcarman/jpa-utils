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
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jwcarman.jpa.search.Searchable;
import org.jwcarman.jpa.spring.audit.AuditableEntity;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Person extends AuditableEntity {

  @Searchable private String firstName;

  @Searchable private String lastName;

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
