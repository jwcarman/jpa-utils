package org.jwcarman.jpa.spring.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.jwcarman.jpa.entity.BaseEntity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Extends {@link BaseEntity} to provide auditing capabilities.
 * <p>
 * This class automatically tracks the creation and modification timestamps, as well as the user responsible for these changes.
 * It integrates with Spring Data JPA's auditing feature, which requires enabling auditing via
 * {@code @EnableJpaAuditing} in a Spring configuration class.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * {@code
 * @Entity
 * public class User extends AuditableEntity {
 *     private String name;
 *
 *     protected User() {
 *         super();
 *     }
 *
 *     public User(String name) {
 *         super();
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
 * <h2>Requirements</h2>
 * <ul>
 *     <li>Spring Data JPA auditing must be enabled using {@code @EnableJpaAuditing}.</li>
 *     <li>An {@code AuditorAware} bean must be configured to track users (for {@code @CreatedBy} and {@code @LastModifiedBy}).</li>
 * </ul>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity extends BaseEntity {

// ------------------------------ FIELDS ------------------------------

    /**
     * The timestamp when the entity was created.
     * <p>
     * Automatically populated by Spring Data JPA auditing and cannot be updated after creation.
     * </p>
     */
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Instant createdDate;

    /**
     * The timestamp when the entity was last modified.
     * <p>
     * Automatically updated whenever the entity is modified.
     * </p>
     */
    @LastModifiedDate
    @Column(nullable = false)
    private Instant modifiedDate;

    /**
     * The username or identifier of the user who created this entity.
     * <p>
     * Automatically populated if an {@code AuditorAware} bean is configured.
     * </p>
     */
    @CreatedBy
    @Column(updatable = false, nullable = false)
    private String createdBy;

    /**
     * The username or identifier of the last user who modified this entity.
     * <p>
     * Automatically updated whenever the entity is modified.
     * </p>
     */
    @LastModifiedBy
    @Column(nullable = false)
    private String modifiedBy;

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * Default constructor required for JPA.
     * <p>
     * This constructor is protected to prevent direct instantiation while allowing subclassing.
     * </p>
     */
    protected AuditableEntity() {
    }

    /**
     * Constructs an auditable entity with a predefined UUID.
     *
     * @param id the UUID to assign to the entity
     * @throws NullPointerException if the provided UUID is null
     */
    protected AuditableEntity(UUID id) {
        super(id);
    }

// --------------------- GETTER METHODS ---------------------

    /**
     * Returns the user who created this entity.
     *
     * @return the creator's username or identifier
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Returns the creation timestamp.
     *
     * @return the timestamp when this entity was created
     */
    public Instant getCreatedDate() {
        return createdDate;
    }

    /**
     * Returns the user who last modified this entity.
     *
     * @return the last modifier's username or identifier
     */
    public String getModifiedBy() {
        return modifiedBy;
    }

    /**
     * Returns the last modification timestamp.
     *
     * @return the timestamp when this entity was last modified
     */
    public Instant getModifiedDate() {
        return modifiedDate;
    }
}
