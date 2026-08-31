package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Amount;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.web.controllers.dtos.CreateCategoryRequest;
import com.fabiankevin.app.web.controllers.dtos.PatchCategoryRequest;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerIntegrationTest {
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
    private CategoryRepository categoryRepository;

    @Autowired
    private JpaCategoryRepository jpaCategoryRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private JpaTransactionRepository jpaTransactionRepository;

    @Autowired
    private JsonMapper jsonMapper;

    private UUID userId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        jpaTransactionRepository.deleteAll();
        jpaCategoryRepository.deleteAll();
    }

    @Nested
    class CreateCategory {

        static Stream<Arguments> invalidCreateCategoryRequestTestCases() {
            return Stream.of(
                    Arguments.of("", TransactionType.EXPENSE),
                    Arguments.of(" ", TransactionType.EXPENSE),
                    Arguments.of("   ", TransactionType.EXPENSE),
                    Arguments.of("12345".repeat(100), TransactionType.EXPENSE),
                    Arguments.of((String) null, TransactionType.EXPENSE),
                    Arguments.of("FOOD", (TransactionType) null)
            );
        }

        @Test
        void givenValidRequest_thenReturnsCreatedWithCategoryResponse() throws Exception {
            CreateCategoryRequest request = CreateCategoryRequest.builder()
                    .name("FOOD")
                    .icon("food")
                    .type(TransactionType.EXPENSE)
                    .build();

            mockMvc.perform(post("/api/categories")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/api/categories/[-a-f0-9]{36}")))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").value("FOOD"))
                    .andExpect(jsonPath("$.type").value("EXPENSE"))
                    .andExpect(jsonPath("$.icon").value("food"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.system").value(false))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        }

        @ParameterizedTest
        @MethodSource("invalidCreateCategoryRequestTestCases")
        void givenInvalidCreateCategoryRequest_thenReturnsBadRequest(String name, TransactionType type) throws Exception {
            CreateCategoryRequest request = CreateCategoryRequest.builder()
                    .name(name)
                    .type(type)
                    .build();

            mockMvc.perform(post("/api/categories")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            CreateCategoryRequest request = CreateCategoryRequest.builder()
                    .name("FOOD")
                    .type(TransactionType.EXPENSE)
                    .build();

            mockMvc.perform(post("/api/categories")
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenDuplicateNameAndType_thenReturnsConflict() throws Exception {
            CreateCategoryRequest firstRequest = CreateCategoryRequest.builder()
                    .name("FOOD")
                    .type(TransactionType.EXPENSE)
                    .build();

            mockMvc.perform(post("/api/categories")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(firstRequest)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/categories")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(firstRequest)))
                    .andExpect(status().isConflict());
        }

        @Test
        void givenInactiveCategoryWithDifferentIcon_thenReactivatesWithNewDetails() throws Exception {
            Category inactiveCategory = categoryRepository.save(
                    Category.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .icon(null)
                            .userId(userId)
                            .active(false)
                            .system(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );

            CreateCategoryRequest request = CreateCategoryRequest.builder()
                    .name("FOOD")
                    .type(TransactionType.EXPENSE)
                    .icon("food-new")
                    .build();

            mockMvc.perform(post("/api/categories")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(inactiveCategory.id().toString()))
                    .andExpect(jsonPath("$.name").value("FOOD"))
                    .andExpect(jsonPath("$.type").value("EXPENSE"))
                    .andExpect(jsonPath("$.icon").value("food-new"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.system").value(false));
        }
    }

    @Nested
    class GetCategoryById {

        @Test
        void givenExistingId_thenReturnsCategory() throws Exception {
            Category category = categoryRepository.save(
                    Category.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .active(true)
                            .system(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );

            mockMvc.perform(get("/api/categories/" + category.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(category.id().toString()))
                    .andExpect(jsonPath("$.name").value("FOOD"))
                    .andExpect(jsonPath("$.type").value("EXPENSE"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.system").value(false));
        }

        @Test
        void givenExistingIdWithIcon_thenReturnsCategoryWithIcon() throws Exception {
            Category category = categoryRepository.save(
                    Category.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .icon("food")
                            .active(true)
                            .system(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );

            mockMvc.perform(get("/api/categories/" + category.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(category.id().toString()))
                    .andExpect(jsonPath("$.name").value("FOOD"))
                    .andExpect(jsonPath("$.icon").value("food"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.system").value(false));
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(get("/api/categories/" + id))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenCategoryNotFound_thenReturnNotFound() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(get("/api/categories/" + id)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenCategoryBelongsToOtherUser_thenReturnNotFound() throws Exception {
            UUID otherUserId = UUID.randomUUID();
            Category category = categoryRepository.save(
                    Category.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(otherUserId)
                            .active(true)
                            .system(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );

            mockMvc.perform(get("/api/categories/" + category.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class GetCategories {

        @Test
        void givenCategoriesWithTransactions_thenReturnMultipleCategoriesWithAggregatedFields() throws Exception {
            var savedCategory1 = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .build()
            );
            var savedCategory2 = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("RENT")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .build()
            );

            Account account = accountRepository.save(
                    Account.builder()
                            .name("CASH")
                            .type(AccountType.CASH)
                            .userId(userId)
                            .currency(Currency.getInstance("USD"))
                            .active(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );

            // Create 2 transactions for FOOD category
            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(Amount.of(50.0, "USD"))
                            .transactionDate(LocalDate.now())
                            .categoryId(savedCategory1.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );
            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(Amount.of(50.0, "USD"))
                            .transactionDate(LocalDate.now())
                            .categoryId(savedCategory1.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            // Create 1 transaction for RENT category
            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(Amount.of(100.0, "USD"))
                            .transactionDate(LocalDate.now())
                            .categoryId(savedCategory2.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            mockMvc.perform(get("/api/categories?page=0&size=10&sort=name&direction=ASC&type=EXPENSE")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].id").value(savedCategory1.id().toString()))
                    .andExpect(jsonPath("$.content[0].name").value("FOOD"))
                    .andExpect(jsonPath("$.content[0].type").value("EXPENSE"))
                    .andExpect(jsonPath("$.content[0].icon").doesNotExist())
                    .andExpect(jsonPath("$.content[0].active").value(true))
                    .andExpect(jsonPath("$.content[0].system").value(false))
                    .andExpect(jsonPath("$.content[0].totalAmount").value(100.0))
                    .andExpect(jsonPath("$.content[0].totalTransactions").value(2))
                    .andExpect(jsonPath("$.content[0].percentage").value(50.0))
                    .andExpect(jsonPath("$.content[1].id").value(savedCategory2.id().toString()))
                    .andExpect(jsonPath("$.content[1].name").value("RENT"))
                    .andExpect(jsonPath("$.content[1].type").value("EXPENSE"))
                    .andExpect(jsonPath("$.content[1].icon").doesNotExist())
                    .andExpect(jsonPath("$.content[1].active").value(true))
                    .andExpect(jsonPath("$.content[1].system").value(false))
                    .andExpect(jsonPath("$.content[1].totalAmount").value(100.0))
                    .andExpect(jsonPath("$.content[1].totalTransactions").value(1))
                    .andExpect(jsonPath("$.content[1].percentage").value(50.0))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10));
        }

        @Test
        void givenTypeFilterIncome_thenReturnsFilteredResponse() throws Exception {
            categoryRepository.save(
                    Category.builder()
                            .name("SALARY")
                            .type(TransactionType.INCOME)
                            .userId(userId)
                            .active(true)
                            .system(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );

            mockMvc.perform(get("/api/categories?page=0&size=10&sort=name&direction=ASC&type=INCOME")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("SALARY"))
                    .andExpect(jsonPath("$.content[0].type").value("INCOME"));
        }

        @Test
        void givenNoTypeFilter_thenReturnsAllTypes() throws Exception {
            categoryRepository.save(
                    Category.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .active(true)
                            .system(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );
            categoryRepository.save(
                    Category.builder()
                            .name("SALARY")
                            .type(TransactionType.INCOME)
                            .userId(userId)
                            .active(true)
                            .system(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );

            mockMvc.perform(get("/api/categories?page=0&size=10&sort=name&direction=ASC")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/categories?page=0&size=2&sort=name&direction=ASC"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenNoCategories_thenReturnsEmptyPage() throws Exception {
            mockMvc.perform(get("/api/categories?page=0&size=10&sort=name&direction=ASC&type=EXPENSE")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0));
        }

        @Test
        void givenCategoriesOwnedByOtherUser_thenExcludesOtherUsersCategories() throws Exception {
            var myCategory = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .build()
            );

            UUID otherUserId = UUID.randomUUID();
            categoryRepository.save(
                    Category.builder()
                            .name("RENT")
                            .type(TransactionType.EXPENSE)
                            .userId(otherUserId)
                            .active(true)
                            .system(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );

            mockMvc.perform(get("/api/categories?page=0&size=10&sort=name&direction=ASC&type=EXPENSE")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(myCategory.id().toString()))
                    .andExpect(jsonPath("$.content[0].name").value("FOOD"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void givenSystemCategoryWithUserTransactionsAndOtherUserTransactions_thenExcludesOtherUsersTransactionsInAggregate() throws Exception {
            UUID userId = UUID.randomUUID();

            var systemCategory = categoryRepository.save(
                    Category.builder()
                            .name("Food")
                            .type(TransactionType.EXPENSE)
                            .userId(null)
                            .active(true)
                            .system(true)
                            .icon("food")
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );

            // Create an account for the current user (systemUserId)
            UUID myAccount = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("Cash")
                            .type(AccountType.CASH)
                            .userId(userId)
                            .currency(Currency.getInstance("PHP"))
                            .build()
            ).id();

            // Create a transaction for the current user linked to the system category (via service)
           transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(Amount.of(50.0, "PHP"))
                            .transactionDate(LocalDate.now())
                            .categoryId(systemCategory.id())
                            .accountId(myAccount)
                            .userId(userId)
                            .build()
            );

            // Create an account for another user
            UUID otherUserId = UUID.randomUUID();
            final Account othersAccount = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("OTHER_ACCOUNT")
                            .type(AccountType.CASH)
                            .userId(otherUserId)
                            .currency(Currency.getInstance("USD"))
                            .build()
            );
            transactionService.addTransaction(AddTransactionCommand.builder()
                            .accountId(othersAccount.id())
                            .categoryId(systemCategory.id())
                            .amount(Amount.of(100.0, "USD"))
                            .transactionDate(LocalDate.now())
                            .userId(otherUserId)
                    .build());

            // Query category summaries as the current user (systemUserId)
            mockMvc.perform(get("/api/categories?page=0&size=10&sort=name&direction=ASC&type=EXPENSE")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(systemCategory.id().toString()))
                    .andExpect(jsonPath("$.content[0].name").value("Food"))
                    // Only the current user's transaction should be included in the aggregate
                    .andExpect(jsonPath("$.content[0].totalAmount").value(50.0))
                    .andExpect(jsonPath("$.content[0].totalTransactions").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void givenTransactionsFromLastMonthAndCurrentMonth_thenOnlyCurrentMonthTransactionsReflectOnCategoryAggregation() throws Exception {
            var savedCategory = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .build()
            );

            Account account = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("CASH")
                            .type(AccountType.CASH)
                            .userId(userId)
                            .currency(Currency.getInstance("USD"))
                            .build()
            );

            // Create 2 transactions from last month — these should NOT appear in the aggregate
            LocalDate lastMonthDate = LocalDate.now().minusMonths(1).withDayOfMonth(15);
            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(Amount.of(200.0, "USD"))
                            .transactionDate(lastMonthDate)
                            .categoryId(savedCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );
            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(Amount.of(100.0, "USD"))
                            .transactionDate(lastMonthDate)
                            .categoryId(savedCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            // Create 1 transaction from the current month — this SHOULD appear in the aggregate
            LocalDate currentMonthDate = LocalDate.now().withDayOfMonth(10);
            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(Amount.of(50.0, "USD"))
                            .transactionDate(currentMonthDate)
                            .categoryId(savedCategory.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            mockMvc.perform(get("/api/categories?page=0&size=10&sort=name&direction=ASC&type=EXPENSE")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(savedCategory.id().toString()))
                    .andExpect(jsonPath("$.content[0].name").value("FOOD"))
                    .andExpect(jsonPath("$.content[0].type").value("EXPENSE"))
                    // Only the current month transaction should be aggregated
                    .andExpect(jsonPath("$.content[0].totalAmount").value(50.0))
                    .andExpect(jsonPath("$.content[0].totalTransactions").value(1))
                    .andExpect(jsonPath("$.content[0].percentage").value(100.0))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    class PatchCategory {

        @Test
        void givenValidPatchRequest_thenReturnsUpdatedCategory() throws Exception {
            Category category = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .build()
            );

            PatchCategoryRequest request = PatchCategoryRequest.builder()
                    .name("GROCERIES")
                    .build();

            mockMvc.perform(patch("/api/categories/" + category.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(category.id().toString()))
                    .andExpect(jsonPath("$.name").value("GROCERIES"))
                    .andExpect(jsonPath("$.type").value("EXPENSE"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.system").value(false));
        }

        @Test
        void givenValidPatchRequestWithIcon_thenReturnsUpdatedWithIcon() throws Exception {
            Category category = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .build()
            );

            PatchCategoryRequest request = PatchCategoryRequest.builder()
                    .name("GROCERIES")
                    .icon("groceries")
                    .build();

            mockMvc.perform(patch("/api/categories/" + category.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(category.id().toString()))
                    .andExpect(jsonPath("$.name").value("GROCERIES"))
                    .andExpect(jsonPath("$.icon").value("groceries"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.system").value(false));
        }

        private static Stream<Arguments> invalidPatchCategoryRequestTestCases() {
            return Stream.of(
                    Arguments.of("12345".repeat(100), null, null),
                    Arguments.of(null, null, "12345".repeat(100)),
                    Arguments.of("", null, "12345".repeat(100))
            );
        }

        @ParameterizedTest
        @MethodSource("invalidPatchCategoryRequestTestCases")
        void givenInvalidPatchCategoryRequest_thenReturnsBadRequest(String name, TransactionType type, String icon) throws Exception {
            Category category = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .build()
            );

            PatchCategoryRequest request = PatchCategoryRequest.builder()
                    .name(name)
                    .type(type)
                    .icon(icon)
                    .build();

            mockMvc.perform(patch("/api/categories/" + category.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            UUID id = UUID.randomUUID();

            PatchCategoryRequest request = PatchCategoryRequest.builder()
                    .name("GROCERIES")
                    .build();

            mockMvc.perform(patch("/api/categories/" + id)
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenCategoryNotFound_thenReturnNotFound() throws Exception {
            UUID id = UUID.randomUUID();

            PatchCategoryRequest request = PatchCategoryRequest.builder()
                    .name("GROCERIES")
                    .build();

            mockMvc.perform(patch("/api/categories/" + id)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenPartialPatch_thenOnlyUpdatesProvidedFields() throws Exception {
            Category category = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .icon("food")
                            .userId(userId)
                            .build()
            );

            PatchCategoryRequest request = PatchCategoryRequest.builder()
                    .icon("new_icon")
                    .build();

            mockMvc.perform(patch("/api/categories/" + category.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("FOOD"))
                    .andExpect(jsonPath("$.icon").value("new_icon"))
                    .andExpect(jsonPath("$.type").value("EXPENSE"));
        }
    }

    @Nested
    class DisableCategory {

        @Test
        void givenCategoryWithoutTransactions_thenHardDeletes() throws Exception {
            Category category = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .build()
            );

            mockMvc.perform(post("/api/categories/" + category.id() + "/disable")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/categories/" + category.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenCategoryWithTransactions_thenSoftDisables() throws Exception {
            Category category = categoryService.createCategory(
                    CreateCategoryCommand.builder()
                            .name("FOOD")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .build()
            );

            Account account = accountService.createAccount(
                    CreateAccountCommand.builder()
                            .name("CASH")
                            .type(AccountType.CASH)
                            .userId(userId)
                            .currency(Currency.getInstance("USD"))
                            .build()
            );

            // Create a transaction linked to this category
            transactionService.addTransaction(
                    AddTransactionCommand.builder()
                            .amount(Amount.of(100.0, "USD"))
                            .transactionDate(LocalDate.now())
                            .categoryId(category.id())
                            .accountId(account.id())
                            .userId(userId)
                            .build()
            );

            mockMvc.perform(post("/api/categories/" + category.id() + "/disable")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNoContent());

            // Verify it's actually disabled (soft delete)
            mockMvc.perform(get("/api/categories/" + category.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(post("/api/categories/" + id + "/disable"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenCategoryNotFound_thenReturnNotFound() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(post("/api/categories/" + id + "/disable")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNotFound());
        }
    }
}
