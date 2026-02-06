package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.*;
import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.services.queries.SummaryQuery;
import com.fabiankevin.app.web.controllers.dtos.CreateTransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void getSummary_givenValidParams_thenShouldReturnSummary() throws Exception {
        SummarySeries summary = new SummarySeries(SummaryType.CATEGORY,
                List.of(new SummaryPoint("FOOD", BigDecimal.valueOf(123))));

        when(transactionService.getSummary(any())).thenReturn(summary);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/transactions/summary")
                        .with(jwt().jwt(jwt))
                        .param("type", "CATEGORY")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CATEGORY"))
                .andExpect(jsonPath("$.points[0].label").value("FOOD"))
                .andExpect(jsonPath("$.points[0].total").value(123));

        ArgumentCaptor<SummaryQuery> captor = ArgumentCaptor.forClass(SummaryQuery.class);
        verify(transactionService, times(1)).getSummary(captor.capture());
        SummaryQuery captured = captor.getValue();

        assertEquals("CATEGORY", captured.type().name(), "type should match request param");
        assertEquals("2026-01-01", captured.from().toString(), "from date should match request param");
        assertEquals("2026-12-31", captured.to().toString(), "to date should match request param");
        assertNotNull(captured.userIds(), "userIds should not be null");
        assertEquals(1, captured.userIds().size(), "userIds should contain one entry extracted from JWT");
        assertEquals(jwt.getSubject(), captured.userIds().getFirst().toString(), "userId should be extracted from JWT subject");
    }

    @Test
    void getTransactions_givenValidParams_thenShouldReturnPagedResponse() throws Exception {
        UUID userId = UUID.fromString(jwt.getSubject());
        PageQuery query = new PageQuery(0, 2, "transactionDate", "ASC");

        Transaction t1 = Transaction.builder()
                .id(UUID.randomUUID())
                .account(Account.builder().id(UUID.randomUUID()).userId(userId).name("A1").currency(Currency.getInstance("PHP")).build())
                .category(Category.builder().id(UUID.randomUUID()).userId(userId).name("FOOD").build())
                .type(TransactionType.EXPENSE)
                .amount(Amount.of(100, Currency.getInstance("PHP")))
                .description("t1")
                .transactionDate(LocalDate.of(2026,1,1))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Transaction t2 = Transaction.builder()
                .id(UUID.randomUUID())
                .account(Account.builder().id(UUID.randomUUID()).userId(userId).name("A1").currency(Currency.getInstance("PHP")).build())
                .category(Category.builder().id(UUID.randomUUID()).userId(userId).name("FOOD").build())
                .type(TransactionType.EXPENSE)
                .amount(Amount.of(200, Currency.getInstance("PHP")))
                .description("t2")
                .transactionDate(LocalDate.of(2026,1,2))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(transactionService.getTransactionsByPageQuery(query, userId))
                .thenReturn(new Page<>(List.of(t1, t2), 0, 2, 2L, 1, true, true));

        mockMvc.perform(get("/api/transactions?page=0&size=2&sort=transactionDate&direction=ASC")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(transactionService, times(1)).getTransactionsByPageQuery(query, userId);
    }

    @Test
    void getTransactions_givenNoContent_thenShouldReturnEmptyPage() throws Exception {
        UUID userId = UUID.fromString(jwt.getSubject());

        when(transactionService.getTransactionsByPageQuery(any(PageQuery.class), eq(userId)))
                .thenReturn(new Page<>(List.of(), 0, 10, 0L, 0, false, true));

        mockMvc.perform(get("/api/transactions?page=0&size=10&sort=transactionDate&direction=ASC")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        verify(transactionService, times(1)).getTransactionsByPageQuery(argThat(
                pageQuery -> pageQuery.page() == 0
                        && pageQuery.size() == 10
                        && pageQuery.sort().equals("transactionDate")
                        && pageQuery.direction().equals("ASC")
        ), eq(userId));
    }
}
