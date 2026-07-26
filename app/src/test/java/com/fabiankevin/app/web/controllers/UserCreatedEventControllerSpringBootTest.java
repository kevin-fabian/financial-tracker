package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.UserProvisioningService;
import com.fabiankevin.app.services.queries.PageQuery;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserCreatedEventControllerSpringBootTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean
    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;
    @Autowired
    private UserProvisioningService userProvisioningService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private CategoryService categoryService;

    @Nested
    class ProvisionUser {
        @Test
        void givenNewEventWithInterests_thenShouldProvisionAndReturnCreated() throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(post("/api/users/provision")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("user:provision"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("identity-service"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of("user:provision"))
                                    ))
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
                                          "groceries",
                                          "bills",
                                          "salary_active"
                                        ]
                                      }
                                    }
                                    """.formatted(userId)))
                    .andExpect(status().isCreated());

            Page<Account> accounts = accountService.getAccountsByPageAndUserId(PageQuery.withDefaults(), userId);
            assertEquals(3L, accounts.totalElements(), "default Cash Wallet + GCash + Maya accounts");

            long categoryCount = categoryCount(userId);
            assertEquals(7L, categoryCount, "default + groceries + bills + salary_active categories");
        }

        @Test
        void givenEventWithoutMetadata_thenShouldProvisionDefaults() throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(post("/api/users/provision")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("user:provision"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("identity-service"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of("user:provision"))
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "id": "%s",
                                      "metadata": {}
                                    }
                                    """.formatted(userId)))
                    .andExpect(status().isCreated());

            assertEquals(1L, accountService.getAccountsByPageAndUserId(PageQuery.withDefaults(), userId).totalElements(),
                    "default Cash Wallet account");
            assertEquals(3L, categoryCount(userId), "default Food & Dining, Transportation, Side Hustle categories");
        }

        @Test
        void givenEventWithNullMetadata_thenShouldProvisionDefaults() throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(post("/api/users/provision")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("user:provision"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("identity-service"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of("user:provision"))
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "id": "%s",
                                      "metadata": null
                                    }
                                    """.formatted(userId)))
                    .andExpect(status().isCreated());

            assertEquals(1L, accountService.getAccountsByPageAndUserId(PageQuery.withDefaults(), userId).totalElements(),
                    "default Cash Wallet account");
            assertEquals(3L, categoryCount(userId), "default Food & Dining, Transportation, Side Hustle categories");
        }

        @Test
        void givenEventWithMissingUserId_thenShouldReturnBadRequest() throws Exception {
            mockMvc.perform(post("/api/users/provision")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("user:provision"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("identity-service"))
                                            .claim("sub", UUID.randomUUID())
                                            .claim("scope", List.of("user:provision"))
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "metadata": {
                                        "accountInterests": "gcash"
                                      }
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenEventWithMissingMetadata_thenShouldProvisionDefaults() throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(post("/api/users/provision")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("user:provision"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("identity-service"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of("user:provision"))
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "id": "%s"
                                    }
                                    """.formatted(userId)))
                    .andExpect(status().isCreated());

            assertEquals(1L, accountService.getAccountsByPageAndUserId(PageQuery.withDefaults(), userId).totalElements(),
                    "default Cash Wallet account");
            assertEquals(3L, categoryCount(userId), "default Food & Dining, Transportation, Side Hustle categories");
        }

        @Test
        void givenNoJwt_thenShouldReturnForbidden() throws Exception {
            mockMvc.perform(post("/api/users/provision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "id": "%s",
                                      "metadata": {}
                                    }
                                    """.formatted(UUID.randomUUID())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenJwtWithNoAuthorities_thenShouldReturnForbidden() throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(post("/api/users/provision")
                            .with(jwt()
                                    .jwt(jwt -> jwt
                                            .audience(List.of("identity-service"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "id": "%s",
                                      "metadata": {}
                                    }
                                    """.formatted(userId)))
                    .andExpect(status().isForbidden());
        }
    }

    private long categoryCount(UUID userId) {
        return categoryService.getCategoriesByPageQuery(PageQuery.withDefaults(), userId, TransactionType.EXPENSE).totalElements()
                + categoryService.getCategoriesByPageQuery(PageQuery.withDefaults(), userId, TransactionType.INCOME).totalElements();
    }
}
