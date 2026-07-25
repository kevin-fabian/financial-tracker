package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetPeriod;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.services.BudgetService;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import com.fabiankevin.app.services.commands.budgets.PatchBudgetCommand;
import com.fabiankevin.app.web.controllers.dtos.CreateBudgetRequest;
import com.fabiankevin.app.web.controllers.dtos.PatchBudgetRequest;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@Import({GlobalExceptionHandler.class})
@WebMvcTest(BudgetController.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BudgetService budgetService;

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
    class CreateBudget {
        @Test
        void givenValidRequest_thenShouldReturnCreated() throws Exception {
            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(UUID.randomUUID())
                    .allocated(500.0)
                    .build();

            when(budgetService.createBudget(any())).thenAnswer(invocation -> {
                UUID id = UUID.randomUUID();
                CreateBudgetCommand command = invocation.getArgument(0);
                return Budget.builder()
                        .id(id)
                        .userId(command.userId())
                        .lastUpdatedBy(command.userId())
                        .period(command.period())
                        .category(com.fabiankevin.app.models.Category.builder()
                                .id(command.categoryId())
                                .name("GROCERIES")
                                .type(com.fabiankevin.app.models.enums.TransactionType.EXPENSE)
                                .userId(command.userId())
                                .icon("local_grocery_store")
                                .active(true)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build())
                        .allocated(command.allocated())
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
            });

            mockMvc.perform(post("/api/budgets")
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/api/budgets/[-a-f0-9]{36}")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.period").value("MONTHLY"))
                    .andExpect(jsonPath("$.categoryId").value(request.categoryId().toString()))
                    .andExpect(jsonPath("$.categoryName").value("GROCERIES"))
                    .andExpect(jsonPath("$.categoryIcon").value("local_grocery_store"))
                    .andExpect(jsonPath("$.allocated").value(500.0))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());

            verify(budgetService, times(1)).createBudget(any());
        }

        @Test
        void givenCategoryNotFound_thenShouldReturnNotFound() throws Exception {
            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(UUID.randomUUID())
                    .allocated(500.0)
                    .build();

            doThrow(new com.fabiankevin.app.exceptions.CategoryNotFoundException())
                    .when(budgetService).createBudget(any());

            mockMvc.perform(post("/api/budgets")
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());

            verify(budgetService, times(1)).createBudget(any());
        }

        @Test
        void givenNoJwt_thenShouldReturnForbidden() throws Exception {
            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(UUID.randomUUID())
                    .allocated(500.0)
                    .build();

            mockMvc.perform(post("/api/budgets")
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(budgetService);
        }
    }

    @Nested
    class GetBudgets {
        @Test
        void givenExistingBudgets_thenShouldReturnBudgetSummaries() throws Exception {
            UUID userId = UUID.fromString(jwt.getSubject());
            UUID categoryId = UUID.randomUUID();

            when(budgetService.getBudgetsByUserId(userId)).thenReturn(List.of(
                    BudgetSummary.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .lastUpdatedBy(userId)
                            .period(BudgetPeriod.MONTHLY)
                            .categoryId(categoryId)
                            .categoryName("GROCERIES")
                            .categoryIcon("local_grocery_store")
                            .allocated(500.0)
                            .spent(200.0)
                            .spentPercentage(40.0)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()));

            mockMvc.perform(get("/api/budgets")
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").isNotEmpty())
                    .andExpect(jsonPath("$[0].period").value("MONTHLY"))
                    .andExpect(jsonPath("$[0].categoryId").value(categoryId.toString()))
                    .andExpect(jsonPath("$[0].categoryName").value("GROCERIES"))
                    .andExpect(jsonPath("$[0].categoryIcon").value("local_grocery_store"))
                    .andExpect(jsonPath("$[0].allocated").value(500.0))
                    .andExpect(jsonPath("$[0].spent").value(200.0))
                    .andExpect(jsonPath("$[0].spentPercentage").value(40.0))
                    .andExpect(jsonPath("$[0].createdAt").exists())
                    .andExpect(jsonPath("$[0].updatedAt").exists());

            verify(budgetService, times(1)).getBudgetsByUserId(userId);
        }

        @Test
        void givenNoJwt_thenShouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/api/budgets"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(budgetService);
        }
    }

    @Nested
    class PatchBudget {
        @Test
        void givenValidRequest_thenShouldReturnUpdatedBudget() throws Exception {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.fromString(jwt.getSubject());
            UUID categoryId = UUID.randomUUID();

            PatchBudgetRequest request = PatchBudgetRequest.builder()
                    .period(BudgetPeriod.YEARLY)
                    .allocated(1000.0)
                    .build();

            when(budgetService.patchBudget(any())).thenAnswer(invocation -> {
                PatchBudgetCommand command = invocation.getArgument(0);
                return Budget.builder()
                        .id(command.id())
                        .userId(userId)
                        .lastUpdatedBy(userId)
                        .period(command.period())
                        .category(com.fabiankevin.app.models.Category.builder()
                                .id(categoryId)
                                .name("GROCERIES")
                                .type(com.fabiankevin.app.models.enums.TransactionType.EXPENSE)
                                .userId(userId)
                                .icon("local_grocery_store")
                                .active(true)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build())
                        .allocated(command.allocated())
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
            });

            mockMvc.perform(patch("/api/budgets/" + id)
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.period").value("YEARLY"))
                    .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
                    .andExpect(jsonPath("$.categoryName").value("GROCERIES"))
                    .andExpect(jsonPath("$.categoryIcon").value("local_grocery_store"))
                    .andExpect(jsonPath("$.allocated").value(1000.0))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());

            verify(budgetService, times(1)).patchBudget(any());
        }

        @Test
        void givenBudgetNotFound_thenShouldReturnNotFound() throws Exception {
            UUID id = UUID.randomUUID();

            PatchBudgetRequest request = PatchBudgetRequest.builder()
                    .allocated(1000.0)
                    .build();

            doThrow(new com.fabiankevin.app.exceptions.BudgetNotFoundException())
                    .when(budgetService).patchBudget(any());

            mockMvc.perform(patch("/api/budgets/" + id)
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());

            verify(budgetService, times(1)).patchBudget(any());
        }
    }
}
