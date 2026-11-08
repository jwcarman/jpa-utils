# Entities

## BaseEntity

`BaseEntity` is a `@MappedSuperclass` for entities identified by a UUID:

| Member | What it is |
|--------|------------|
| `UUID id` | The `@Id`: a time-ordered UUID v7, assigned when the object is constructed, never updated |
| `Long version` | The `@Version` column for optimistic locking, managed by JPA |
| `equals` / `hashCode` | Identity by `id` only |

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    private String name;
    private BigDecimal price;

    public Product(String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }
}
```

### The id exists before the row does

The no-argument constructor assigns a UUID v7, so `new Product(...)` has its identifier immediately, before it is
saved. It can go into a `Set`, become a map key, or be referenced by another new entity, and its `equals` and
`hashCode` never change. Identity based on a database-generated id can't do that, because the id is `null` until
the insert.

UUID v7 values start with a timestamp, so new rows insert near the end of the primary-key index instead of at
random points, which avoids the index fragmentation random (v4) UUIDs cause.

Spring Data still knows a new entity from an existing one: an entity with a wrapper-typed `@Version` is new while
its version is `null`, so `save` persists rather than merges.

To give an entity a specific id (rebuilding one from an external source, for example), use the protected
`BaseEntity(UUID id)` constructor; it rejects `null`.

### Equality and Hibernate proxies

Two entities are equal when their ids are equal. `equals` and `hashCode` read the id through `getId()`, never the
field, on both sides of the comparison. That matters for Hibernate's lazy proxies:
`entityManager.getReference(Product.class, id)` returns a generated subclass whose own fields are not the entity's
state, and only its `getId()` reports the real identifier. A proxy and the loaded entity are therefore equal and
hash alike.

!!! warning "Don't override `equals` or `hashCode`"
    Identity by id is what makes entities safe in hash-based collections across their lifecycle. An override that
    compares other fields makes an entity unequal to itself after an update, and unequal to its own proxy.

## AuditableEntity

`AuditableEntity` extends `BaseEntity` with four columns that Spring Data JPA's auditing fills in:

| Field | Set |
|-------|-----|
| `Instant createdDate` | on insert, never updated |
| `Instant modifiedDate` | on every insert and update |
| `String createdBy` | on insert, never updated |
| `String modifiedBy` | on every insert and update |

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends AuditableEntity {

    private BigDecimal total;

    public Order(BigDecimal total) {
        this.total = total;
    }
}
```

Enable auditing and say who the current user is:

```java
@Configuration
@EnableJpaAuditing
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditor() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName);
    }
}
```

Without an `AuditorAware` bean the dates are still recorded and the user columns stay `null`.

The audit fields take no part in equality: they describe when and by whom a row was written, not which entity it
is.
