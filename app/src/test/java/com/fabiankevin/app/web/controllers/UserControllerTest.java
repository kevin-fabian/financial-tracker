package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.dtos.UserResponse;
import com.fabiankevin.app.services.UserRegistrationService;
import com.fabiankevin.app.services.commands.CreateUserCommand;
import com.fabiankevin.app.web.controllers.dtos.CreateUserRequest;
import com.github.fabiankevin.lemon.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({GlobalExceptionHandler.class})
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @Autowired
    private JsonMapper jsonMapper;
    private Jwt jwt;

    @BeforeEach
    void setup() {
        jwt = Jwt.withTokenValue(UUID.randomUUID().toString())
                .subject(UUID.randomUUID().toString())
                .header("alg", "RS256")
                .audience(List.of("financial-tracker-test"))
                .claim("role", "USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void createUser_givenValidRequest_thenShouldRegisterUser() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .username("john.doe@gmail.com")
                .password("SecureP@ss1")
                .confirmPassword("SecureP@ss1")
                .categoryInterests(Set.of("groceries", "bills"))
                .accountInterests(Set.of("bank", "credit_card"))
                .build();

        UUID createdUserId = UUID.randomUUID();
        UserResponse userResponse = UserResponse.builder()
                .id(createdUserId)
                .firstName("John")
                .lastName("Doe")
                .build();
        when(userRegistrationService.register(any(CreateUserCommand.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/users")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(createdUserId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        verify(userRegistrationService, times(1)).register(any(CreateUserCommand.class));
    }

    @Test
    void createUser_givenNoJwt_thenShouldReturnForbidden() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .username("john.doe@gmail.com")
                .password("SecureP@ss1")
                .confirmPassword("SecureP@ss1")
                .build();

        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userRegistrationService);
    }

    @Test
    void createUser_givenMissingRequiredFields_thenShouldReturnBadRequest() throws Exception {
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .username("john.doe@gmail.com")
                .password("SecureP@ss1")
                .confirmPassword("SecureP@ss1")
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userRegistrationService, times(0)).register(any());
    }

    @Test
    void createUser_givenInvalidPasswordFormat_thenShouldReturnBadRequest() throws Exception {
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .username("john.doe@gmail.com")
                .password("weak")
                .confirmPassword("weak")
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userRegistrationService, times(0)).register(any());
    }

    @Test
    void createUser_givenFirstNameLessThan2Characters_thenShouldReturnBadRequest() throws Exception {
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .firstName("A")
                .lastName("Doe")
                .username("john.doe@gmail.com")
                .password("SecureP@ss1")
                .confirmPassword("SecureP@ss1")
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userRegistrationService, times(0)).register(any());
    }

    @Test
    void createUser_givenFirstNameMoreThan50Characters_thenShouldReturnBadRequest() throws Exception {
        String longFirstName = "A".repeat(51);
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .firstName(longFirstName)
                .lastName("Doe")
                .username("john.doe@gmail.com")
                .password("SecureP@ss1")
                .confirmPassword("SecureP@ss1")
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userRegistrationService, times(0)).register(any());
    }

    @Test
    void createUser_givenLastNameLessThan2Characters_thenShouldReturnBadRequest() throws Exception {
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .firstName("John")
                .lastName("D")
                .username("john.doe@gmail.com")
                .password("SecureP@ss1")
                .confirmPassword("SecureP@ss1")
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userRegistrationService, times(0)).register(any());
    }

    @Test
    void createUser_givenLastNameMoreThan50Characters_thenShouldReturnBadRequest() throws Exception {
        String longLastName = "D".repeat(51);
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .firstName("John")
                .lastName(longLastName)
                .username("john.doe@gmail.com")
                .password("SecureP@ss1")
                .confirmPassword("SecureP@ss1")
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userRegistrationService, times(0)).register(any());
    }

    @Test
    void createUser_givenFirstNameWithInvalidCharacters_thenShouldReturnBadRequest() throws Exception {
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .firstName("John@123")
                .lastName("Doe")
                .username("john.doe@gmail.com")
                .password("SecureP@ss1")
                .confirmPassword("SecureP@ss1")
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userRegistrationService, times(0)).register(any());
    }

    @Test
    void createUser_givenLastNameWithInvalidCharacters_thenShouldReturnBadRequest() throws Exception {
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .firstName("John")
                .lastName("Doe#456!")
                .username("john.doe@gmail.com")
                .password("SecureP@ss1")
                .confirmPassword("SecureP@ss1")
                .build();

        mockMvc.perform(post("/api/users")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userRegistrationService, times(0)).register(any());
    }
}
