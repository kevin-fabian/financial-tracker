---
name: write-spring-cucumber-autotests
description: Guide for writing Cucumber-based integration tests with Allure reporting. Use this when writing or debugging autotest module tests, feature files, or step definitions.
---

# Cucumber Integration Testing Guide

A guide for writing integration tests using **Cucumber BDD**, **Spring Boot**, and **Allure Reporting**.
Reuse existing step definitions and patterns for consistency. Follow the project structure and conventions for maintainable, reliable tests.

## Quick Reference

```bash
# Run all integration tests
mvn test -Pautotest -pl autotest -Dspring.profiles.active=local

# View HTML reports
mvn allure:serve -pl autotest
```

## Project Structure

```
autotest/
├── src/main/java/.../
│   ├── config/          # RestClient, RetryTemplate configs
│   ├── clients/         # HTTP clients (RestClient-based)
│   │   └── dto/         # Request/Response DTOs
│   └── utils/           # SpelTemplateParser, helpers
└── src/test/
    ├── java/.../
    │   ├── CucumberTest.java              # JUnit Suite runner
    │   └── integration/
    │       ├── contexts/TestContext.java  # Scenario-scoped state
    │       └── steps/                     # Step definitions
    └── resources/
        ├── allure.properties
        └── features/                      # Gherkin feature files
```

## Key Patterns

### 1. Feature File Conventions

```gherkin
@integration_test
Feature: [Feature Name]

  Background:
    Given [common setup steps]

  Scenario: [Clear description of what is being tested]
    Given [precondition]
    When [action]
    Then [expected outcome]
```

- Tag scenarios appropriately: `@integration_test`, `@smoke`, etc.
- Use `Background` for common setup (e.g., cleanup, state reset)
- Use `Scenario Outline` with `Examples` for parameterized tests
- Use data tables for structured inputs

### 2. SpEL Expressions for Dynamic Data

Use SpEL expressions in Gherkin tables for dynamic values:

| Expression | Description |
|------------|-------------|
| `#randomUUID()` | Generate random UUID |
| `#now('YYYYMMdd')` | Format current date/time |
| `#variable` | Reference context variable |
| `#list[index].property` | Access nested properties |

Example:
```gherkin
Given the following records exist
  | id            | createdAt      |
  | #randomUUID() | #now('YYYYMMdd') |
```

### 3. TestContext for State Sharing

`TestContext` is `@ScenarioScope` and shares state between steps:

```java
// Store data
testContext.addContext("key", value);

// Retrieve typed data
List<Response> responses = testContext.getContext(
    "key", new ParameterizedTypeReference<>() {});

// Parse SpEL expressions
String id = testContext.parse("#createdRecords[0].id", String.class);
```

### 4. Step Definition Best Practices

```java
@Slf4j
public class MySteps {
    @Autowired
    private MyClient client;
    @Autowired
    private TestContext testContext;

    @Given("the system has received records")
    public void setupRecords(DataTable dataTable) {
        for (Map<String, String> row : dataTable.entries()) {
            var request = buildRequest(row);
            var response = client.createRecord(request);
            assertEquals(201, response.getStatusCode().value());
        }
    }
}
```

### 5. Retry Patterns for Async Operations

Use `RetryTemplate` for polling async results:

```java
@Then("the record should be processed")
public void verifyProcessed() {
    retryTemplate.execute(ctx -> {
        var response = client.getRecord(id);
        if (!"PROCESSED".equals(response.status())) {
            throw new IllegalStateException("Not yet processed");
        }
        return response;
    });
}
```

Configure retries:
```java
@Bean
public RetryTemplate retryTemplate() {
    return RetryTemplate.builder()
        .maxAttempts(5)
        .fixedBackoff(2000)
        .retryOn(IllegalStateException.class)
        .build();
}
```

### 6. Allure Attachments for Debugging

Add attachments to help debug failures:

```java
Allure.addAttachment("Request", MediaType.APPLICATION_JSON_VALUE, 
    jsonMapper.writeValueAsString(request));
Allure.addAttachment("Response", MediaType.APPLICATION_JSON_VALUE, 
    jsonMapper.writeValueAsString(response));
```

## Maven Profiles

| Profile | Behavior |
|---------|----------|
| `local` (default) | Skips autotest for fast dev builds |
| `autotest` | Runs integration tests with Allure reporting |

## Troubleshooting

| Issue | Solution |
|-------|----------|
| **Tests not running** | Use `-Pautotest` profile |
| **Test isolation failures** | Clean state in `Background` steps, use `#randomUUID()` |
| **Timing issues** | Add `RetryTemplate` for async operations |
| **SpEL errors** | Ensure variable exists in `TestContext` before referencing |
| **Empty Allure reports** | Check `target/allure-results` has XML files |

## Related Documentation

- **[General Test Guidelines](.github/skills/write-test-cases/SKILL.md)** → Test naming conventions and assertions
- **[General Coding Guidelines](.github/copilot-instructions.md)** → Code style and architecture patterns
