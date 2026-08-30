package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.web.controllers.dtos.CreateCategoryRequest;
import com.fabiankevin.app.web.controllers.dtos.PatchCategoryRequest;
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
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerSpringBootTest {
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
    private CategoryService categoryService;

    @Autowired
    private JsonMapper jsonMapper;

    private UUID userId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
    }

    @Nested
    class CreateCategory {

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
    class DeleteCategoryById {

        @Test
        void givenExistingId_thenReturnsNoContent() throws Exception {
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

            mockMvc.perform(delete("/api/categories/" + category.id())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNoContent());

            // Verify it's actually deleted
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
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/categories/" + id))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenCategoryNotFound_thenReturnNotFound() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/categories/" + id)
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
        void givenMultipleCategories_thenReturnsPagedResponse() throws Exception {
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
                            .name("RENT")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
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
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].name").value("FOOD"))
                    .andExpect(jsonPath("$.content[1].name").value("RENT"))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10));
        }

        @Test
        void givenMultipleCategoriesWithIcons_thenReturnsPagedResponseWithIcons() throws Exception {
            categoryRepository.save(
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
            categoryRepository.save(
                    Category.builder()
                            .name("RENT")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .icon("house")
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
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].name").value("FOOD"))
                    .andExpect(jsonPath("$.content[0].icon").value("food"))
                    .andExpect(jsonPath("$.content[1].name").value("RENT"))
                    .andExpect(jsonPath("$.content[1].icon").value("house"));
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
        void givenTypeFilterIncomeWithIcon_thenReturnsFilteredResponseWithIcon() throws Exception {
            categoryRepository.save(
                    Category.builder()
                            .name("SALARY")
                            .type(TransactionType.INCOME)
                            .userId(userId)
                            .icon("money")
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
                    .andExpect(jsonPath("$.content[0].icon").value("money"));
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
    }

    @Nested
    class PatchCategory {

        @Test
        void givenValidPatchRequest_thenReturnsUpdatedCategory() throws Exception {
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
        void givenValidCategoryId_thenReturnsNoContent() throws Exception {
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

            mockMvc.perform(post("/api/categories/" + category.id() + "/disable")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isNoContent());

            // Verify it's actually disabled
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

    @Nested
    class GetCategorySummaries {

        @Test
        void givenCategoriesWithTransactions_thenReturnsPagedSummaryResponse() throws Exception {
            categoryRepository.save(
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
            categoryRepository.save(
                    Category.builder()
                            .name("RENT")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .icon("house")
                            .active(true)
                            .system(false)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );

            mockMvc.perform(get("/api/categories/summaries?page=0&size=10&sort=name&direction=ASC&type=EXPENSE")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void givenNoJwt_thenReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/categories/summaries?page=0&size=10&sort=name&direction=ASC"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenNoCategories_thenReturnsEmptyPage() throws Exception {
            mockMvc.perform(get("/api/categories/summaries?page=0&size=10&sort=name&direction=ASC&type=EXPENSE")
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
    }
}
