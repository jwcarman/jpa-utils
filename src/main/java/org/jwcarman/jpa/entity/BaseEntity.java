package org.jwcarman.jpa.entity;

import com.fasterxml.uuid.Generators;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * A base class for JPA entities that provides a universally unique identifier (UUID) as a primary key.
 * <p>
 * This class is intended to be extended by entity classes that require a unique and immutable identifier.
 * It ensures that every entity has a stable identifier upon creation and maintains a version for optimistic locking.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * {@code
 * @Entity
 * public class User extends BaseEntity {
 *     private String name;
 *
 *     protected User() {
 *     }
 *
 *     public User(String name) {
 *         this.name = name;
 *     }
 *
 *     public String getName() {
 *         return name;
 *     }
 * }
 * }
 * </pre>
 *
 * <h2>Key Features</h2>
 * <ul>
 *     <li>Automatically assigns a time-based UUID upon creation.</li>
 *     <li>Enforces a non-null ID using {@link java.util.Objects#requireNonNull(Object, String)}.</li>
 *     <li>Provides a version field for optimistic locking.</li>
 *     <li>Implements a final {@code equals()} and {@code hashCode()} based on the unique identifier.</li>
 *     <li>Does not include a {@code toString()} method to allow users full control over string representations.</li>
 * </ul>
 */
@MappedSuperclass
public abstract class BaseEntity {

// ------------------------------ FIELDS ------------------------------

    /**
     * The unique identifier for the entity.
     * <p>
     * This is a universally unique identifier (UUID) that is assigned at the time of entity creation
     * and remains constant throughout the entity's lifecycle.
     * </p>
     */
    @Id
    @Column(updatable = false, nullable = false)
    @Getter
    private UUID id;

    /**
     * The version field for optimistic locking.
     * <p>
     * This field is managed by JPA and is automatically incremented upon updates to prevent
     * concurrent modification conflicts.
     * </p>
     */
    @Version
    @Getter
    private Long version;

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * Default constructor that assigns a time-based UUID to the entity.
     * <p>
     * This constructor is required for JPA and ensures that an entity always has a stable identifier upon instantiation.
     * </p>
     */
    protected BaseEntity() {
        this(Generators.timeBasedEpochGenerator().generate());
    }

    /**
     * Constructs an entity with the given UUID.
     * <p>
     * This constructor allows for explicit assignment of the UUID, useful when reconstructing entities
     * from external sources.
     * </p>
     *
     * @param id the UUID to be assigned to the entity (must not be null)
     * @throws NullPointerException if the provided UUID is null
     */
    protected BaseEntity(UUID id) {
        this.id = requireNonNull(id, "ID cannot be null.");
    }

// ------------------------ CANONICAL METHODS ------------------------

    /**
     * Determines equality based on the entity's unique identifier.
     * <p>
     * Two entities are considered equal if they are of compatible types (proxy-safe) and have the same non-null identifier.
     * This implementation ensures that different aggregate types don't compare equal even if they share the same UUID.
     * </p>
     *
     * @param o the object to compare against
     * @return {@code true} if both entities are type-compatible and have the same UUID, {@code false} otherwise
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity that)) return false;

        // Ensure type compatibility (handles proxies and inheritance)
        if (!this.getClass().isAssignableFrom(that.getClass()) &&
            !that.getClass().isAssignableFrom(this.getClass())) {
            return false;
        }

        return id != null && id.equals(that.id);
    }

    /**
     * Computes a hash code using the entity's unique identifier.
     *
     * @return a stable hash code based on the entity's UUID
     */
    @Override
    public final int hashCode() {
        return id.hashCode();
    }

}
