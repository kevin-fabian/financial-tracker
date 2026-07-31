package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ItemPriority;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import com.fabiankevin.app.models.shopping_list.ShoppingList;
import com.fabiankevin.app.persistence.ShoppingListRepository;
import com.fabiankevin.app.web.controllers.dtos.shopping_list.CreateShoppingItemRequest;
import com.fabiankevin.app.web.controllers.dtos.shopping_list.CreateShoppingListRequest;
import com.fabiankevin.app.web.controllers.dtos.shopping_list.PatchShoppingItemRequest;
import com.fabiankevin.app.web.controllers.dtos.shopping_list.PatchShoppingListRequest;
import org.assertj.core.api.Assertions;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    class CreateShoppingList {

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
        void givenRequestWithSharedWithUserIds_thenSavesSharedWithUserIdsButDoesNotReturnThem() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID sharedUser1 = UUID.randomUUID();
            UUID sharedUser2 = UUID.randomUUID();

            CreateShoppingListRequest request = CreateShoppingListRequest.builder()
                    .name("Shared Groceries")
                    .description("Shared weekly groceries")
                    .budget(150.0)
                    .sharedWithUserIds(List.of(sharedUser1, sharedUser2))
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
                    .andExpect(jsonPath("$.name").value("Shared Groceries"))
                    .andExpect(jsonPath("$.sharedWithUserIds").doesNotExist());

            ShoppingList saved = shoppingListRepository.findAllByUserId(userId).getFirst();
            Assertions.assertThat(saved.sharedWithUserIds())
                    .containsExactlyInAnyOrder(sharedUser1, sharedUser2);
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
    class UpdateShoppingList {

        @Test
        void givenExistingList_thenReturnsOkWithUpdatedList() throws Exception {
            UUID userId = UUID.randomUUID();

            ShoppingList shoppingList = ShoppingList.builder()
                    .name("Groceries")
                    .description("Weekly groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .budget(200.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            UUID shoppingListId = shoppingListRepository.save(shoppingList).id();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            PatchShoppingListRequest request = PatchShoppingListRequest.builder()
                    .name("Weekly Groceries")
                    .budget(250.0)
                    .status(ShoppingListStatus.ACTIVE)
                    .build();

            mockMvc.perform(patch("/api/shopping-lists/{id}", shoppingListId)
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
                    .andExpect(jsonPath("$.id").value(shoppingListId.toString()))
                    .andExpect(jsonPath("$.name").value("Weekly Groceries"))
                    .andExpect(jsonPath("$.description").value("Weekly groceries"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.budget").value(250.0))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.initial").value("JD"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        void givenExistingList_whenPatchSharedWithUserIds_thenPersistsAndDoesNotReturnThem() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID sharedUser1 = UUID.randomUUID();
            UUID sharedUser2 = UUID.randomUUID();

            ShoppingList shoppingList = ShoppingList.builder()
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .budget(100.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            UUID shoppingListId = shoppingListRepository.save(shoppingList).id();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            PatchShoppingListRequest request = PatchShoppingListRequest.builder()
                    .sharedWithUserIds(List.of(sharedUser1, sharedUser2))
                    .build();

            mockMvc.perform(patch("/api/shopping-lists/{id}", shoppingListId)
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
                    .andExpect(jsonPath("$.sharedWithUserIds").doesNotExist());

            ShoppingList saved = shoppingListRepository.findById(shoppingListId).get();
            Assertions.assertThat(saved.sharedWithUserIds())
                    .containsExactlyInAnyOrder(sharedUser1, sharedUser2);
        }

        @Test
        void givenNonExistentList_thenReturnsNotFound() throws Exception {
            UUID userId = UUID.randomUUID();

            PatchShoppingListRequest request = PatchShoppingListRequest.builder()
                    .name("Updated")
                    .build();

            mockMvc.perform(patch("/api/shopping-lists/{id}", UUID.randomUUID())
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
        void givenListNotOwnedByUser_thenReturnsNotFound() throws Exception {
            UUID listOwnerId = UUID.randomUUID();
            UUID unauthorizedUserId = UUID.randomUUID();

            ShoppingList shoppingList = ShoppingList.builder()
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(listOwnerId)
                    .budget(100.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            UUID shoppingListId = shoppingListRepository.save(shoppingList).id();

            PatchShoppingListRequest request = PatchShoppingListRequest.builder()
                    .name("Hacked")
                    .build();

            mockMvc.perform(patch("/api/shopping-lists/{id}", shoppingListId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", unauthorizedUserId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("oversizedUpdateListFieldRequests")
        void givenFieldExceedsMaxLength_thenReturnsBadRequest(String label, PatchShoppingListRequest request) throws Exception {
            mockMvc.perform(patch("/api/shopping-lists/{id}", UUID.randomUUID())
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

        static Stream<Arguments> oversizedUpdateListFieldRequests() {
            return Stream.of(
                    Arguments.of("name exceeds 64 chars",
                            PatchShoppingListRequest.builder()
                                    .name("a".repeat(65))
                                    .build()),
                    Arguments.of("description exceeds 128 chars",
                            PatchShoppingListRequest.builder()
                                    .description("a".repeat(129))
                                    .build())
            );
        }
    }

    @Nested
    class GetShoppingLists {

        @Test
        void givenExistingListsWithItems_thenReturnsListOfSummaries() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID addedBy = UUID.randomUUID();

            ShoppingItem milk = ShoppingItem.builder()
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .priority(ItemPriority.HIGH)
                    .notes("Whole milk")
                    .addedBy(addedBy)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            ShoppingList list1 = ShoppingList.builder()
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .description("Weekly groceries")
                    .budget(200.0)
                    .items(new ArrayList<>(List.of(milk)))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            ShoppingList list2 = ShoppingList.builder()
                    .name("Supplies")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .description("Office supplies")
                    .budget(50.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            shoppingListRepository.save(list1);
            shoppingListRepository.save(list2);

            when(userClient.getUsersByIds(List.of(addedBy, userId)))
                    .thenReturn(List.of(
                            User.builder().id(userId).firstName("John").lastName("Doe").build(),
                            User.builder().id(addedBy).firstName("Jane").lastName("Doe").build()));

            mockMvc.perform(get("/api/shopping-lists")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("Groceries"))
                    .andExpect(jsonPath("$[0].description").value("Weekly groceries"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].budget").value(200.0))
                    .andExpect(jsonPath("$[0].firstName").value("John"))
                    .andExpect(jsonPath("$[0].lastName").value("Doe"))
                    .andExpect(jsonPath("$[0].initial").value("JD"))
                    .andExpect(jsonPath("$[0].items").isArray())
                    .andExpect(jsonPath("$[0].items.length()").value(1))
                    .andExpect(jsonPath("$[0].items[0].name").value("Milk"))
                    .andExpect(jsonPath("$[0].items[0].category").value("Dairy"))
                    .andExpect(jsonPath("$[0].items[0].quantity").value(2.0))
                    .andExpect(jsonPath("$[0].items[0].unit").value("liters"))
                    .andExpect(jsonPath("$[0].items[0].price").value(3.5))
                    .andExpect(jsonPath("$[0].items[0].purchased").value(false))
                    .andExpect(jsonPath("$[0].items[0].priority").value("HIGH"))
                    .andExpect(jsonPath("$[0].items[0].notes").value("Whole milk"))
                    .andExpect(jsonPath("$[0].items[0].addedByFirstName").value("Jane"))
                    .andExpect(jsonPath("$[0].items[0].addedByLastName").value("Doe"))
                    .andExpect(jsonPath("$[0].items[0].addedByInitial").value("JD"))
                    .andExpect(jsonPath("$[0].createdAt").exists())
                    .andExpect(jsonPath("$[0].updatedAt").exists())
                    .andExpect(jsonPath("$[1].name").value("Supplies"))
                    .andExpect(jsonPath("$[1].items").isArray())
                    .andExpect(jsonPath("$[1].items.length()").value(0));
        }

        @Test
        void givenListsWithoutItems_thenReturnsEmptyItemsForAll() throws Exception {
            UUID userId = UUID.randomUUID();

            ShoppingList list1 = ShoppingList.builder()
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .description("Weekly groceries")
                    .budget(200.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            ShoppingList list2 = ShoppingList.builder()
                    .name("Supplies")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .description("Office supplies")
                    .budget(50.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            shoppingListRepository.save(list1);
            shoppingListRepository.save(list2);

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(get("/api/shopping-lists")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("Groceries"))
                    .andExpect(jsonPath("$[0].items").isArray())
                    .andExpect(jsonPath("$[0].items.length()").value(0))
                    .andExpect(jsonPath("$[1].name").value("Supplies"))
                    .andExpect(jsonPath("$[1].items").isArray())
                    .andExpect(jsonPath("$[1].items.length()").value(0));
        }

        @Test
        void givenNoLists_thenReturnsEmptyList() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            mockMvc.perform(get("/api/shopping-lists")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    class AddShoppingItem {

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

    @Nested
    class UpdateShoppingItem {

        @Test
        void givenExistingItem_thenReturnsOkWithUpdatedItem() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID addedBy = UUID.randomUUID();

            ShoppingItem milk = ShoppingItem.builder()
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .priority(ItemPriority.HIGH)
                    .notes("Whole milk")
                    .addedBy(addedBy)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            ShoppingList shoppingList = ShoppingList.builder()
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .items(new ArrayList<>(List.of(milk)))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            UUID shoppingListId = shoppingListRepository.save(shoppingList).id();
            UUID itemId = shoppingListRepository.findById(shoppingListId).get().items().getFirst().id();

            when(userClient.getUsersByIds(List.of(addedBy)))
                    .thenReturn(List.of(User.builder().id(addedBy).firstName("Jane").lastName("Doe").build()));

            PatchShoppingItemRequest request = PatchShoppingItemRequest.builder()
                    .price(4.0)
                    .priority(ItemPriority.MEDIUM)
                    .build();

            mockMvc.perform(patch("/api/shopping-lists/{id}/items/{itemId}", shoppingListId, itemId)
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
                    .andExpect(jsonPath("$.id").value(itemId.toString()))
                    .andExpect(jsonPath("$.name").value("Milk"))
                    .andExpect(jsonPath("$.category").value("Dairy"))
                    .andExpect(jsonPath("$.quantity").value(2.0))
                    .andExpect(jsonPath("$.unit").value("liters"))
                    .andExpect(jsonPath("$.price").value(4.0))
                    .andExpect(jsonPath("$.priority").value("MEDIUM"))
                    .andExpect(jsonPath("$.notes").value("Whole milk"))
                    .andExpect(jsonPath("$.addedByFirstName").value("Jane"))
                    .andExpect(jsonPath("$.addedByLastName").value("Doe"))
                    .andExpect(jsonPath("$.addedByInitial").value("JD"))
                    .andExpect(jsonPath("$.purchased").value(false))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        void givenNonExistentList_thenReturnsNotFound() throws Exception {
            UUID userId = UUID.randomUUID();

            PatchShoppingItemRequest request = PatchShoppingItemRequest.builder()
                    .price(4.0)
                    .build();

            mockMvc.perform(patch("/api/shopping-lists/{id}/items/{itemId}", UUID.randomUUID(), UUID.randomUUID())
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
        void givenNonExistentItem_thenReturnsNotFound() throws Exception {
            UUID userId = UUID.randomUUID();

            ShoppingList shoppingList = ShoppingList.builder()
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            UUID shoppingListId = shoppingListRepository.save(shoppingList).id();

            PatchShoppingItemRequest request = PatchShoppingItemRequest.builder()
                    .price(4.0)
                    .build();

            mockMvc.perform(patch("/api/shopping-lists/{id}/items/{itemId}", shoppingListId, UUID.randomUUID())
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
        void givenExistingItem_whenPatchPurchasedTrue_thenReturnsPurchasedTrue() throws Exception {
            UUID userId = UUID.randomUUID();

            ShoppingItem milk = ShoppingItem.builder()
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .addedBy(userId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            ShoppingList shoppingList = ShoppingList.builder()
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .items(new ArrayList<>(List.of(milk)))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            UUID shoppingListId = shoppingListRepository.save(shoppingList).id();
            UUID itemId = shoppingListRepository.findById(shoppingListId).get().items().getFirst().id();

            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder().id(userId).firstName("John").lastName("Doe").build()));

            PatchShoppingItemRequest request = PatchShoppingItemRequest.builder()
                    .purchased(true)
                    .build();

            mockMvc.perform(patch("/api/shopping-lists/{id}/items/{itemId}", shoppingListId, itemId)
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
                    .andExpect(jsonPath("$.id").value(itemId.toString()))
                    .andExpect(jsonPath("$.purchased").value(true))
                    .andExpect(jsonPath("$.name").value("Milk"));
        }

        @Test
        void givenListNotOwnedOrSharedWithUser_thenReturnsNotFound() throws Exception {
            UUID listOwnerId = UUID.randomUUID();
            UUID unauthorizedUserId = UUID.randomUUID();

            ShoppingItem milk = ShoppingItem.builder()
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .priority(ItemPriority.HIGH)
                    .addedBy(listOwnerId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            ShoppingList shoppingList = ShoppingList.builder()
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(listOwnerId)
                    .items(new ArrayList<>(List.of(milk)))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            UUID shoppingListId = shoppingListRepository.save(shoppingList).id();
            UUID itemId = shoppingListRepository.findById(shoppingListId).get().items().getFirst().id();

            PatchShoppingItemRequest request = PatchShoppingItemRequest.builder()
                    .price(4.0)
                    .build();

            mockMvc.perform(patch("/api/shopping-lists/{id}/items/{itemId}", shoppingListId, itemId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", unauthorizedUserId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("oversizedFieldRequests")
        void givenFieldExceedsMaxLength_thenReturnsBadRequest(String label, PatchShoppingItemRequest request) throws Exception {
            mockMvc.perform(patch("/api/shopping-lists/{id}/items/{itemId}", UUID.randomUUID(), UUID.randomUUID())
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
                            PatchShoppingItemRequest.builder()
                                    .name("a".repeat(129))
                                    .build()),
                    Arguments.of("category exceeds 128 chars",
                            PatchShoppingItemRequest.builder()
                                    .category("a".repeat(129))
                                    .build()),
                    Arguments.of("notes exceeds 32 chars",
                            PatchShoppingItemRequest.builder()
                                    .notes("a".repeat(33))
                                    .build()),
                    Arguments.of("unit exceeds 36 chars",
                            PatchShoppingItemRequest.builder()
                                    .unit("liters".repeat(40))
                                    .build())
            );
        }
    }

    @Nested
    class DeleteShoppingItem {

        @Test
        void givenExistingItem_thenReturnsNoContent() throws Exception {
            UUID userId = UUID.randomUUID();

            ShoppingItem milk = ShoppingItem.builder()
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .priority(ItemPriority.HIGH)
                    .addedBy(userId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            ShoppingList shoppingList = ShoppingList.builder()
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .items(new ArrayList<>(List.of(milk)))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            UUID shoppingListId = shoppingListRepository.save(shoppingList).id();
            UUID itemId = shoppingListRepository.findById(shoppingListId).get().items().getFirst().id();

            mockMvc.perform(delete("/api/shopping-lists/{id}/items/{itemId}", shoppingListId, itemId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json"))
                    .andExpect(status().isNoContent());

            Assertions.assertThat(shoppingListRepository.findById(shoppingListId).get().items()).isEmpty();
        }

        @Test
        void givenNonExistentList_thenReturnsNotFound() throws Exception {
            UUID userId = UUID.randomUUID();

            mockMvc.perform(delete("/api/shopping-lists/{id}/items/{itemId}", UUID.randomUUID(), UUID.randomUUID())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenNonExistentItem_thenReturnsNotFound() throws Exception {
            UUID userId = UUID.randomUUID();

            ShoppingList shoppingList = ShoppingList.builder()
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            UUID shoppingListId = shoppingListRepository.save(shoppingList).id();

            mockMvc.perform(delete("/api/shopping-lists/{id}/items/{itemId}", shoppingListId, UUID.randomUUID())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenListNotOwnedOrSharedWithUser_thenReturnsNotFound() throws Exception {
            UUID listOwnerId = UUID.randomUUID();
            UUID unauthorizedUserId = UUID.randomUUID();

            ShoppingItem milk = ShoppingItem.builder()
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .priority(ItemPriority.HIGH)
                    .addedBy(listOwnerId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            ShoppingList shoppingList = ShoppingList.builder()
                    .name("Groceries")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(listOwnerId)
                    .items(new ArrayList<>(List.of(milk)))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            UUID shoppingListId = shoppingListRepository.save(shoppingList).id();
            UUID itemId = shoppingListRepository.findById(shoppingListId).get().items().getFirst().id();

            mockMvc.perform(delete("/api/shopping-lists/{id}/items/{itemId}", shoppingListId, itemId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("financial-tracker-test"))
                                            .claim("sub", unauthorizedUserId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json"))
                    .andExpect(status().isNotFound());

            Assertions.assertThat(shoppingListRepository.findById(shoppingListId).get().items()).hasSize(1);
        }
    }
}
