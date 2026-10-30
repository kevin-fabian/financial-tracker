package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.AccountSummary;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.SyncService;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.web.controllers.helper.TransactionServiceTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static com.fabiankevin.app.models.enums.AccountType.E_WALLET;
import static com.fabiankevin.app.models.enums.TransactionType.EXPENSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SyncControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

    @MockitoBean
    private UserClient userClient;

    @MockitoSpyBean
    private JpaTransactionRepository jpaTransactionRepository;

    @Autowired
    private SyncService syncService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private JpaCategoryRepository jpaCategoryRepository;

    @Autowired
    private JpaAccountRepository jpaAccountRepository;

    @Autowired
    private TransactionServiceTestHelper transactionHelper;

    private UUID userId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        jpaTransactionRepository.deleteAll();
        jpaCategoryRepository.deleteAll();
        jpaAccountRepository.deleteAll();
    }

    @Nested
    class PrepareSync {

        @Test
        void givenValidRequest_thenReturnsOkAndDeletesAllUserData() throws Exception {
            // Seed data via service layer
            createTestData(userId);

            mockMvc.perform(post("/api/sync/prepare")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk());

            // Verify all data deleted
            assertThat(jpaTransactionRepository.count()).isZero();
            assertThat(jpaCategoryRepository.count()).isZero();
            assertThat(jpaAccountRepository.count()).isZero();
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/sync/prepare"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenUserWithNoData_thenReturnsOk() throws Exception {
            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("Test").lastName("User").build()));

            mockMvc.perform(post("/api/sync/prepare")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk());
        }

        @Test
        void givenTransactionDeleteFails_thenReturnsInternalServerErrorAndRollsBack() throws Exception {
            // Seed data via service layer
            createTestData(userId);

            // Verify data exists before sync
            assertThat(jpaTransactionRepository.count()).isGreaterThan(0);
            assertThat(jpaCategoryRepository.count()).isGreaterThan(0);
            assertThat(jpaAccountRepository.count()).isGreaterThan(0);

            // Make the spy throw on deleteAllByAccountUserId to simulate a database error mid-sync
            doThrow(new RuntimeException("database error")).when(jpaTransactionRepository).deleteAllByAccountUserId(userId);

            // Call sync — should return 500
            mockMvc.perform(post("/api/sync/prepare")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isInternalServerError());

            // Verify data was rolled back — all original records should still exist
            assertThat(jpaTransactionRepository.count()).isGreaterThan(0);
            assertThat(jpaCategoryRepository.count()).isGreaterThan(0);
            assertThat(jpaAccountRepository.count()).isGreaterThan(0);
        }

        private void createTestData(UUID uid) {
            Category category = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("Groceries")
                            .type(EXPENSE)
                            .userId(uid)
                            .build());

            AccountSummary accountSummary = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("GCASH")
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .userId(uid)
                            .build());

            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(100.0)
                            .transactionDate(LocalDate.now())
                            .categoryId(category.id())
                            .accountId(accountSummary.account().id())
                            .userId(uid)
                            .build());
        }
    }
}
