package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Amount;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.web.controllers.dtos.CreateTransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Autowired
    private JsonMapper objectMapper;
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
    void createTransaction_givenValidRequest_thenShouldCreateTransaction() throws Exception {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(Amount.of(100, Currency.getInstance("PHP")))
                .type(TransactionType.EXPENSE)
                .description("Dinner")
                .transactionDate(LocalDate.of(2026, 1, 1))
                .categoryId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .build();

        when(transactionService.addTransaction(any())).thenAnswer(invocation -> {
            AddTransactionCommand command = invocation.getArgument(0);

            return Transaction.builder()
                    .id(UUID.randomUUID())
                    .account(Account.builder()
                            .id(command.accountId())
                            .userId(command.userId())
                            .name("GCASH")
                            .currency(Currency.getInstance("PHP"))
                            .build())
                    .category(Category.builder()
                            .id(command.categoryId())
                            .userId(command.userId())
                            .name("FOOD")
                            .build())
                    .type(command.type())
                    .amount(command.amount())
                    .description(command.description())
                    .transactionDate(command.transactionDate())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });

        mockMvc.perform(post("/api/transactions")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/api/transactions/[-a-f0-9]{36}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.description").value("Dinner"))
                .andExpect(jsonPath("$.amount.value").value(100))
                .andExpect(jsonPath("$.amount.currency").value("PHP"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.transactionDate").value("2026-01-01"))
                .andExpect(jsonPath("$.account.id").isNotEmpty())
                .andExpect(jsonPath("$.account.name").value("GCASH"))
                .andExpect(jsonPath("$.category.id").isNotEmpty())
                .andExpect(jsonPath("$.category.name").value("FOOD"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(transactionService, times(1)).addTransaction(any());
    }

    @Test
    void createTransaction_givenMissingRequiredFields_thenShouldReturnBadRequest() throws Exception {
        CreateTransactionRequest invalidRequest = CreateTransactionRequest.builder()
                .type(TransactionType.EXPENSE)
                .description("Dinner")
                .transactionDate(LocalDate.now())
                .categoryId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/transactions")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(transactionService, times(0)).addTransaction(any());
    }
}
