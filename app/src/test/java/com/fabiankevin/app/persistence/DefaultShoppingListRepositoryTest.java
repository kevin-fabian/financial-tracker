package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.enums.ItemPriority;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import com.fabiankevin.app.models.shopping_list.ShoppingList;
import com.fabiankevin.app.persistence.jpa_repositories.JpaShoppingListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import(DefaultShoppingListRepositoryTest.ContextConfiguration.class)
@ExtendWith(SpringExtension.class)
@DataJpaTest
class DefaultShoppingListRepositoryTest {

    @MockitoSpyBean
    private JpaShoppingListRepository jpaShoppingListRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    private ShoppingList shoppingList;

    @TestConfiguration
    public static class ContextConfiguration {
        @Bean
        public ShoppingListRepository shoppingListRepository(JpaShoppingListRepository jpaShoppingListRepository) {
            return new DefaultShoppingListRepository(jpaShoppingListRepository);
        }
    }

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        ShoppingItem item = ShoppingItem.builder()
                .id(null)
                .name("Milk")
                .category("Groceries")
                .quantity(2.0)
                .unit("liters")
                .price(3.50)
                .purchased(false)
                .priority(ItemPriority.HIGH)
                .notes("Whole milk")
                .addedBy(userId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        shoppingList = ShoppingList.builder()
                .id(null)
                .name("Weekly Groceries")
                .description("Weekly grocery run")
                .status(ShoppingListStatus.ACTIVE)
                .items(List.of(item))
                .userId(userId)
                .budget(100.00)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Nested
    class Save {

        @Test
        void givenValidShoppingList_thenPersistsAndRetrievesAllFieldsIncludingCascadeItems() {
            ShoppingList saved = shoppingListRepository.save(shoppingList);

            assertThat(saved.id()).isNotNull();
            assertThat(saved.name()).isEqualTo("Weekly Groceries");
            assertThat(saved.description()).isEqualTo("Weekly grocery run");
            assertThat(saved.status()).isEqualTo(ShoppingListStatus.ACTIVE);
            assertThat(saved.userId()).isNotNull();
            assertThat(saved.budget()).isEqualTo(100.00);
            assertThat(saved.items()).hasSize(1);

            ShoppingItem savedItem = saved.items().getFirst();
            assertThat(savedItem.id()).isNotNull();
            assertThat(savedItem.name()).isEqualTo("Milk");
            assertThat(savedItem.category()).isEqualTo("Groceries");
            assertThat(savedItem.quantity()).isEqualTo(2.0);
            assertThat(savedItem.unit()).isEqualTo("liters");
            assertThat(savedItem.price()).isEqualTo(3.50);
            assertThat(savedItem.purchased()).isFalse();
            assertThat(savedItem.priority()).isEqualTo(ItemPriority.HIGH);
            assertThat(savedItem.notes()).isEqualTo("Whole milk");
            assertThat(savedItem.addedBy()).isNotNull();

            verify(jpaShoppingListRepository, times(1)).save(any());
        }

        @Test
        void givenShoppingListWithEmptyItems_thenPersistsSuccessfully() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingList emptyList = ShoppingList.builder()
                    .id(null)
                    .name("Empty List")
                    .status(ShoppingListStatus.ACTIVE)
                    .items(List.of())
                    .userId(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingList saved = shoppingListRepository.save(emptyList);

            assertThat(saved.id()).isNotNull();
            assertThat(saved.name()).isEqualTo("Empty List");
            assertThat(saved.items()).isEmpty();

            verify(jpaShoppingListRepository, times(1)).save(any());
        }

        @Test
        void givenNullShoppingList_thenThrowsInvalidDataAccessApiUsageException() {
            assertThatThrownBy(() -> shoppingListRepository.save(null))
                    .isInstanceOf(InvalidDataAccessApiUsageException.class);

            verify(jpaShoppingListRepository, times(1)).save(any());
        }
    }

    @Nested
    class FindById {

        @Test
        void givenExistingId_thenReturnsShoppingListWithAllFields() {
            ShoppingList saved = shoppingListRepository.save(shoppingList);
            UUID id = saved.id();

            Optional<ShoppingList> found = shoppingListRepository.findById(id);

            assertThat(found).isPresent();
            ShoppingList result = found.get();
            assertThat(result.id()).isEqualTo(id);
            assertThat(result.name()).isEqualTo("Weekly Groceries");
            assertThat(result.description()).isEqualTo("Weekly grocery run");
            assertThat(result.status()).isEqualTo(ShoppingListStatus.ACTIVE);
            assertThat(result.userId()).isEqualTo(shoppingList.userId());
            assertThat(result.budget()).isEqualTo(100.00);
            assertThat(result.items()).hasSize(1);

            ShoppingItem item = result.items().getFirst();
            assertThat(item.id()).isNotNull();
            assertThat(item.name()).isEqualTo("Milk");
            assertThat(item.category()).isEqualTo("Groceries");
            assertThat(item.quantity()).isEqualTo(2.0);
            assertThat(item.unit()).isEqualTo("liters");
            assertThat(item.price()).isEqualTo(3.50);
            assertThat(item.purchased()).isFalse();
            assertThat(item.priority()).isEqualTo(ItemPriority.HIGH);
            assertThat(item.notes()).isEqualTo("Whole milk");
            assertThat(item.addedBy()).isNotNull();
        }

        @Test
        void givenNonExistentId_thenReturnsEmptyOptional() {
            Optional<ShoppingList> found = shoppingListRepository.findById(UUID.randomUUID());

            assertThat(found).isEmpty();
        }
    }

    @Nested
    class FindAllByUserId {

        @Test
        void givenOwnedShoppingList_thenReturnsIt() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingList ownedList = ShoppingList.builder()
                    .id(null)
                    .name("Owned List")
                    .status(ShoppingListStatus.ACTIVE)
                    .items(List.of())
                    .userId(userId)
                    .budget(50.00)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            shoppingListRepository.save(ownedList);

            List<ShoppingList> result = shoppingListRepository.findAllByUserId(userId);

            assertThat(result).hasSize(1);
            ShoppingList list = result.getFirst();
            assertThat(list.id()).isNotNull();
            assertThat(list.name()).isEqualTo("Owned List");
            assertThat(list.description()).isNull();
            assertThat(list.status()).isEqualTo(ShoppingListStatus.ACTIVE);
            assertThat(list.userId()).isEqualTo(userId);
            assertThat(list.budget()).isEqualTo(50.00);
            assertThat(list.items()).isEmpty();
        }

        @Test
        void givenSharedShoppingList_thenReturnsItToSharedUser() {
            UUID ownerUserId = UUID.randomUUID();
            UUID sharedUserId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingList sharedList = ShoppingList.builder()
                    .id(null)
                    .name("Shared List")
                    .status(ShoppingListStatus.ACTIVE)
                    .items(List.of())
                    .userId(ownerUserId)
                    .sharedWithUserIds(List.of(sharedUserId))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            shoppingListRepository.save(sharedList);

            List<ShoppingList> result = shoppingListRepository.findAllByUserId(sharedUserId);

            assertThat(result).hasSize(1);
            ShoppingList list = result.getFirst();
            assertThat(list.id()).isNotNull();
            assertThat(list.name()).isEqualTo("Shared List");
            assertThat(list.userId()).isEqualTo(ownerUserId);
            assertThat(list.sharedWithUserIds()).containsExactly(sharedUserId);
        }

        @Test
        void givenOwnedAndSharedLists_thenReturnsBoth() {
            UUID userId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingList ownedList = ShoppingList.builder()
                    .id(null)
                    .name("My List")
                    .status(ShoppingListStatus.ACTIVE)
                    .items(List.of())
                    .userId(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingList sharedList = ShoppingList.builder()
                    .id(null)
                    .name("Shared With Me")
                    .status(ShoppingListStatus.ACTIVE)
                    .items(List.of())
                    .userId(otherUserId)
                    .sharedWithUserIds(List.of(userId))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            shoppingListRepository.save(ownedList);
            shoppingListRepository.save(sharedList);

            List<ShoppingList> result = shoppingListRepository.findAllByUserId(userId);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ShoppingList::name)
                    .containsExactlyInAnyOrder("My List", "Shared With Me");
        }

        @Test
        void givenNoMatchingLists_thenReturnsEmptyList() {
            List<ShoppingList> result = shoppingListRepository.findAllByUserId(UUID.randomUUID());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class DeleteById {

        @Test
        void givenExistingId_thenDeletesShoppingList() {
            ShoppingList saved = shoppingListRepository.save(shoppingList);
            UUID id = saved.id();

            shoppingListRepository.deleteById(id);

            Optional<ShoppingList> found = shoppingListRepository.findById(id);
            assertThat(found).isEmpty();
            verify(jpaShoppingListRepository, times(1)).deleteById(id);
        }

        @Test
        void givenNonExistentId_thenDoesNotThrow() {
            assertThatCode(() -> shoppingListRepository.deleteById(UUID.randomUUID()))
                    .doesNotThrowAnyException();
        }
    }
}
