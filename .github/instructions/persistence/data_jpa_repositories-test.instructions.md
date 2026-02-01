---
applyTo: '**/persistence/repositories/**/*Test.java'
description: 'Data JPA Test for repository implementation'
---

## Repository implementation unit tests guidelines

When writing unit test for repository implementations, please follow these guidelines:

- Place test classes in the `persistence/repositories` package.
- Use `@DataJpaTest` annotation for the test class to configure an in-memory database and scan for JPA entities and
  repositories.
- Do not mock the repository implementation being tested; instead, use a real instance with an in-memory database.
- Use `@TestConfiguration` to define beans required for testing the repository implementation.
- Use `@MockitoSpyBean` to spy on the JPA repository interface used by the repository implementation.
- Use `@Autowired` to inject the repository implementation being tested.
- Write test methods to cover various scenarios for each method in the repository interface.
- Use `@BeforeEach` to set up mock data or configurations needed for the tests.
- Do not set id manually; let it be generated automatically.
- The test class should be concise and focused on testing repository-specific operations only.

Examples:

```java
package com.example.persistence.repositories;

import com.example.persistence.jpa_repositories.JpaUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;


@DataJpaTest
class DefaultCodeSnippetRepositoryTest {
    @MockitoSpyBean
    private JpaUserRepository jpaUserRepository;
    @Autowired
    private UserRepository userRepository;
    
    private User user;

    @TestConfiguration
    public static class ContextConfiguration {
        @Bean
        public UserRepository userRepository(JpaUserRepository jpaUserRepository) {
            return new DefaultUserRepository(jpaUserRepository);
        }
    }
    
    @BeforeEach
    void setUp() {
        user = User.builder()
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void findById_existingId_returnsUser() {
        userRepository.save(user);
        
        var foundUser = userRepository.findById(user.getId())
                .orThrow(UserNotFoundException::new);
        
        Assertions.assertThat(user)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(foundUser);
        assertNotNull(foundUser.getId(), "id should not be null");
        
        verify(jpaUserRepository, times(1)).findById(user.getId());
    }
}
```