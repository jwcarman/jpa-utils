package org.jwcarman.jpa.entity;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;

import java.util.Objects;
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
 *     <li>Implements {@code equals()} and {@code hashCode()} based on the unique identifier, read through
 *     {@code getId()} so Hibernate lazy proxies compare equal to the loaded entity.</li>
 *     <li>Does not include a {@code toString()} method to allow users full control over string representations.</li>
 * </ul>
 */
@MappedSuperclass
public abstract class BaseEntity {

// ------------------------------ FIELDS ------------------------------

    /**
     * The UUID v7 generator for every entity. JUG generators are thread-safe, and one shared instance keeps
     * identifiers strictly increasing within a millisecond, which a generator per entity would not.
     */
    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

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
     * Identifiers come from one shared generator, so they are strictly increasing even for entities created within the
     * same millisecond.
     * </p>
     */
    protected BaseEntity() {
        this(ID_GENERATOR.generate());
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
     * Two entities are considered equal if they have the same UUID.
     * Since UUIDs are universally unique, collision across different entity types
     * is effectively impossible and represents a design error if it occurs.
     * </p>
     *
     * <p>
     * The identifier is read through {@code getId()}, never the field, on both sides. A Hibernate lazy proxy is a
     * generated subclass whose own fields are not the entity's state; its {@code getId()} returns the real
     * identifier without loading the entity. For the same reason this method is not {@code final} (a proxy could
     * not intercept it); subclasses should not override it.
     * </p>
     *
     * @param o the object to compare against
     * @return {@code true} if both entities have the same UUID, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    /**
     * Computes a hash code using the entity's unique identifier.
     * <p>
     * Returns a stable hash code based on the entity's UUID, or 0 if the ID is null.
     * This ensures consistent behavior even in edge cases where the ID might not be set.
     * </p>
     *
     * @return a stable hash code based on the entity's UUID, or 0 if ID is null
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

}
