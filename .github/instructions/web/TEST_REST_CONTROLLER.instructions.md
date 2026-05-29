---
applyTo: '**/*Controller.java'
description: 'WebMvcTest controller implementation requirements.'
---

## WebMvcTest Controller implementation Best Practices

### Do

- Annotate test classes with `@WebMvcTest(ControllerClass.class)` to configure sliced Spring context for testing the specified controller.
- Use `@MockitoBean` to mock service layer dependencies of the controller.
- Use `@MockitoSpyBean` to spy on the service layer dependencies of the controller.
- Use `MockMvc` to perform HTTP requests and assert responses.
- Use `JsonMapper` to serialize request to JSON string.

---

## Example WebMvcTest Controller Implementation

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JsonMapper jsonMapper;
    @MockitoBean
    private UserService service;

    @Test
    void create_givenValidRequest_thenShouldReturnCreatedUser() throws Exception {
        when(service.create(any(CreateUserCommand.class)))
                .thenAnswer(invocation -> {
                    CreateUserCommand command = invocation.getArgument(0);
                    return User.builder()
                            .id(UUID.randomUUID())
                            .name(command.name())
                            .email(command.email())
                            .build();
                });

        mockMvc.perform(post("/v1/users")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(
                                CreateUserRequest.builder()
                                        .name("John Doe")
                                        .email("john.doe@test.com")
                                        .build()
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@test.com"));

        verify(service, times(1)).create(any(CreateUserCommand.class));
    }
}
```

