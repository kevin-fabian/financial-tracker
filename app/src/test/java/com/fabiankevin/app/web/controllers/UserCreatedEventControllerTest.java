package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.services.UserProvisioningService;
import com.github.fabiankevin.lemon.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(UserCreatedEventController.class)
@Import({GlobalExceptionHandler.class})
class UserCreatedEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProvisioningService userProvisioningService;

    private Jwt jwt;

    @BeforeEach
    void setup() {
        jwt = Jwt.withTokenValue(UUID.randomUUID().toString())
                .subject(UUID.randomUUID().toString())
                .header("alg", "RS256")
                .audience(List.of("identity-service"))
                .claim("scope", List.of("user:provision"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void provisionUser_givenNewEventWithInterests_thenShouldProvisionAndReturnCreated() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/users/provision")
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "%s",
                                  "metadata": {
                                    "accountInterests": [
                                      "gcash",
                                      "maya"
                                    ],
                                    "categoryInterests": [
                                       "groceries" ,
                                       "bills",
                                       "salary_active"
                                    ]
                                  }
                                }
                                """.formatted(userId)))
                .andExpect(status().isCreated());

        verify(userProvisioningService, times(1))
                .provisionUser(eq(userId), eq(Set.of("gcash", "maya")), eq(Set.of("groceries", "bills", "salary_active")));
    }

    @Test
    void provisionUser_givenEventWithoutMetadata_thenShouldProvisionWithEmptyInterests() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/users/provision")
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "%s",
                                  "metadata": {}
                                }
                                """.formatted(userId)))
                .andExpect(status().isCreated());

        verify(userProvisioningService, times(1))
                .provisionUser(eq(userId), eq(Set.of()), eq(Set.of()));
    }

    @Test
    void provisionUser_givenEventWithNullMetadata_thenShouldReturnCreated() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/users/provision")
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "%s",
                                  "metadata": null
                                }
                                """.formatted(userId)))
                .andExpect(status().isCreated());

        verify(userProvisioningService, times(1))
                .provisionUser(eq(userId), eq(Set.of()), eq(Set.of()));
    }

    @Test
    void provisionUser_givenEventWithMissingUserId_thenShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/users/provision")
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "metadata": {
                                    "accountInterests": "gcash"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userProvisioningService);
    }

    @Test
    void provisionUser_givenEventWithMissingMetadata_thenShouldProvisionWithEmptyInterests() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/users/provision")
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "%s"
                                }
                                """.formatted(userId)))
                .andExpect(status().isCreated());

        verify(userProvisioningService, times(1))
                .provisionUser(eq(userId), eq(Set.of()), eq(Set.of()));
    }

    @Test
    void provisionUser_givenServiceThrows_thenShouldReturnInternalServerError() throws Exception {
        UUID userId = UUID.randomUUID();

        doThrow(new RuntimeException("Service error"))
                .when(userProvisioningService).provisionUser(eq(userId), any(), any());

        mockMvc.perform(post("/api/users/provision")
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "%s",
                                  "metadata": {}
                                }
                                """.formatted(userId)))
                .andExpect(status().isInternalServerError());
    }
}
