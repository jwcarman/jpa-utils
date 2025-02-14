# JPA Utilities
![Maven Central Version](https://img.shields.io/maven-central/v/org.jwcarman.jpa/jpa-utils)

A collection of utilities I've found useful when writing applications and services using 
the [Jakarta Persistence API](https://jakarta.ee/specifications/persistence/).

# Base Entity
The `BaseEntity` class provides a base implementation of the `Entity` interface, which includes:
- A stable, UUID-based identifier, generated at construction time using the [Java Uuid Generator](https://github.com/cowtowncoder/java-uuid-generator) library's time-based epoch generator (version 7)
- A JPA `@Version` property for optimistic locking (also helps the JPA provider understand if the entity is new or not)

 
