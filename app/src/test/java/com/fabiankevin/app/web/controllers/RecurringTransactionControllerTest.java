package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionStatus;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;
import com.fabiankevin.app.models.recurring_transactions.TransactionStatus;
import com.fabiankevin.app.services.RecurringTransactionService;
import com.fabiankevin.app.web.controllers.dtos.CreateRecurringTransactionRequest;
import com.github.fabiankevin.lemon.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import({GlobalExceptionHandler.class})
@WebMvcTest(RecurringTransactionController.class)
class RecurringTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecurringTransactionService recurringTransactionService;

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

    @Nested
    class Create {

        @Test
        void givenValidRequest_thenReturnsCreatedWithSummary() throws Exception {
            UUID categoryId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();

            CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(categoryId)
                    .accountId(accountId)
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("GROCERIES")
                    .type(TransactionType.EXPENSE)
                    .userId(UUID.fromString(jwt.getSubject()))
                    .icon("local_grocery_store")
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Account account = Account.builder()
                    .id(accountId)
                    .name("Checking")
                    .userId(UUID.fromString(jwt.getSubject()))
                    .currency(Currency.getInstance("USD"))
                    .type(AccountType.CASH)
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            User user = User.builder()
                    .id(UUID.fromString(jwt.getSubject()))
                    .firstName("Kevin")
                    .lastName("Fabian")
                    .build();

            when(recurringTransactionService.create(any())).thenReturn(RecurringTransactionSummary.builder()
                    .id(UUID.randomUUID())
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .category(category)
                    .account(account)
                    .dayOfMonth(15)
                    .nextOccurrenceDate(ZonedDateTime.now().plusMonths(1).withDayOfMonth(15))
                    .endDate(ZonedDateTime.now().plusMonths(6))
                    .remainingDays(5)
                    .transactionStatus(TransactionStatus.UPCOMING)
                    .status(RecurringTransactionStatus.ACTIVE)
                    .user(user)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());

            mockMvc.perform(post("/api/recurring-transactions")
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.description").value("Monthly subscription"))
                    .andExpect(jsonPath("$.amount").value(15.99))
                    .andExpect(jsonPath("$.variableAmount").value(false))
                    .andExpect(jsonPath("$.categoryName").value("GROCERIES"))
                    .andExpect(jsonPath("$.accountName").value("Checking"))
                    .andExpect(jsonPath("$.dayOfMonth").value(15))
                    .andExpect(jsonPath("$.remainingDays").value(5))
                    .andExpect(jsonPath("$.transactionStatus").value("UPCOMING"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.firstName").value("Kevin"))
                    .andExpect(jsonPath("$.lastName").value("Fabian"))
                    .andExpect(jsonPath("$.initial").value("KF"));

            verify(recurringTransactionService).create(any());
        }

        @Test
        void givenNoJwt_thenReturnsForbidden() throws Exception {
            CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(UUID.randomUUID())
                    .accountId(UUID.randomUUID())
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            mockMvc.perform(post("/api/recurring-transactions")
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(recurringTransactionService);
        }
    }
}
