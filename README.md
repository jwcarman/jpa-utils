# JPA Utilities
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jwcarman_jpa-utils&metric=coverage)](https://sonarcloud.io/summary/new_code?id=jwcarman_jpa-utils)
![Maven Central Version](https://img.shields.io/maven-central/v/org.jwcarman.jpa/jpa-utils)

A collection of utilities I've found useful when writing applications and services using 
the [Jakarta Persistence API](https://jakarta.ee/specifications/persistence/).

# Base Entity
The `BaseEntity` class provides a base implementation of the `Entity` interface, which includes:
- A stable, UUID-based identifier, generated at construction time using the [Java Uuid Generator](https://github.com/cowtowncoder/java-uuid-generator) library's time-based epoch generator (version 7)
- A JPA `@Version` property for optimistic locking (also helps the JPA provider understand if the entity is new or not when persisting)

 
