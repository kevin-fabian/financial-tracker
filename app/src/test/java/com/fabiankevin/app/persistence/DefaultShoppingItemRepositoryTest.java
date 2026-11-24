package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.enums.ItemPriority;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import com.fabiankevin.app.persistence.entities.ShoppingListEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaShoppingItemRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@Import(DefaultShoppingItemRepositoryTest.ContextConfiguration.class)
@ExtendWith(SpringExtension.class)
@DataJpaTest
class DefaultShoppingItemRepositoryTest {

    @MockitoSpyBean
    private JpaShoppingItemRepository jpaShoppingItemRepository;

    @MockitoSpyBean
    private JpaShoppingListRepository jpaShoppingListRepository;

    @Autowired
    private ShoppingItemRepository shoppingItemRepository;

    @Autowired
    private JpaShoppingListRepository shoppingListJpaRepository;

    private ShoppingItem shoppingItem;
    private UUID shoppingListId;

    @TestConfiguration
    public static class ContextConfiguration {
        @Bean
        public ShoppingItemRepository shoppingItemRepository(JpaShoppingItemRepository jpaShoppingItemRepository, jakarta.persistence.EntityManager entityManager) {
            return new DefaultShoppingItemRepository(jpaShoppingItemRepository, entityManager);
        }
    }

    @BeforeEach
    void setUp() {
        shoppingListId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        shoppingItem = ShoppingItem.builder()
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
    }

    @Nested
    class Save {

        @Test
        void givenValidShoppingItem_shouldPersistAndRetrieveAllFields() {
            ShoppingItem saved = shoppingItemRepository.save(shoppingItem, shoppingListId);

            assertThat(saved.id()).isNotNull();
            assertThat(saved.name()).isEqualTo("Milk");
            assertThat(saved.category()).isEqualTo("Groceries");
            assertThat(saved.quantity()).isEqualTo(2.0);
            assertThat(saved.unit()).isEqualTo("liters");
            assertThat(saved.price()).isEqualTo(3.50);
            assertThat(saved.purchased()).isFalse();
            assertThat(saved.priority()).isEqualTo(ItemPriority.HIGH);
            assertThat(saved.notes()).isEqualTo("Whole milk");
            assertThat(saved.addedBy()).isNotNull();
            assertThat(saved.createdAt()).isNotNull();
            assertThat(saved.updatedAt()).isNotNull();

            var found = jpaShoppingItemRepository.findById(saved.id()).orElseThrow();
            assertThat(found.getShoppingListId()).isEqualTo(shoppingListId);
        }

        @Test
        void givenNullShoppingItem_shouldThrowInvalidDataAccessApiUsageException() {
            assertThatThrownBy(() -> shoppingItemRepository.save(null, UUID.randomUUID()))
                    .isInstanceOf(InvalidDataAccessApiUsageException.class);
        }

        @Test
        void givenShoppingItemWithExplicitId_shouldUseThatId() {
            UUID explicitId = UUID.randomUUID();
            ShoppingItem itemWithId = ShoppingItem.builder()
                    .id(explicitId)
                    .name("Water")
                    .category("Beverages")
                    .quantity(1.0)
                    .unit("bottle")
                    .price(1.50)
                    .purchased(false)
                    .priority(ItemPriority.LOW)
                    .notes(null)
                    .addedBy(UUID.randomUUID())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            ShoppingItem saved = shoppingItemRepository.save(itemWithId, shoppingListId);

            assertThat(saved.id()).isEqualTo(explicitId);

            var found = jpaShoppingItemRepository.findById(explicitId).orElseThrow();
            assertThat(found.getShoppingListId()).isEqualTo(shoppingListId);
        }
    }

    @Nested
    class Update {

        @Test
        void givenExistingItemWithChanges_shouldPersistUpdatedFields() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingListEntity shoppingList = ShoppingListEntity.builder()
                    .id(shoppingListId)
                    .name("Test List")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            shoppingListJpaRepository.saveAndFlush(shoppingList);

            ShoppingItem item = ShoppingItem.builder()
                    .id(null)
                    .name("Milk")
                    .category("Groceries")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.50)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .notes(null)
                    .addedBy(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            ShoppingItem saved = shoppingItemRepository.save(item, shoppingListId);

            ShoppingItem updated = ShoppingItem.builder()
                    .id(saved.id())
                    .name("Whole Milk")
                    .category("Dairy")
                    .quantity(3.0)
                    .unit("liters")
                    .price(4.00)
                    .purchased(true)
                    .priority(ItemPriority.MEDIUM)
                    .notes("Organic")
                    .addedBy(userId)
                    .createdAt(saved.createdAt())
                    .updatedAt(Instant.now())
                    .build();

            ShoppingItem result = shoppingItemRepository.update(updated, shoppingListId);

            assertThat(result.id()).isEqualTo(saved.id());
            assertThat(result.name()).isEqualTo("Whole Milk");
            assertThat(result.category()).isEqualTo("Dairy");
            assertThat(result.quantity()).isEqualTo(3.0);
            assertThat(result.unit()).isEqualTo("liters");
            assertThat(result.price()).isEqualTo(4.00);
            assertThat(result.purchased()).isTrue();
            assertThat(result.priority()).isEqualTo(ItemPriority.MEDIUM);
            assertThat(result.notes()).isEqualTo("Organic");

            var found = jpaShoppingItemRepository.findById(saved.id()).orElseThrow();
            assertThat(found.getShoppingListId()).isEqualTo(shoppingListId);
        }

        @Test
        void givenNullItem_shouldThrowInvalidDataAccessApiUsageException() {
            assertThatThrownBy(() -> shoppingItemRepository.update(null, UUID.randomUUID()))
                    .isInstanceOf(InvalidDataAccessApiUsageException.class);
        }
    }

    @Nested
    class FindByShoppingListId {

        @Test
        void givenExistingShoppingListId_shouldReturnAllItemsForThatList() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingListEntity shoppingList = ShoppingListEntity.builder()
                    .id(shoppingListId)
                    .name("Test List")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            shoppingListJpaRepository.saveAndFlush(shoppingList);
            reset(jpaShoppingItemRepository);

            ShoppingItem apples = ShoppingItem.builder()
                    .id(null)
                    .name("Apples")
                    .category("Fruits")
                    .quantity(6.0)
                    .unit("pieces")
                    .price(1.25)
                    .purchased(false)
                    .priority(ItemPriority.LOW)
                    .notes(null)
                    .addedBy(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            ShoppingItem cheese = ShoppingItem.builder()
                    .id(null)
                    .name("Cheese")
                    .category("Dairy")
                    .quantity(1.0)
                    .unit("block")
                    .price(5.99)
                    .purchased(true)
                    .priority(ItemPriority.MEDIUM)
                    .notes("Cheddar")
                    .addedBy(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            shoppingItemRepository.save(apples, shoppingListId);
            shoppingItemRepository.save(cheese, shoppingListId);

            Set<ShoppingItem> result = shoppingItemRepository.findByShoppingListId(shoppingListId);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ShoppingItem::name)
                    .containsExactlyInAnyOrder("Apples", "Cheese");
            assertThat(result).extracting(ShoppingItem::category)
                    .containsExactlyInAnyOrder("Fruits", "Dairy");

            ShoppingItem applesItem = result.stream()
                    .filter(item -> "Apples".equals(item.name()))
                    .findFirst().orElseThrow();
            assertThat(applesItem.quantity()).isEqualTo(6.0);
            assertThat(applesItem.purchased()).isFalse();

            ShoppingItem cheeseItem = result.stream()
                    .filter(item -> "Cheese".equals(item.name()))
                    .findFirst().orElseThrow();
            assertThat(cheeseItem.price()).isEqualTo(5.99);
            assertThat(cheeseItem.purchased()).isTrue();
        }

        @Test
        void givenNonExistentShoppingListId_shouldReturnEmptySet() {
            Set<ShoppingItem> result = shoppingItemRepository.findByShoppingListId(UUID.randomUUID());

            assertThat(result).isEmpty();
        }

        @Test
        void givenMultipleShoppingLists_shouldReturnOnlyMatchingItems() {
            UUID anotherListId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingListEntity list1 = ShoppingListEntity.builder()
                    .id(shoppingListId)
                    .name("List 1")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            ShoppingListEntity list2 = ShoppingListEntity.builder()
                    .id(anotherListId)
                    .name("List 2")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            shoppingListJpaRepository.saveAndFlush(list1);
            shoppingListJpaRepository.saveAndFlush(list2);
            reset(jpaShoppingItemRepository);

            ShoppingItem list1Item = ShoppingItem.builder()
                    .id(null)
                    .name("Item for List 1")
                    .category("Cat A")
                    .quantity(1.0)
                    .unit("piece")
                    .price(10.00)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .notes(null)
                    .addedBy(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ShoppingItem list2Item = ShoppingItem.builder()
                    .id(null)
                    .name("Item for List 2")
                    .category("Cat B")
                    .quantity(2.0)
                    .unit("pieces")
                    .price(20.00)
                    .purchased(false)
                    .priority(ItemPriority.LOW)
                    .notes(null)
                    .addedBy(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            shoppingItemRepository.save(list1Item, shoppingListId);
            shoppingItemRepository.save(list2Item, anotherListId);

            Set<ShoppingItem> result = shoppingItemRepository.findByShoppingListId(shoppingListId);

            assertThat(result).hasSize(1);
            assertThat(result).extracting(ShoppingItem::name)
                    .containsExactly("Item for List 1");
        }
    }

    @Nested
    class CountByShoppingListId {

        @Test
        void givenShoppingListWithItems_shouldReturnCorrectCount() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingListEntity shoppingList = ShoppingListEntity.builder()
                    .id(shoppingListId)
                    .name("Test List")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            shoppingListJpaRepository.saveAndFlush(shoppingList);
            reset(jpaShoppingItemRepository);

            ShoppingItem item1 = ShoppingItem.builder()
                    .id(null)
                    .name("Item 1")
                    .category("Cat A")
                    .quantity(1.0)
                    .unit("piece")
                    .price(10.00)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .notes(null)
                    .addedBy(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            ShoppingItem item2 = ShoppingItem.builder()
                    .id(null)
                    .name("Item 2")
                    .category("Cat B")
                    .quantity(2.0)
                    .unit("pieces")
                    .price(20.00)
                    .purchased(false)
                    .priority(ItemPriority.LOW)
                    .notes(null)
                    .addedBy(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            ShoppingItem item3 = ShoppingItem.builder()
                    .id(null)
                    .name("Item 3")
                    .category("Cat C")
                    .quantity(3.0)
                    .unit("pieces")
                    .price(30.00)
                    .purchased(true)
                    .priority(ItemPriority.MEDIUM)
                    .notes(null)
                    .addedBy(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            shoppingItemRepository.save(item1, shoppingListId);
            shoppingItemRepository.save(item2, shoppingListId);
            shoppingItemRepository.save(item3, shoppingListId);

            long count = shoppingItemRepository.countByShoppingListId(shoppingListId);

            assertThat(count).isEqualTo(3);
        }

        @Test
        void givenShoppingListWithNoItems_shouldReturnZero() {
            long count = shoppingItemRepository.countByShoppingListId(shoppingListId);

            assertThat(count).isZero();
        }
    }

    @Nested
    class DeleteById {

        @Test
        void givenExistingItemId_shouldDeleteThatItem() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingListEntity shoppingList = ShoppingListEntity.builder()
                    .id(shoppingListId)
                    .name("Test List")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            shoppingListJpaRepository.saveAndFlush(shoppingList);
            reset(jpaShoppingItemRepository);

            ShoppingItem item = ShoppingItem.builder()
                    .id(null)
                    .name("Milk")
                    .category("Groceries")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.50)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .notes(null)
                    .addedBy(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            ShoppingItem saved = shoppingItemRepository.save(item, shoppingListId);
            UUID itemId = saved.id();

            assertThat(shoppingItemRepository.findByShoppingListId(shoppingListId)).hasSize(1);

            shoppingItemRepository.deleteById(itemId);

            assertThat(shoppingItemRepository.findByShoppingListId(shoppingListId)).isEmpty();
            verify(jpaShoppingItemRepository, times(1)).deleteById(itemId);
        }

        @Test
        void givenNonExistentItemId_shouldNotThrow() {
            assertThatCode(() -> shoppingItemRepository.deleteById(UUID.randomUUID()))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class DeleteByShoppingListId {

        @Test
        void givenShoppingListWithItems_shouldDeleteAllItems() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingListEntity shoppingList = ShoppingListEntity.builder()
                    .id(shoppingListId)
                    .name("Test List")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            shoppingListJpaRepository.saveAndFlush(shoppingList);
            reset(jpaShoppingItemRepository);

            ShoppingItem item1 = ShoppingItem.builder()
                    .id(null)
                    .name("Item 1")
                    .category("Cat A")
                    .quantity(1.0)
                    .unit("piece")
                    .price(10.00)
                    .purchased(false)
                    .priority(ItemPriority.HIGH)
                    .notes(null)
                    .addedBy(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            ShoppingItem item2 = ShoppingItem.builder()
                    .id(null)
                    .name("Item 2")
                    .category("Cat B")
                    .quantity(2.0)
                    .unit("pieces")
                    .price(20.00)
                    .purchased(false)
                    .priority(ItemPriority.LOW)
                    .notes(null)
                    .addedBy(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            shoppingItemRepository.save(item1, shoppingListId);
            shoppingItemRepository.save(item2, shoppingListId);

            assertThat(shoppingItemRepository.findByShoppingListId(shoppingListId)).hasSize(2);
            assertThat(shoppingItemRepository.countByShoppingListId(shoppingListId)).isEqualTo(2);

            shoppingItemRepository.deleteByShoppingListId(shoppingListId);

            assertThat(shoppingItemRepository.findByShoppingListId(shoppingListId)).isEmpty();
            assertThat(shoppingItemRepository.countByShoppingListId(shoppingListId)).isZero();

            verify(jpaShoppingItemRepository, times(1)).deleteByShoppingListId(shoppingListId);
        }

        @Test
        void givenNonExistentShoppingListId_shouldNotThrow() {
            assertThatCode(() -> shoppingItemRepository.deleteByShoppingListId(UUID.randomUUID()))
                    .doesNotThrowAnyException();
        }
    }
}
