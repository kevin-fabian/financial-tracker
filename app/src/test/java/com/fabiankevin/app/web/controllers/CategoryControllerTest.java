package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.web.controllers.dtos.CreateCategoryRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Jwt jwt;

    @BeforeEach
    void setup(){
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
                .build();

        when(categoryService.createCategory(any())).thenAnswer(invocation -> {
            UUID id = UUID.randomUUID();
            com.fabiankevin.app.services.commands.CreateCategoryCommand command = invocation.getArgument(0);
            UUID userId = command.userId() != null ? command.userId() : UUID.randomUUID();
            return Category.builder()
                    .id(id)
                    .name(command.name())
                    .userId(userId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

        });

        mockMvc.perform(post("/api/categories")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/api/categories/[-a-f0-9]{36}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("FOOD"));

        verify(categoryService, times(1)).createCategory(any());
    }

    @Test
    void getCategoryById_givenExistingId_thenShouldReturnCategory() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.fromString(jwt.getSubject());

        when(categoryService.getCategoryById(id, userId)).thenReturn(Category.builder()
                .id(id)
                .name("FOOD")
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/categories/" + id)
                .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("FOOD"));

        verify(categoryService, times(1)).getCategoryById(id, userId);
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
    void getCategoriesByPageQuery_givenValidParams_thenShouldReturnPagedResponse() throws Exception {
        UUID userId = UUID.fromString(jwt.getSubject());
        PageQuery query = new PageQuery(0, 2, "name", "ASC");

        Category c1 = Category.builder()
                .id(UUID.randomUUID())
                .name("FOOD")
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Category c2 = Category.builder()
                .id(UUID.randomUUID())
                .name("RENT")
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(categoryService.getCategoriesByPageQuery(query, userId))
                .thenReturn(new Page<>(List.of(c1, c2), 0, 2, 2L, 1, true, true));

        mockMvc.perform(get("/api/categories?page=0&size=2&sort=name&direction=ASC")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(categoryService, times(1)).getCategoriesByPageQuery(query, userId);
    }
}
