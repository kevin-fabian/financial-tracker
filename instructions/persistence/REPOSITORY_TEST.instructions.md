---
applyTo: '**/*RepositoryTest.java'
description: 'Unit test guidelines for repository implementations in this repository'
---

## Writing Repository Implementation Tests Best Practices

### Do

- Use `DataJpaTest` annotation for the test class to configure an in-memory database and scan for JPA entities and repositories.
- Use `@TestConfiguration` to define beans required for testing the repository implementation.
- Use `@MockitoSpyBean` to spy on the JPA repository interface used by the repository implementation to verify interactions.
- Use `@Autowired` to inject the repository implementation being tested.

### Don't

- Don't implement explicit test for Jpa**Repository interfaces; focus on testing the custom repository implementation that uses the Jpa**Repository.
- Don't mock the repository implementation being tested; use a real instance with an in-memory database instead.
- Don't set id manually; let it be generated automatically.

---

## Example Repository Implementation Test

```java
@DataJpaTest
class DefaultCodeSnippetRepositoryTest {
    @MockitoSpyBean
    private JpaUserRepository jpaUserRepository;
    @Autowired
    private UserRepository userRepository;
    
    private User user;

    @TestConfiguration
    public static class TestConfig {
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
