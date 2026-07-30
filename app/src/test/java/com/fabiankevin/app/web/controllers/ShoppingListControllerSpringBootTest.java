package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ItemPriority;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import com.fabiankevin.app.models.shopping_list.ShoppingList;
import com.fabiankevin.app.persistence.ShoppingListRepository;
import com.fabiankevin.app.web.controllers.dtos.shopping_list.CreateShoppingItemRequest;
import com.fabiankevin.app.web.controllers.dtos.shopping_list.CreateShoppingListRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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
import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShoppingListControllerSpringBootTest {

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
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @Nested
    class Create {

        @Test
        void givenValidRequest_thenReturnsCreatedWithSummary() throws Exception {
            UUID userId = UUID.randomUUID();

            CreateShoppingListRequest request = CreateShoppingListRequest.builder()
                    .name("Groceries")
                    .description("Weekly groceries")
                    .budget(200.0)
                    .build();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(post("/api/shopping-lists")
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
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").value("Groceries"))
                    .andExpect(jsonPath("$.description").value("Weekly groceries"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.budget").value(200.0))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.initial").value("JD"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        void givenNoJwt_thenReturnsForbidden() throws Exception {
            CreateShoppingListRequest request = CreateShoppingListRequest.builder()
                    .name("Groceries")
                    .description("Weekly groceries")
                    .budget(200.0)
                    .build();

            mockMvc.perform(post("/api/shopping-lists")
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @ParameterizedTest
        @NullAndEmptySource
        void givenBlankName_thenReturnsBadRequest(String name) throws Exception {
            CreateShoppingListRequest request = CreateShoppingListRequest.builder()
                    .name(name)
                    .description("Weekly groceries")
                    .budget(200.0)
                    .build();

            mockMvc.perform(post("/api/shopping-lists")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", UUID.randomUUID())
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("oversizedFieldRequests")
        void givenFieldExceedsMaxLength_thenReturnsBadRequest(String label, CreateShoppingListRequest request) throws Exception {
            mockMvc.perform(post("/api/shopping-lists")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", UUID.randomUUID())
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        static Stream<Arguments> oversizedFieldRequests() {
            return Stream.of(
                    Arguments.of("name exceeds 64 chars",
                            CreateShoppingListRequest.builder()
                                    .name("a".repeat(65))
                                    .description("Weekly groceries")
                                    .budget(200.0)
                                    .build()),
                    Arguments.of("description exceeds 128 chars",
                            CreateShoppingListRequest.builder()
                                    .name("Groceries")
                                    .description("a".repeat(129))
                                    .budget(200.0)
                                    .build())
            );
        }
    }

    @Nested
    class AddItem {

        @Test
        void givenExistingList_thenReturnsCreatedWithItem() throws Exception {
            UUID userId = UUID.randomUUID();

            // provision a list first by persisting it through the repository; capture the generated id
            var shoppingList = ShoppingList.builder()
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            UUID shoppingListId = shoppingListRepository.save(shoppingList).id();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            CreateShoppingItemRequest request = CreateShoppingItemRequest.builder()
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .notes("Whole milk")
                    .priority(ItemPriority.HIGH)
                    .build();

            mockMvc.perform(post("/api/shopping-lists/{id}/items", shoppingListId)
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
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").value("Milk"))
                    .andExpect(jsonPath("$.category").value("Dairy"))
                    .andExpect(jsonPath("$.quantity").value(2.0))
                    .andExpect(jsonPath("$.unit").value("liters"))
                    .andExpect(jsonPath("$.price").value(3.5))
                    .andExpect(jsonPath("$.purchased").value(false))
                    .andExpect(jsonPath("$.priority").value("HIGH"))
                    .andExpect(jsonPath("$.notes").value("Whole milk"))
                    .andExpect(jsonPath("$.addedByFirstName").value("John"))
                    .andExpect(jsonPath("$.addedByLastName").value("Doe"))
                    .andExpect(jsonPath("$.addedByInitial").value("JD"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        void givenNonExistentList_thenReturnsNotFound() throws Exception {
            UUID userId = UUID.randomUUID();

            CreateShoppingItemRequest request = CreateShoppingItemRequest.builder()
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .priority(ItemPriority.HIGH)
                    .build();

            mockMvc.perform(post("/api/shopping-lists/{id}/items", UUID.randomUUID())
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

        @ParameterizedTest
        @NullAndEmptySource
        void givenBlankName_thenReturnsBadRequest(String name) throws Exception {
            CreateShoppingItemRequest request = CreateShoppingItemRequest.builder()
                    .name(name)
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .priority(ItemPriority.HIGH)
                    .build();

            mockMvc.perform(post("/api/shopping-lists/{id}/items", UUID.randomUUID())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", UUID.randomUUID())
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @NullAndEmptySource
        void givenBlankCategory_thenReturnsBadRequest(String category) throws Exception {
            CreateShoppingItemRequest request = CreateShoppingItemRequest.builder()
                    .name("Milk")
                    .quantity(2.0)
                    .unit("liters")
                    .category(category)
                    .price(3.5)
                    .priority(ItemPriority.HIGH)
                    .build();

            mockMvc.perform(post("/api/shopping-lists/{id}/items", UUID.randomUUID())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", UUID.randomUUID())
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("oversizedFieldRequests")
        void givenFieldExceedsMaxLength_thenReturnsBadRequest(String label, CreateShoppingItemRequest request) throws Exception {
            mockMvc.perform(post("/api/shopping-lists/{id}/items", UUID.randomUUID())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", UUID.randomUUID())
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        static Stream<Arguments> oversizedFieldRequests() {
            return Stream.of(
                    Arguments.of("name exceeds 128 chars",
                            CreateShoppingItemRequest.builder()
                                    .name("a".repeat(129))
                                    .category("Dairy")
                                    .quantity(2.0)
                                    .unit("liters")
                                    .price(3.5)
                                    .priority(ItemPriority.HIGH)
                                    .build()),
                    Arguments.of("category exceeds 128 chars",
                            CreateShoppingItemRequest.builder()
                                    .name("Milk")
                                    .category("a".repeat(129))
                                    .quantity(2.0)
                                    .unit("liters")
                                    .price(3.5)
                                    .priority(ItemPriority.HIGH)
                                    .build()),
                    Arguments.of("notes exceeds 128 chars",
                            CreateShoppingItemRequest.builder()
                                    .name("Milk")
                                    .category("Dairy")
                                    .quantity(2.0)
                                    .unit("liters")
                                    .price(3.5)
                                    .notes("a".repeat(129))
                                    .priority(ItemPriority.HIGH)
                                    .build()),
                    Arguments.of("unit exceeds 36 chars",
                            CreateShoppingItemRequest.builder()
                                    .name("Milk")
                                    .category("Dairy")
                                    .quantity(2.0)
                                    .unit("liters".repeat(40))
                                    .price(3.5)
                                    .priority(ItemPriority.HIGH)
                                    .build())
            );
        }
    }
}
