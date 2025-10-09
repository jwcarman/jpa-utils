# Contributing to JPA Utilities

Thanks for your interest in contributing to JPA Utilities! We welcome pull requests, issues, and feedback from the community.

## How to Contribute

### 🐛 Reporting Bugs

If you find a bug, please open an issue and include:
- A clear description of the problem
- Steps to reproduce the issue
- Expected vs actual behavior
- Version of JPA Utilities and relevant environment details (Java version, Spring Boot version, database)

### 💡 Requesting Features

We're happy to hear your ideas! Before opening a feature request, check if one already exists. If not, open a new issue and include:
- A description of the proposed feature
- Why it would be useful
- Any relevant use cases or examples

### 🔧 Submitting a Pull Request

If you'd like to contribute code:
1. **Fork** the repository and create a new branch from `main`
2. Make your changes, writing tests if applicable
3. Run the build and tests:
   ```bash
   mvn clean verify
   ```
4. Ensure code quality checks pass:
   ```bash
   mvn -Pci verify sonar:sonar
   ```
5. Open a pull request and describe your changes

Please follow idiomatic Java practices and keep your code clean and well-documented. For larger changes, consider opening an issue first to discuss the approach.

## 🧱 Project Structure

- `src/main/java/org/jwcarman/jpa/entity/` - Base entity classes (UUID-based IDs, auditing)
- `src/main/java/org/jwcarman/jpa/pagination/` - Framework-agnostic pagination contracts
- `src/main/java/org/jwcarman/jpa/search/` - `@Searchable` annotation and search utilities
- `src/main/java/org/jwcarman/jpa/spring/` - Spring Data JPA integration
- `src/test/java/` - Unit and integration tests
- `src/test/java/org/jwcarman/jpa/integration/` - Multi-database integration tests

## 🧪 Testing

We maintain high test coverage and test against multiple databases:
- **Unit tests**: Run with `mvn test`
- **Integration tests**: Run with `mvn verify` (uses Testcontainers)
- **Supported databases**: PostgreSQL, MySQL, MariaDB, CockroachDB, Oracle, H2

When adding new features, please include:
- Unit tests for core logic
- Integration tests if the feature interacts with JPA/databases
- Javadoc for public APIs

## 📜 Code Style and Conventions

- **Java 21+**: Use modern Java features judiciously — prefer clarity and simplicity
- **Formatting**: Follow standard Java conventions (IntelliJ IDEA or Google Java Style)
- **Javadoc**: All public methods and classes should have comprehensive Javadoc
- **Visibility**: Keep method visibility as narrow as possible
- **Immutability**: Prefer immutable objects (records, final fields) where appropriate
- **Null safety**: Use `Optional` where appropriate, document null behavior

## 🔢 Java Version Policy

We follow a **"last LTS"** policy to balance modern features with maximum compatibility:

- **Current baseline**: Java 21 (LTS released September 2023)
- **Policy**: Upgrade to the newest LTS when the *next* LTS is released
- **Rationale**: This ensures we stay on well-adopted versions while maximizing library compatibility

**Example timeline:**
- Java 21 LTS (Sep 2023) → We adopt Java 21
- Java 25 LTS (Sep 2025) → We stay on Java 21 (allow adoption time)
- Java 29 LTS (Sep 2027) → We upgrade to Java 25

This approach gives enterprise users 2+ years to adopt each LTS before we require it, while keeping the codebase modern.

### Commit Messages

Follow conventional commit format:
- `feat: add support for multi-field sorting`
- `fix: clamp defaultPageSize to prevent DoS`
- `docs: add error handling examples to README`
- `test: add integration tests for Oracle database`
- `refactor: extract clampPageSize helper method`

## 🙌 Community Standards

We strive to foster a welcoming and respectful community. By participating, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

## 📄 License

By contributing to this project, you agree that your contributions will be licensed under the Apache License 2.0.

---

Thank you for contributing to JPA Utilities! 🎉
