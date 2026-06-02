package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.commands.PatchCategoryCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.web.controllers.dtos.CreateCategoryRequest;
import com.fabiankevin.app.web.controllers.dtos.CreateIconRequest;
import com.fabiankevin.app.web.controllers.dtos.IconResponse;
import com.fabiankevin.app.web.controllers.dtos.PatchCategoryRequest;
import com.github.fabiankevin.lemon.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
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

@WebMvcTest(CategoryController.class)
@Import({GlobalExceptionHandler.class})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Autowired
    private  JsonMapper jsonMapper;

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
    void createCategory_givenValidRequest_thenShouldCreateCategory() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .build();

        when(categoryService.createCategory(any())).thenAnswer(invocation -> {
            UUID id = UUID.randomUUID();
            com.fabiankevin.app.services.commands.CreateCategoryCommand command = invocation.getArgument(0);
            UUID userId = command.userId() != null ? command.userId() : UUID.randomUUID();
            return Category.builder()
                    .id(id)
                    .name(command.name())
                    .type(command.type() != null ? command.type() : TransactionType.EXPENSE)
                    .userId(userId)
                    .icon(command.icon())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

        });

        mockMvc.perform(post("/api/categories")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/api/categories/[-a-f0-9]{36}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("FOOD"))
                .andExpect(jsonPath("$.icon").doesNotExist());

        verify(categoryService, times(1)).createCategory(any());
    }

    @Test
    void createCategory_givenValidRequestWithIcon_thenShouldCreateCategoryWithIcon() throws Exception {
        CreateIconRequest iconRequest = CreateIconRequest.builder()
                .codePoint(128161)
                .fontFamily("Material Icons")
                .build();

        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .icon(iconRequest)
                .build();

        when(categoryService.createCategory(any())).thenAnswer(invocation -> {
            UUID id = UUID.randomUUID();
            com.fabiankevin.app.services.commands.CreateCategoryCommand command = invocation.getArgument(0);
            UUID userId = command.userId() != null ? command.userId() : UUID.randomUUID();
            return Category.builder()
                    .id(id)
                    .name(command.name())
                    .type(command.type() != null ? command.type() : TransactionType.EXPENSE)
                    .userId(userId)
                    .icon(command.icon())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });

        mockMvc.perform(post("/api/categories")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/api/categories/[-a-f0-9]{36}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("FOOD"))
                .andExpect(jsonPath("$.icon.codePoint").value(128161))
                .andExpect(jsonPath("$.icon.fontFamily").value("Material Icons"));

        verify(categoryService, times(1)).createCategory(any());
    }

    @Test
    void createCategory_givenNoJwt_thenShouldReturnForbidden() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("FOOD")
                .build();

        mockMvc.perform(post("/api/categories")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    void getCategoryById_givenExistingId_thenShouldReturnCategory() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.fromString(jwt.getSubject());

        when(categoryService.getCategoryById(id, userId)).thenReturn(Category.builder()
                .id(id)
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/categories/" + id)
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("FOOD"))
                .andExpect(jsonPath("$.icon").doesNotExist());

        verify(categoryService, times(1)).getCategoryById(id, userId);
    }

    @Test
    void getCategoryById_givenExistingIdWithIcon_thenShouldReturnCategoryWithIcon() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID iconId = UUID.randomUUID();

        when(categoryService.getCategoryById(id, userId)).thenReturn(Category.builder()
                .id(id)
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(com.fabiankevin.app.models.IconData.builder()
                        .id(iconId)
                        .codePoint(128161)
                        .fontFamily("Material Icons")
                        .build())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/categories/" + id)
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("FOOD"))
                .andExpect(jsonPath("$.icon.id").value(iconId.toString()))
                .andExpect(jsonPath("$.icon.codePoint").value(128161))
                .andExpect(jsonPath("$.icon.fontFamily").value("Material Icons"));

        verify(categoryService, times(1)).getCategoryById(id, userId);
    }

    @Test
    void getCategoryById_givenNoJwt_thenShouldReturnUnauthorized() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/categories/" + id))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(categoryService);
    }

    @Test
    void deleteCategoryById_givenExistingId_thenShouldReturnNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.fromString(jwt.getSubject());

        mockMvc.perform(delete("/api/categories/" + id)
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isNoContent());

        verify(categoryService, times(1)).deleteCategoryById(id, userId);
    }

    @Test
    void deleteCategoryById_givenNoJwt_thenShouldReturnForbidden() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/categories/" + id))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    void getCategoriesByPageQuery_givenValidParams_thenShouldReturnPagedResponse() throws Exception {
        UUID userId = UUID.fromString(jwt.getSubject());
        PageQuery query = new PageQuery(0, 2, "name", "ASC");
        TransactionType type = TransactionType.EXPENSE;

        Category c1 = Category.builder()
                .id(UUID.randomUUID())
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Category c2 = Category.builder()
                .id(UUID.randomUUID())
                .name("RENT")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryService.getCategoriesByPageQuery(query, userId, type))
                .thenReturn(new Page<>(List.of(c1, c2), 0, 2, 2L, 1, true, true));

        mockMvc.perform(get("/api/categories?page=0&size=2&sort=name&direction=ASC&type=EXPENSE")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(categoryService, times(1)).getCategoriesByPageQuery(query, userId, type);
    }

    @Test
    void getCategoriesByPageQuery_givenValidParamsWithIcons_thenShouldReturnPagedResponseWithIcons() throws Exception {
        UUID userId = UUID.fromString(jwt.getSubject());
        PageQuery query = new PageQuery(0, 2, "name", "ASC");
        TransactionType type = TransactionType.EXPENSE;
        UUID iconId1 = UUID.randomUUID();
        UUID iconId2 = UUID.randomUUID();

        Category c1 = Category.builder()
                .id(UUID.randomUUID())
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(com.fabiankevin.app.models.IconData.builder()
                        .id(iconId1)
                        .codePoint(128161)
                        .fontFamily("Material Icons")
                        .build())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Category c2 = Category.builder()
                .id(UUID.randomUUID())
                .name("RENT")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(com.fabiankevin.app.models.IconData.builder()
                        .id(iconId2)
                        .codePoint(128175)
                        .fontFamily("Material Icons")
                        .build())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryService.getCategoriesByPageQuery(query, userId, type))
                .thenReturn(new Page<>(List.of(c1, c2), 0, 2, 2L, 1, true, true));

        mockMvc.perform(get("/api/categories?page=0&size=2&sort=name&direction=ASC&type=EXPENSE")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].icon.codePoint").value(128161))
                .andExpect(jsonPath("$.content[1].icon.codePoint").value(128175))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(categoryService, times(1)).getCategoriesByPageQuery(query, userId, type);
    }

    @Test
    void getCategoriesByPageQuery_givenTypeFilterINCOME_shouldReturnFilteredResponse() throws Exception {
        UUID userId = UUID.fromString(jwt.getSubject());
        PageQuery query = new PageQuery(0, 2, "name", "ASC");
        TransactionType type = TransactionType.INCOME;

        Category c1 = Category.builder()
                .id(UUID.randomUUID())
                .name("SALARY")
                .type(TransactionType.INCOME)
                .userId(userId)
                .icon(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryService.getCategoriesByPageQuery(query, userId, type))
                .thenReturn(new Page<>(List.of(c1), 0, 2, 1L, 1, true, true));

        mockMvc.perform(get("/api/categories?page=0&size=2&sort=name&direction=ASC&type=INCOME")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("SALARY"))
                .andExpect(jsonPath("$.content[0].icon").doesNotExist());

        verify(categoryService, times(1)).getCategoriesByPageQuery(query, userId, type);
    }

    @Test
    void getCategoriesByPageQuery_givenTypeFilterINCOMEWithIcon_shouldReturnFilteredResponseWithIcon() throws Exception {
        UUID userId = UUID.fromString(jwt.getSubject());
        PageQuery query = new PageQuery(0, 2, "name", "ASC");
        TransactionType type = TransactionType.INCOME;
        UUID iconId = UUID.randomUUID();

        Category c1 = Category.builder()
                .id(UUID.randomUUID())
                .name("SALARY")
                .type(TransactionType.INCOME)
                .userId(userId)
                .icon(com.fabiankevin.app.models.IconData.builder()
                        .id(iconId)
                        .codePoint(128104)
                        .fontFamily("Material Icons")
                        .build())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryService.getCategoriesByPageQuery(query, userId, type))
                .thenReturn(new Page<>(List.of(c1), 0, 2, 1L, 1, true, true));

        mockMvc.perform(get("/api/categories?page=0&size=2&sort=name&direction=ASC&type=INCOME")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("SALARY"))
                .andExpect(jsonPath("$.content[0].icon.codePoint").value(128104))
                .andExpect(jsonPath("$.content[0].icon.fontFamily").value("Material Icons"));

        verify(categoryService, times(1)).getCategoriesByPageQuery(query, userId, type);
    }

    @Test
    void getCategoriesByPageQuery_givenNoTypeFilter_shouldDefaultToExpenseType() throws Exception {
        UUID userId = UUID.fromString(jwt.getSubject());
        PageQuery query = new PageQuery(0, 2, "name", "ASC");
        TransactionType type = null;

        Category c1 = Category.builder()
                .id(UUID.randomUUID())
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Category c2 = Category.builder()
                .id(UUID.randomUUID())
                .name("SALARY")
                .type(TransactionType.INCOME)
                .userId(userId)
                .icon(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryService.getCategoriesByPageQuery(query, userId, null))
                .thenReturn(new Page<>(List.of(c1, c2), 0, 2, 2L, 1, true, true));

        mockMvc.perform(get("/api/categories?page=0&size=2&sort=name&direction=ASC")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].icon").doesNotExist())
                .andExpect(jsonPath("$.content[1].icon").doesNotExist());

        verify(categoryService, times(1)).getCategoriesByPageQuery(query, userId, null);
    }

    @Test
    void getCategoriesByPageQuery_givenNoTypeFilterWithIcons_shouldDefaultToExpenseType() throws Exception {
        UUID userId = UUID.fromString(jwt.getSubject());
        PageQuery query = new PageQuery(0, 2, "name", "ASC");
        TransactionType type = null;
        UUID iconId1 = UUID.randomUUID();
        UUID iconId2 = UUID.randomUUID();

        Category c1 = Category.builder()
                .id(UUID.randomUUID())
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon(com.fabiankevin.app.models.IconData.builder()
                        .id(iconId1)
                        .codePoint(128161)
                        .fontFamily("Material Icons")
                        .build())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Category c2 = Category.builder()
                .id(UUID.randomUUID())
                .name("SALARY")
                .type(TransactionType.INCOME)
                .userId(userId)
                .icon(com.fabiankevin.app.models.IconData.builder()
                        .id(iconId2)
                        .codePoint(128104)
                        .fontFamily("Material Icons")
                        .build())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryService.getCategoriesByPageQuery(query, userId, null))
                .thenReturn(new Page<>(List.of(c1, c2), 0, 2, 2L, 1, true, true));

        mockMvc.perform(get("/api/categories?page=0&size=2&sort=name&direction=ASC")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].icon.codePoint").value(128161))
                .andExpect(jsonPath("$.content[1].icon.codePoint").value(128104));

        verify(categoryService, times(1)).getCategoriesByPageQuery(query, userId, null);
    }

    @Test
    void getCategoriesByPageQuery_givenNoJwt_thenShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/categories?page=0&size=2&sort=name&direction=ASC"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(categoryService);
    }

    @Test
    void getCategoriesByPageQuery_givenNoContent_thenShouldReturnEmptyPage() throws Exception {
        UUID userId = UUID.fromString(jwt.getSubject());

        when(categoryService.getCategoriesByPageQuery(any(PageQuery.class), eq(userId), any()))
                .thenReturn(new Page<>(List.of(), 0, 10, 0L, 0, false, true));

        mockMvc.perform(get("/api/categories?page=0&size=10&sort=name&direction=ASC")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        verify(categoryService, times(1)).getCategoriesByPageQuery(argThat(
                pageQuery -> pageQuery.page() == 0
                        && pageQuery.size() == 10
                        && pageQuery.sort().equals("name")
                        && pageQuery.direction().equals("ASC")
        ), eq(userId), any());
    }

    @Test
    void patchCategory_givenValidRequest_thenShouldReturnUpdated() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.fromString(jwt.getSubject());

        PatchCategoryRequest request = PatchCategoryRequest.builder()
                .name("GROCERIES")
                .build();

        when(categoryService.patchCategory(any())).thenAnswer(invocation -> {
            PatchCategoryCommand cmd = invocation.getArgument(0);
            return Category.builder()
                    .id(cmd.id())
                    .name(cmd.name())
                    .type(cmd.type() != null ? cmd.type() : TransactionType.EXPENSE)
                    .userId(userId)
                    .icon(cmd.icon())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });

        mockMvc.perform(patch("/api/categories/" + id)
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("GROCERIES"))
                .andExpect(jsonPath("$.icon").doesNotExist());

        verify(categoryService, times(1)).patchCategory(any());
    }

    @Test
    void patchCategory_givenValidRequestWithIcon_thenShouldReturnUpdatedWithIcon() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID iconId = UUID.randomUUID();

        IconResponse iconResponse = IconResponse.builder()
                .id(iconId)
                .codePoint(128161)
                .fontFamily("Material Icons")
                .build();

        PatchCategoryRequest request = PatchCategoryRequest.builder()
                .name("GROCERIES")
                .icon(iconResponse)
                .build();

        when(categoryService.patchCategory(any())).thenAnswer(invocation -> {
            com.fabiankevin.app.services.commands.PatchCategoryCommand cmd = invocation.getArgument(0);
            return Category.builder()
                    .id(cmd.id())
                    .name(cmd.name())
                    .type(cmd.type() != null ? cmd.type() : TransactionType.EXPENSE)
                    .userId(userId)
                    .icon(cmd.icon())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });

        mockMvc.perform(patch("/api/categories/" + id)
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("GROCERIES"))
                .andExpect(jsonPath("$.icon.id").value(iconId.toString()))
                .andExpect(jsonPath("$.icon.codePoint").value(128161))
                .andExpect(jsonPath("$.icon.fontFamily").value("Material Icons"));

        verify(categoryService, times(1)).patchCategory(any());
    }

    @Test
    void patchCategory_givenNoJwt_thenShouldReturnForbidden() throws Exception {
        UUID id = UUID.randomUUID();

        PatchCategoryRequest request = PatchCategoryRequest.builder()
                .name("GROCERIES")
                .build();

        mockMvc.perform(patch("/api/categories/" + id)
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    void disableCategory_givenValidCategoryId_shouldDelegateCategoryIdAndUserIdAndReturnOk() throws Exception {
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.fromString(jwt.getSubject());

        mockMvc.perform(patch("/api/categories/" + categoryId + "/disable")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).disableCategory(categoryId, userId);
    }

    @Test
    void disableCategory_givenInvalidCategoryId_shouldThrowCategoryNotFoundException() throws Exception {
        UUID categoryId = UUID.randomUUID();

        doThrow(new com.fabiankevin.app.exceptions.CategoryNotFoundException())
                .when(categoryService).disableCategory(any(), any());

        mockMvc.perform(patch("/api/categories/" + categoryId + "/disable")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isNotFound());

        verify(categoryService, times(1)).disableCategory(eq(categoryId), any());
    }
}
