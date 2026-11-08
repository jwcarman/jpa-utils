package org.jwcarman.jpa.spring.audit;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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

    static class Document extends AuditableEntity {
        Document(UUID id) {
            super(id);
        }
    }
}
