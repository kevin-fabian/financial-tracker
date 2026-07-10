package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.services.UserProvisioningService;
import com.github.fabiankevin.lemon.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserCreatedEventController.class)
@Import(GlobalExceptionHandler.class)
class UserCreatedEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProvisioningService userProvisioningService;

    @Test
    void provisionUser_givenValidEventWithInterests_thenShouldProvisionAndReturnCreated() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/users/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "metadata": {
                                    "accountInterests": "gcash,maya",
                                    "categoryInterests": "groceries,bills,salary_active"
                                  }
                                }
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.provisionedAccounts").isArray())
                .andExpect(jsonPath("$.provisionedAccounts[0]").value("GCash"))
                .andExpect(jsonPath("$.provisionedAccounts[1]").value("Maya"))
                .andExpect(jsonPath("$.provisionedCategories").isArray())
                .andExpect(jsonPath("$.provisionedCategories[0]").value("Groceries"))
                .andExpect(jsonPath("$.provisionedCategories[1]").value("Utilities"))
                .andExpect(jsonPath("$.provisionedCategories[2]").value("Subscriptions"))
                .andExpect(jsonPath("$.provisionedCategories[3]").value("Salary"));

        verify(userProvisioningService, times(1))
                .provisionUser(eq(userId), eq(Set.of("gcash", "maya")), eq(Set.of("groceries", "bills", "salary_active")));
    }

    @Test
    void provisionUser_givenEventWithoutMetadata_thenShouldProvisionWithEmptyInterests() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/users/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "metadata": {}
                                }
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.provisionedAccounts").isArray())
                .andExpect(jsonPath("$.provisionedAccounts").isEmpty())
                .andExpect(jsonPath("$.provisionedCategories").isArray())
                .andExpect(jsonPath("$.provisionedCategories").isEmpty());

        verify(userProvisioningService, times(1))
                .provisionUser(eq(userId), eq(Set.of()), eq(Set.of()));
    }

    @Test
    void provisionUser_givenEventWithNullMetadata_thenShouldProvisionWithEmptyInterests() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/users/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "metadata": null
                                }
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.provisionedAccounts").isArray())
                .andExpect(jsonPath("$.provisionedAccounts").isEmpty())
                .andExpect(jsonPath("$.provisionedCategories").isArray())
                .andExpect(jsonPath("$.provisionedCategories").isEmpty());

        verify(userProvisioningService, times(1))
                .provisionUser(eq(userId), eq(Set.of()), eq(Set.of()));
    }

    @Test
    void provisionUser_givenEventWithUnknownInterests_thenShouldIgnoreUnknown() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/users/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "metadata": {
                                    "accountInterests": "unknown_wallet",
                                    "categoryInterests": "unknown_category"
                                  }
                                }
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.provisionedAccounts").isArray())
                .andExpect(jsonPath("$.provisionedAccounts").isEmpty())
                .andExpect(jsonPath("$.provisionedCategories").isArray())
                .andExpect(jsonPath("$.provisionedCategories").isEmpty());

        verify(userProvisioningService, times(1))
                .provisionUser(eq(userId), eq(Set.of("unknown_wallet")), eq(Set.of("unknown_category")));
    }

    @Test
    void provisionUser_givenEventWithMissingUserId_thenShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/users/provision")
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
    void provisionUser_givenEventWithMissingMetadata_thenShouldReturnBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/users/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s"
                                }
                                """.formatted(userId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userProvisioningService);
    }

    @Test
    void provisionUser_givenServiceThrows_thenShouldReturnInternalServerError() throws Exception {
        UUID userId = UUID.randomUUID();

        doThrow(new RuntimeException("Service error"))
                .when(userProvisioningService).provisionUser(eq(userId), any(), any());

        mockMvc.perform(post("/api/users/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "metadata": {
                                    "accountInterests": "gcash",
                                    "categoryInterests": "groceries"
                                  }
                                }
                                """.formatted(userId)))
                .andExpect(status().isInternalServerError());
    }
}
