## Repository Instructions & Project Architecture Guidelines

## Project Architecture Overview
This project follows light hexagonal architecture principles, with a clear separation of concerns between the core domain logic, application services, and external interfaces. The architecture is designed to be modular, maintainable, and scalable.
The project follows DDD-light principles, with a focus on defining domain models that handle invariants itself. A model should not exist if it cannot be in a valid state.

---

## Project Overview

Multi-module Maven project with the following modules:

- `app`: Spring Boot application entry point, configuration, and main class.
- `autotest`: Spring Cucumber tests for end-to-end testing of the application.

**Quick Architecture Reference**
controllers -> services -> repositories -> jpa_repositories -> entities

---

## Copilot Instructions

**Test Guideline Instruction**[TEST.instructions.md](/instrunctions/tests/TEST.instructions.md)
**Service Implementation Instruction**[SERVICE.instructions.md](/instrunctions/services/SERVICE.instructions.md)
**Repository Implementation Instruction**[REPOSITORY.instructions.md](/instrunctions/repositories/REPOSITORY.instructions.md)


---

## Coding Standards and Best Practices

### Do
- Follow existing code style and conventions in the project for consistency.
- Use /config/AppConfig.java for manual Spring bean definitions and configuration.
- Use Java 25 features and idioms where appropriate, such as records for simple data carriers, pattern matching for instanceof, and text blocks for multi-line strings.
- Follow Java standard naming conventions for packages, classes, methods, and variables. e.g., packages in lowercase, classes in PascalCase, methods and variables in camelCase.
- Use Lombok to reduce boilerplate code for entities and DTOs, but avoid overusing it.
- Use `Builder(toBuilder = true)` for Java records to allow for easy construction and modification of immutable data carriers.
- Prefer direct import over wildcard imports for better readability and to avoid namespace pollution.
- Use Java `Instant` for timestamps and `UUID` for unique identifiers in domain models and entities.
- Use Lombok's `@RequiredArgsConstructor` for service implementations to automatically generate constructors for required dependencies.
- Use Java record for binding properties from `application.properties` or `application.yaml` configuration files.
- Apply the Single Responsibility Principle at the class and method level, ensuring that each class and method has a clear and focused responsibility.
- Use Java's `Optional` for return types that may be absent instead of returning null, to improve null safety and express intent.


### Don't

---

## Package Structure Standards
```bash
com.fabiankevin.app
├── App.java            # Main application entry point
├── config              # Spring configuration classes
├── exceptions          # Custom exception classes
├── utils               # Utility classes and helpers
├── models              # Domain models
│   ├── enums           # Enum types
├── persistence         # Data access layer
│   ├── entities         # JPA entities
│   ├── jpa_repositories # JPA repositories
│   ├── repositories     # Custom repositories
├── services            # Application services
│     ├── commands       # Command handlers
│     ├── queries        # Query handlers
├── clients            # External service clients
│     ├── payment       # Payment service clients
│     ├── identity      # Identity service clients
├── web # Web layer
│   ├── controllers     # REST controllers
│   ├── dtos            # Web-specific DTOs for request/response
```

