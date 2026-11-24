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
package org.jwcarman.jpa.spring.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.jwcarman.jpa.entity.BaseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AuditableEntityTest {

  @Test
  void entities_with_the_same_id_are_equal_regardless_of_audit_fields() {
    UUID id = UUID.randomUUID();
    Document first = new Document(id);
    Document second = new Document(id);
    ReflectionTestUtils.setField(first, "createdBy", "alice");
    ReflectionTestUtils.setField(first, "modifiedDate", Instant.parse("2026-01-01T00:00:00Z"));
    ReflectionTestUtils.setField(second, "createdBy", "bob");
    ReflectionTestUtils.setField(second, "modifiedDate", Instant.parse("2026-06-01T00:00:00Z"));

    assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
  }

  @Test
  void entities_with_different_ids_are_not_equal() {
    assertThat(new Document(UUID.randomUUID())).isNotEqualTo(new Document(UUID.randomUUID()));
  }

  @Test
  void an_entity_stays_equal_to_itself_after_its_audit_fields_change() {
    Document document = new Document(UUID.randomUUID());
    int hashBefore = document.hashCode();

    ReflectionTestUtils.setField(document, "modifiedBy", "carol");

    assertThat(document).isEqualTo(document);
    assertThat(document.hashCode()).isEqualTo(hashBefore);
  }

  @Test
  void hashes_exactly_like_a_base_entity_with_the_same_id() {
    UUID id = UUID.randomUUID();

    assertThat(new Document(id)).hasSameHashCodeAs(new PlainEntity(id));
  }

  static class PlainEntity extends BaseEntity {
    PlainEntity(UUID id) {
      super(id);
    }
  }

  static class Document extends AuditableEntity {
    Document(UUID id) {
      super(id);
    }
  }
}
