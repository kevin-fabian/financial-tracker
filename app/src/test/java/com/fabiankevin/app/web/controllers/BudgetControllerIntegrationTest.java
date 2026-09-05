package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetPeriod;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.services.BudgetService;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import com.fabiankevin.app.web.controllers.dtos.budgets.CreateBudgetRequest;
import com.fabiankevin.app.web.controllers.dtos.budgets.PatchBudgetRequest;
import com.fabiankevin.app.web.controllers.dtos.party.HouseholdResponse;
import com.fabiankevin.app.web.controllers.helper.HouseholdServiceTestHelper;
import com.fabiankevin.app.web.controllers.helper.TransactionServiceTestHelper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.fabiankevin.app.models.enums.TransactionType.EXPENSE;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BudgetControllerIntegrationTest {
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
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private JpaCategoryRepository jpaCategoryRepository;
    @Autowired
    private JsonMapper jsonMapper;
    @Autowired
    private HouseholdServiceTestHelper householdHelper;
    @Autowired
    private TransactionServiceTestHelper transactionHelper;

    @Nested
    class GetBudgets {

        @Test
        void givenBudgetCategoryWithTransactions_thenShouldReturnSummaryWithSpent() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            transactionHelper.createTransaction(userId, TransactionType.EXPENSE, 150.0, "GROCERIES");
            transactionHelper.createTransaction(userId, TransactionType.EXPENSE, 50.0, "GROCERIES");
            createBudget(userId, category, 500.0);

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.get(0).equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(get("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].user.id").value(userId.toString()))
                    .andExpect(jsonPath("$[0].user.firstName").value("John"))
                    .andExpect(jsonPath("$[0].user.lastName").value("Doe"))
                    .andExpect(jsonPath("$[0].user.initial").value("JD"))
                    .andExpect(jsonPath("$[0].updatedBy.id").value(userId.toString()))
                    .andExpect(jsonPath("$[0].updatedBy.firstName").value("John"))
                    .andExpect(jsonPath("$[0].updatedBy.lastName").value("Doe"))
                    .andExpect(jsonPath("$[0].updatedBy.initial").value("JD"))
                    .andExpect(jsonPath("$[0].createdAt").exists())
                    .andExpect(jsonPath("$[0].updatedAt").exists())
                    .andExpect(jsonPath("$[0].period").value("MONTHLY"))
                    .andExpect(jsonPath("$[0].category.id").value(category.id().toString()))
                    .andExpect(jsonPath("$[0].category.name").value("GROCERIES"))
                    .andExpect(jsonPath("$[0].category.icon").value("local_grocery_store"))
                    .andExpect(jsonPath("$[0].allocated").value(500.0))
                    .andExpect(jsonPath("$[0].spent").value(200.0))
                    .andExpect(jsonPath("$[0].spentPercentage").value(40.0));
        }

        @Test
        void givenNoBudgets_thenShouldReturnEmpty() throws Exception {
            UUID userId = UUID.randomUUID();
            mockMvc.perform(get("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void givenTransactionsFromLastMonth_thenShouldNotIncludeInSpent() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            transactionHelper.createTransaction(userId, EXPENSE, 150.0, "GROCERIES", lastMonth);
            transactionHelper.createTransaction(userId, EXPENSE, 50.0, "GROCERIES", lastMonth);
            createBudget(userId, category, 500.0);

            when(userClient.getUsersByIds(argThat(ids -> ids.size() == 1 && ids.get(0).equals(userId))))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(get("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].user.id").value(userId.toString()))
                    .andExpect(jsonPath("$[0].user.firstName").value("John"))
                    .andExpect(jsonPath("$[0].user.lastName").value("Doe"))
                    .andExpect(jsonPath("$[0].user.initial").value("JD"))
                    .andExpect(jsonPath("$[0].updatedBy.id").value(userId.toString()))
                    .andExpect(jsonPath("$[0].updatedBy.firstName").value("John"))
                    .andExpect(jsonPath("$[0].updatedBy.lastName").value("Doe"))
                    .andExpect(jsonPath("$[0].updatedBy.initial").value("JD"))
                    .andExpect(jsonPath("$[0].createdAt").exists())
                    .andExpect(jsonPath("$[0].updatedAt").exists())
                    .andExpect(jsonPath("$[0].period").value("MONTHLY"))
                    .andExpect(jsonPath("$[0].category.id").value(category.id().toString()))
                    .andExpect(jsonPath("$[0].category.name").value("GROCERIES"))
                    .andExpect(jsonPath("$[0].category.icon").value("local_grocery_store"))
                    .andExpect(jsonPath("$[0].allocated").value(500.0))
                    .andExpect(jsonPath("$[0].spent").value(0.0))
                    .andExpect(jsonPath("$[0].spentPercentage").value(0.0));
        }

        @Test
        void givenTwoUsersWithBudgetsNoParty_thenReturnsOnlyOwnBudgets() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            Category userCategory = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Category otherCategory = createCategory(otherUserId, "DINING", TransactionType.EXPENSE, "restaurant");
            createBudget(userId, userCategory, 500.0);
            createBudget(otherUserId, otherCategory, 300.0);

            // Mock user client for both users (enrichment still needs both)
            when(userClient.getUsersByIds(argThat(ids -> ids.contains(userId))))
                    .thenReturn(
                            List.of(
                                    User.builder().id(userId).firstName("Alice").lastName("Smith").build(),
                                    User.builder().id(otherUserId).firstName("Bob").lastName("Jones").build()
                            )
                    );

            mockMvc.perform(get("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].user.id").value(userId.toString()))
                    .andExpect(jsonPath("$[0].user.firstName").value("Alice"))
                    .andExpect(jsonPath("$[0].user.lastName").value("Smith"))
                    .andExpect(jsonPath("$[0].user.initial").value("AS"))
                    .andExpect(jsonPath("$[0].updatedBy.id").value(userId.toString()))
                    .andExpect(jsonPath("$[0].updatedBy.firstName").value("Alice"))
                    .andExpect(jsonPath("$[0].updatedBy.lastName").value("Smith"))
                    .andExpect(jsonPath("$[0].updatedBy.initial").value("AS"))
                    .andExpect(jsonPath("$[0].period").value("MONTHLY"))
                    .andExpect(jsonPath("$[0].category.name").value("GROCERIES"))
                    .andExpect(jsonPath("$[0].category.icon").value("local_grocery_store"))
                    .andExpect(jsonPath("$[0].allocated").value(500.0))
                    .andExpect(jsonPath("$[0].spent").value(0.0))
                    .andExpect(jsonPath("$[0].spentPercentage").value(0.0));
        }

        @Test
        void givenUserWithPartyMembers_thenReturnsConsolidatedBudgets() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            // Set up user mocks before party operations
            when(userClient.getUserByEmail("invitee@example.com"))
                    .thenReturn(User.builder().id(otherUserId).firstName("Bob").lastName("Jones").build());
            when(userClient.getUsersByIds(argThat(ids -> ids.contains(userId) && ids.contains(otherUserId))))
                    .thenReturn(
                            List.of(
                                    User.builder().id(userId).firstName("Alice").lastName("Smith").build(),
                                    User.builder().id(otherUserId).firstName("Bob").lastName("Jones").build()
                            )
                    );

            // Create party and invite + accept via helper
            HouseholdResponse householdResponse = householdHelper.createHouseHold(userId);
            householdHelper.inviteAndAccept(householdResponse.id(), userId, otherUserId, "invitee@example.com");

            // Create categories and budgets for both users
            Category userCategory = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Category otherCategory = createCategory(otherUserId, "DINING", TransactionType.EXPENSE, "restaurant");
            createBudget(userId, userCategory, 500.0);
            createBudget(otherUserId, otherCategory, 300.0);

            mockMvc.perform(get("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    // First budget (DINING) — belongs to otherUserId (Bob Jones)
                    .andExpect(jsonPath("$[0].user.id").value(otherUserId.toString()))
                    .andExpect(jsonPath("$[0].user.firstName").value("Bob"))
                    .andExpect(jsonPath("$[0].user.lastName").value("Jones"))
                    .andExpect(jsonPath("$[0].user.initial").value("BJ"))
                    .andExpect(jsonPath("$[0].updatedBy.id").value(otherUserId.toString()))
                    .andExpect(jsonPath("$[0].updatedBy.firstName").value("Bob"))
                    .andExpect(jsonPath("$[0].updatedBy.lastName").value("Jones"))
                    .andExpect(jsonPath("$[0].updatedBy.initial").value("BJ"))
                    .andExpect(jsonPath("$[0].createdAt").exists())
                    .andExpect(jsonPath("$[0].updatedAt").exists())
                    .andExpect(jsonPath("$[0].period").value("MONTHLY"))
                    .andExpect(jsonPath("$[0].category").exists())
                    .andExpect(jsonPath("$[0].category.name").value("DINING"))
                    .andExpect(jsonPath("$[0].category.icon").value("restaurant"))
                    .andExpect(jsonPath("$[0].allocated").value(300.0))
                    .andExpect(jsonPath("$[0].spent").value(0.0))
                    .andExpect(jsonPath("$[0].spentPercentage").value(0.0))
                    // Second budget (GROCERIES) — belongs to userId (Alice Smith)
                    .andExpect(jsonPath("$[1].user.id").value(userId.toString()))
                    .andExpect(jsonPath("$[1].user.firstName").value("Alice"))
                    .andExpect(jsonPath("$[1].user.lastName").value("Smith"))
                    .andExpect(jsonPath("$[1].user.initial").value("AS"))
                    .andExpect(jsonPath("$[1].updatedBy.id").value(userId.toString()))
                    .andExpect(jsonPath("$[1].updatedBy.firstName").value("Alice"))
                    .andExpect(jsonPath("$[1].updatedBy.lastName").value("Smith"))
                    .andExpect(jsonPath("$[1].updatedBy.initial").value("AS"))
                    .andExpect(jsonPath("$[1].createdAt").exists())
                    .andExpect(jsonPath("$[1].updatedAt").exists())
                    .andExpect(jsonPath("$[1].period").value("MONTHLY"))
                    .andExpect(jsonPath("$[1].category").exists())
                    .andExpect(jsonPath("$[1].category.name").value("GROCERIES"))
                    .andExpect(jsonPath("$[1].category.icon").value("local_grocery_store"))
                    .andExpect(jsonPath("$[1].allocated").value(500.0))
                    .andExpect(jsonPath("$[1].spent").value(0.0))
                    .andExpect(jsonPath("$[1].spentPercentage").value(0.0));
        }

        @Test
        void givenJwtWithNoAuthorities_thenShouldReturnForbidden() throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(get("/api/budgets")
                            .with(jwt()
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class CreateBudget {
        @Test
        void givenValidRequest_thenShouldReturnCreatedAndAllFields() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(category.id())
                    .allocated(500.0)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/api/budgets/[-a-f0-9]{36}")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.user.id").value(userId.toString()))
                    .andExpect(jsonPath("$.user.firstName").value("John"))
                    .andExpect(jsonPath("$.user.lastName").value("Doe"))
                    .andExpect(jsonPath("$.user.initial").value("JD"))
                    .andExpect(jsonPath("$.updatedBy.id").value(userId.toString()))
                    .andExpect(jsonPath("$.updatedBy.firstName").value("John"))
                    .andExpect(jsonPath("$.updatedBy.lastName").value("Doe"))
                    .andExpect(jsonPath("$.updatedBy.initial").value("JD"))
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.period").value("MONTHLY"))
                    .andExpect(jsonPath("$.category.id").value(category.id().toString()))
                    .andExpect(jsonPath("$.category.name").value("GROCERIES"))
                    .andExpect(jsonPath("$.category.icon").value("local_grocery_store"))
                    .andExpect(jsonPath("$.allocated").value(500.0))
                    .andExpect(jsonPath("$.spent").value(0.0))
                    .andExpect(jsonPath("$.spentPercentage").value(0.0));
        }

        @Test
        void givenCategoryWithExistingTransactions_thenShouldReturnSummaryWithSpent() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            transactionHelper.createTransaction(userId, EXPENSE, 150.0, "GROCERIES");
            transactionHelper.createTransaction(userId, EXPENSE, 50.0, "GROCERIES");

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(category.id())
                    .allocated(500.0)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/api/budgets/[-a-f0-9]{36}")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.period").value("MONTHLY"))
                    .andExpect(jsonPath("$.category.id").value(category.id().toString()))
                    .andExpect(jsonPath("$.category.name").value("GROCERIES"))
                    .andExpect(jsonPath("$.category.icon").value("local_grocery_store"))
                    .andExpect(jsonPath("$.allocated").value(500.0))
                    .andExpect(jsonPath("$.spent").value(200.0))
                    .andExpect(jsonPath("$.spentPercentage").value(40.0));
        }

        @Test
        void givenCategoryNotFound_thenShouldReturnNotFound() throws Exception {
            UUID userId = UUID.randomUUID();

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(UUID.randomUUID())
                    .allocated(500.0)
                    .build();

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
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
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenJwtWithNoAuthorities_thenShouldReturnForbidden() throws Exception {
            UUID userId = UUID.randomUUID();

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(UUID.randomUUID())
                    .allocated(500.0)
                    .build();

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @ParameterizedTest
        @MethodSource("invalidCreateBudgetArguments")
        void givenInvalidCreateBudgetRequestField_thenReturnsBadRequest(
                CreateBudgetRequest request) throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        static Stream<Arguments> invalidCreateBudgetArguments() {
            return Stream.of(
                    Arguments.of(CreateBudgetRequest.builder()
                            .period(BudgetPeriod.MONTHLY)
                            .allocated(500.0)
                            .build()),
                    Arguments.of(CreateBudgetRequest.builder()
                            .period(BudgetPeriod.MONTHLY)
                            .categoryId(UUID.randomUUID())
                            .allocated(0)
                            .build()),
                    Arguments.of(CreateBudgetRequest.builder()
                            .period(BudgetPeriod.MONTHLY)
                            .categoryId(UUID.randomUUID())
                            .allocated(-100)
                            .build()),
                    Arguments.of(CreateBudgetRequest.builder()
                            .period(BudgetPeriod.MONTHLY)
                            .categoryId(UUID.randomUUID())
                            .allocated(-0.01)
                            .build())
            );
        }

        @Test
        void givenExistingBudgetForCategory_thenReturnsConflict() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            createBudget(userId, category, 500.0);

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(category.id())
                    .allocated(1000.0)
                    .build();

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        void givenCategoryBelongsToOtherUser_thenReturnsNotFound() throws Exception {
            UUID currentUserId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            Category otherCategory = createCategory(otherUserId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(otherCategory.id())
                    .allocated(500.0)
                    .build();

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", currentUserId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenAllocatedWithDecimalPrecision_thenReturnsCreatedWithExactValue() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(category.id())
                    .allocated(123.45)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.allocated").value(123.45));
        }

        @Test
        void givenSystemCategory_thenReturnsCreated() throws Exception {
            UUID userId = UUID.randomUUID();
            Category systemCategory = createSystemCategory();

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(systemCategory.id())
                    .allocated(500.0)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/api/budgets/[-a-f0-9]{36}")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.user.id").value(userId.toString()))
                    .andExpect(jsonPath("$.period").value("MONTHLY"))
                    .andExpect(jsonPath("$.category.id").value(systemCategory.id().toString()))
                    .andExpect(jsonPath("$.category.name").value("GROCERIES"))
                    .andExpect(jsonPath("$.category.icon").value("local_grocery_store"))
                    .andExpect(jsonPath("$.allocated").value(500.0))
                    .andExpect(jsonPath("$.spent").value(0.0))
                    .andExpect(jsonPath("$.spentPercentage").value(0.0));
        }
    }

    @Nested
    class PatchBudget {
        @Test
        void givenValidRequest_thenShouldReturnUpdatedBudgetAndAllFields() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            BudgetSummary budgetSummary = createBudget(userId, category, 500.0);
            Budget budget = budgetSummary.budget();

            PatchBudgetRequest request = PatchBudgetRequest.builder()
                    .period(BudgetPeriod.YEARLY)
                    .allocated(1000.0)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(patch("/api/budgets/" + budget.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(budget.id().toString()))
                    .andExpect(jsonPath("$.user.id").value(userId.toString()))
                    .andExpect(jsonPath("$.user.firstName").value("John"))
                    .andExpect(jsonPath("$.user.lastName").value("Doe"))
                    .andExpect(jsonPath("$.user.initial").value("JD"))
                    .andExpect(jsonPath("$.updatedBy.id").value(userId.toString()))
                    .andExpect(jsonPath("$.updatedBy.firstName").value("John"))
                    .andExpect(jsonPath("$.updatedBy.lastName").value("Doe"))
                    .andExpect(jsonPath("$.updatedBy.initial").value("JD"))
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.period").value("YEARLY"))
                    .andExpect(jsonPath("$.category.id").value(category.id().toString()))
                    .andExpect(jsonPath("$.category.name").value("GROCERIES"))
                    .andExpect(jsonPath("$.category.icon").value("local_grocery_store"))
                    .andExpect(jsonPath("$.allocated").value(1000.0))
                    .andExpect(jsonPath("$.spent").value(0.0))
                    .andExpect(jsonPath("$.spentPercentage").value(0.0));
        }

        @Test
        void givenBudgetWithTransactions_thenShouldReturnSummaryWithSpent() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            transactionHelper.createTransaction(userId, EXPENSE, 150.0, "GROCERIES");
            transactionHelper.createTransaction(userId, EXPENSE, 50.0, "GROCERIES");
            BudgetSummary budgetSummary = createBudget(userId, category, 500.0);
            Budget budget = budgetSummary.budget();
            PatchBudgetRequest request = PatchBudgetRequest.builder()
                    .allocated(1000.0)
                    .build();

            mockMvc.perform(patch("/api/budgets/" + budget.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(budget.id().toString()))
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.period").value("MONTHLY"))
                    .andExpect(jsonPath("$.category.id").value(category.id().toString()))
                    .andExpect(jsonPath("$.category.name").value("GROCERIES"))
                    .andExpect(jsonPath("$.category.icon").value("local_grocery_store"))
                    .andExpect(jsonPath("$.allocated").value(1000.0))
                    .andExpect(jsonPath("$.spent").value(200.0))
                    .andExpect(jsonPath("$.spentPercentage").value(20.0));
        }

        @Test
        void givenBudgetNotFound_thenShouldReturnNotFound() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID id = UUID.randomUUID();

            PatchBudgetRequest request = PatchBudgetRequest.builder()
                    .allocated(1000.0)
                    .build();

            mockMvc.perform(patch("/api/budgets/" + id)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenJwtWithNoAuthorities_thenShouldReturnForbidden() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID id = UUID.randomUUID();

            PatchBudgetRequest request = PatchBudgetRequest.builder()
                    .allocated(1000.0)
                    .build();

            mockMvc.perform(patch("/api/budgets/" + id)
                            .with(jwt()
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @ParameterizedTest
        @MethodSource("partialPatchArguments")
        void givenPartialPatchField_thenReturnsUpdatedFieldOnly(
                PatchBudgetRequest request,
                String expectedPeriod,
                Double expectedAllocated) throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            BudgetSummary budgetSummary = createBudget(userId, category, 500.0);
            Budget budget = budgetSummary.budget();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(patch("/api/budgets/" + budget.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(budget.id().toString()))
                    .andExpect(jsonPath("$.period").value(expectedPeriod))
                    .andExpect(jsonPath("$.allocated").value(expectedAllocated));
        }

        static Stream<Arguments> partialPatchArguments() {
            return Stream.of(
                    Arguments.of(
                            PatchBudgetRequest.builder()
                                    .allocated(1000.0)
                                    .build(),
                            "MONTHLY",
                            1000.0
                    ),
                    Arguments.of(
                            PatchBudgetRequest.builder()
                                    .period(BudgetPeriod.YEARLY)
                                    .build(),
                            "YEARLY",
                            500.0
                    )
            );
        }

        @Test
        void givenPatchToExistingCategory_thenReturnsConflict() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category1 = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Category category2 = createCategory(userId, "TRANSPORT", TransactionType.EXPENSE, "transport");
            createBudget(userId, category1, 500.0);
            BudgetSummary budgetSummary = createBudget(userId, category2, 300.0);
            Budget budget = budgetSummary.budget();

            PatchBudgetRequest request = PatchBudgetRequest.builder()
                    .categoryId(category1.id())
                    .build();

            mockMvc.perform(patch("/api/budgets/" + budget.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        void givenPatchWithEmptyRequestBody_thenReturnsBudgetUnchanged() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            BudgetSummary budgetSummary = createBudget(userId, category, 500.0);
            Budget budget = budgetSummary.budget();

            PatchBudgetRequest request = PatchBudgetRequest.builder().build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(patch("/api/budgets/" + budget.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(budget.id().toString()))
                    .andExpect(jsonPath("$.period").value("MONTHLY"))
                    .andExpect(jsonPath("$.allocated").value(500.0))
                    .andExpect(jsonPath("$.category.id").value(category.id().toString()));
        }

        @Test
        void givenPatchWithAllFields_thenReturnsFullyUpdatedBudget() throws Exception {
            UUID userId = UUID.randomUUID();
            Category originalCategory = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Category newCategory = createCategory(userId, "TRANSPORT", TransactionType.EXPENSE, "transport");
            BudgetSummary budgetSummary = createBudget(userId, originalCategory, 500.0);
            Budget budget = budgetSummary.budget();

            PatchBudgetRequest request = PatchBudgetRequest.builder()
                    .period(BudgetPeriod.YEARLY)
                    .categoryId(newCategory.id())
                    .allocated(2000.0)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(patch("/api/budgets/" + budget.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(budget.id().toString()))
                    .andExpect(jsonPath("$.period").value("YEARLY"))
                    .andExpect(jsonPath("$.allocated").value(2000.0))
                    .andExpect(jsonPath("$.category.id").value(newCategory.id().toString()));
        }

        @Test
        void givenPatchAllocatedToZeroOrNegative_thenReturnsUpdated() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            BudgetSummary budgetSummary = createBudget(userId, category, 500.0);
            Budget budget = budgetSummary.budget();

            PatchBudgetRequest request = PatchBudgetRequest.builder()
                    .allocated(-100.0)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(patch("/api/budgets/" + budget.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allocated").value(-100.0));
        }

        @Test
        void givenNewCategoryBelongsToOtherUser_thenReturnsNotFound() throws Exception {
            UUID currentUserId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            Category category = createCategory(currentUserId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            Category otherCategory = createCategory(otherUserId, "TRANSPORT", TransactionType.EXPENSE, "transport");
            BudgetSummary budgetSummary = createBudget(currentUserId, category, 500.0);
            Budget budget = budgetSummary.budget();

            PatchBudgetRequest request = PatchBudgetRequest.builder()
                    .categoryId(otherCategory.id())
                    .build();

            mockMvc.perform(patch("/api/budgets/" + budget.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", currentUserId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeleteBudget {
        @Test
        void givenExistingBudget_thenShouldReturnNoContent() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");
            BudgetSummary summary = createBudget(userId, category, 500.0);

            mockMvc.perform(delete("/api/budgets/" + summary.budget().id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNoContent());
        }

        @Test
        void givenNoJwt_thenShouldReturnForbidden() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/budgets/" + id))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenJwtWithNoAuthorities_thenShouldReturnForbidden() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/budgets/" + id)
                            .with(jwt()
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isForbidden());
        }
    }

    private Category createCategory(UUID userId, String name, TransactionType type, String icon) {
        return categoryService.createCategory(CreateCategoryCommand.builder()
                .name(name)
                .type(type)
                .icon(icon)
                .userId(userId)
                .build());
    }

    private Category createSystemCategory() {
        var entity = com.fabiankevin.app.persistence.entities.CategoryEntity.builder()
                .name("GROCERIES")
                .transactionType(TransactionType.EXPENSE)
                .icon("local_grocery_store")
                .userId(null)
                .active(true)
                .system(true)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        var saved = jpaCategoryRepository.save(entity);
        return saved.toModel();
    }

    private BudgetSummary createBudget(UUID userId, Category category, double allocated) {
        return budgetService.createBudget(CreateBudgetCommand.builder()
                .userId(userId)
                .period(BudgetPeriod.MONTHLY)
                .categoryId(category.id())
                .allocated(allocated)
                .build());
    }
}
