---
description: Generate a white-box and black-box test plan for the requested code
---

**System Instructions:**

Apply `@{.qwen/skills/write-test-cases/SKILL.md}`.

## Objective

Analyze `{{args}}` and create a comprehensive test plan covering both:

* **Black-box testing** — behavior observable from the method/API contract without relying on implementation details.
* **White-box testing** — implementation paths, branches, conditions, exceptions, edge cases, and important internal decisions that should be covered by tests.

Do **not** implement the tests. Only create the test plan.

## Process

### 1. Analyze the target

Analyze `{{args}}` and identify:

* Classes and methods that need testing
* Method inputs and outputs
* Preconditions and validation rules
* Success scenarios
* Failure scenarios
* Exceptions and error handling
* Boundary conditions
* Null/empty/invalid inputs
* Branches and conditional logic
* Dependencies and their possible outcomes
* Database/repository interactions
* External service interactions
* Side effects and state changes
* Existing tests that should be considered to avoid duplication

If `{{args}}` contains multiple methods, group the test cases by method.

### 2. Generate black-box test cases

For each method, derive tests from its expected behavior and contract.

Consider:

* Valid inputs
* Invalid inputs
* Missing/empty inputs
* Boundary values
* Non-existent resources
* Duplicate resources
* Different valid combinations of inputs
* Expected response/output
* Expected error/exception
* Observable side effects

Do not create test cases solely because an internal implementation detail exists.

### 3. Generate white-box test cases

Inspect the implementation and identify important execution paths.

Consider:

* `if/else` branches
* `switch` branches
* Boolean conditions
* Early returns
* Exception paths
* `try/catch` behavior
* Different dependency responses
* Empty results
* Null results
* Repository/database failure paths
* Conditional side effects
* Mapping/transformation logic
* Important loops or collection handling

Ensure every meaningful branch/path has an appropriate test case where practical.

### 4. Avoid redundant tests

When multiple inputs produce the same behavior and assertions, use a **parameterized test case** instead of creating separate test cases.

For example:

```text
givenInvalidCategoryRequest_thenBadRequestShouldBeReturned
  - name = null
  - name = ""
  - name = "   "
  - name > maximum length
```

Do not use parameterization when the scenarios have materially different behavior, setup, or assertions.

### 5. Name test cases consistently

Use the pattern:

```text
given<Condition>_then<ExpectedOutcome>
```

Examples:

```text
CreateCategory
  - givenValidRequest_thenCategoryShouldBeCreated
  - givenDuplicateCategory_thenConflictShouldBeReturned
  - givenInvalidRequest_thenBadRequestShouldBeReturned

GetCategories
  - givenExistingCategories_thenCategoriesShouldBeRetrieved
  - givenNoCategories_thenEmptyListShouldBeReturned
```

Use names that describe **behavior and expected outcome**, not implementation details.

### 6. Prioritize test cases

Classify each test case as:

* **Required** — important functional behavior or meaningful code path
* **Recommended** — valuable edge/error scenario
* **Optional** — low-risk or defensive scenario

Avoid generating tests merely to increase test count.

### 7. Create the test plan

Create:

`test_plan.md`

in the project root.

The document should contain:

1. Scope
2. Classes/methods analyzed
3. Test cases grouped by method
4. Black-box test cases
5. White-box test cases
6. Parameterized test opportunities
7. Coverage gaps or assumptions

Use this structure:

```markdown
# Test Plan

## Scope

<summary of the code being tested>

## CreateCategory

### Black-box

| Priority | Test Case | Type | Expected Result |
|---|---|---|---|
| Required | givenValidRequest_thenCategoryShouldBeCreated | Happy path | Category is created |
| Required | givenDuplicateCategory_thenConflictShouldBeReturned | Error | Conflict response |
| Required | givenInvalidRequest_thenBadRequestShouldBeReturned | Parameterized | Bad request |

### White-box

| Priority | Test Case | Branch/Path Covered | Expected Result |
|---|---|---|---|
| Required | givenExistingCategory_thenCreationShouldBeRejected | Duplicate check | Conflict |
| Required | givenRepositoryFailure_thenErrorShouldBeHandled | Exception path | Expected error |

### Parameterized Tests

- `givenInvalidRequest_thenBadRequestShouldBeReturned`
  - null name
  - empty name
  - blank name
  - name exceeding maximum length

## GetCategories

...

## Coverage Gaps / Assumptions

- ...
```

### Rules

* Do not implement production code.
* Do not implement tests.
* Do not modify existing tests.
* Do not modify project configuration.
* Do not invent business rules that cannot be inferred from the code, documentation, or existing tests.
* Prefer fewer meaningful tests over many redundant tests.
* Clearly distinguish black-box behavior from white-box implementation coverage.
* Reuse existing test conventions and naming patterns found in the project.
* If an existing test already covers a scenario, identify it rather than creating a duplicate.
* If behavior is ambiguous, document the assumption in `Coverage Gaps / Assumptions`.
