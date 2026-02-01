---
applyTo: '**/*Test.java'
description: 'All test implementation guidelines'
---

## Global test implementation guidelines

When writing core service test implementation, please follow these guidelines:

- Follow test method name conventions like `methodName_<given>_<then>`.
- Add a line(not a comment) between given, action, and assert sections in the test methods for better readability.
- Add concise assertion messages to explain the purpose of the assertions.
- Do not add any comments in the test methods.
- Use assertions to verify the expected outcomes of each test case.
- Use `@ParameterizedTest` and `@ValueSource` for parameterized tests where applicable.
- Use AssertJ for assertions for complex assertions and JUnit 5 assertions for simple assertions.
- Order the test methods logically, grouping related tests together.
