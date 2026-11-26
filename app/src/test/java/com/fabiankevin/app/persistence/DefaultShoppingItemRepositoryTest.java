package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.enums.ItemPriority;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import com.fabiankevin.app.models.shopping_list.ShoppingList;
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
    private ShoppingList shoppingList;

    @TestConfiguration
    public static class ContextConfiguration {
        @Bean
        public ShoppingItemRepository shoppingItemRepository(JpaShoppingItemRepository jpaShoppingItemRepository, JpaShoppingListRepository jpaShoppingListRepository, jakarta.persistence.EntityManager entityManager) {
            return new DefaultShoppingItemRepository(jpaShoppingItemRepository, jpaShoppingListRepository, entityManager);
        }
    }

    @BeforeEach
    void setUp() {
        UUID listId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        shoppingList = ShoppingList.builder()
                .id(listId)
                .name("Test List")
                .status(ShoppingListStatus.ACTIVE)
                .userId(userId)
                .createdAt(now)
                .updatedAt(now)
                .build();

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
            shoppingListJpaRepository.saveAndFlush(toShoppingListEntity(shoppingList));

            ShoppingItem saved = shoppingItemRepository.save(shoppingItem, shoppingList);

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
            assertThat(found.getShoppingList().getId()).isEqualTo(shoppingList.id());
        }

        @Test
        void givenNullShoppingItem_shouldThrowInvalidDataAccessApiUsageException() {
            assertThatThrownBy(() -> shoppingItemRepository.save(null, shoppingList))
                    .isInstanceOf(InvalidDataAccessApiUsageException.class);
        }

        @Test
        void givenShoppingItemWithExplicitId_shouldUseThatId() {
            shoppingListJpaRepository.saveAndFlush(toShoppingListEntity(shoppingList));

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

            ShoppingItem saved = shoppingItemRepository.save(itemWithId, shoppingList);

            assertThat(saved.id()).isEqualTo(explicitId);

            var found = jpaShoppingItemRepository.findById(explicitId).orElseThrow();
            assertThat(found.getShoppingList().getId()).isEqualTo(shoppingList.id());
        }
    }

    @Nested
    class Update {

        @Test
        void givenExistingItemWithChanges_shouldPersistUpdatedFields() {
            shoppingListJpaRepository.saveAndFlush(toShoppingListEntity(shoppingList));
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
                    .addedBy(shoppingList.userId())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            ShoppingItem saved = shoppingItemRepository.save(item, shoppingList);

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
                    .addedBy(shoppingList.userId())
                    .createdAt(saved.createdAt())
                    .updatedAt(Instant.now())
                    .build();

            ShoppingItem result = shoppingItemRepository.update(updated, shoppingList);

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
            assertThat(found.getShoppingList().getId()).isEqualTo(shoppingList.id());
        }

        @Test
        void givenNullItem_shouldThrowInvalidDataAccessApiUsageException() {
            assertThatThrownBy(() -> shoppingItemRepository.update(null, shoppingList))
                    .isInstanceOf(InvalidDataAccessApiUsageException.class);
        }
    }

    @Nested
    class FindByShoppingListId {

        @Test
        void givenExistingShoppingListId_shouldReturnAllItemsForThatList() {
            shoppingListJpaRepository.saveAndFlush(toShoppingListEntity(shoppingList));
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
                    .addedBy(shoppingList.userId())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
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
                    .addedBy(shoppingList.userId())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            shoppingItemRepository.save(apples, shoppingList);
            shoppingItemRepository.save(cheese, shoppingList);

            Set<ShoppingItem> result = shoppingItemRepository.findByShoppingListId(shoppingList.id());

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

            ShoppingListEntity list1Entity = ShoppingListEntity.builder()
                    .id(shoppingList.id())
                    .name("List 1")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            ShoppingListEntity list2Entity = ShoppingListEntity.builder()
                    .id(anotherListId)
                    .name("List 2")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            shoppingListJpaRepository.saveAndFlush(list1Entity);
            shoppingListJpaRepository.saveAndFlush(list2Entity);
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

            shoppingItemRepository.save(list1Item, shoppingList);
            ShoppingList secondList = ShoppingList.builder()
                    .id(anotherListId)
                    .name("List 2")
                    .status(ShoppingListStatus.ACTIVE)
                    .userId(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            shoppingItemRepository.save(list2Item, secondList);

            Set<ShoppingItem> result = shoppingItemRepository.findByShoppingListId(shoppingList.id());

            assertThat(result).hasSize(1);
            assertThat(result).extracting(ShoppingItem::name)
                    .containsExactly("Item for List 1");
        }
    }

    @Nested
    class CountByShoppingListId {

        @Test
        void givenShoppingListWithItems_shouldReturnCorrectCount() {
            shoppingListJpaRepository.saveAndFlush(toShoppingListEntity(shoppingList));
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
                    .addedBy(shoppingList.userId())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
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
                    .addedBy(shoppingList.userId())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
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
                    .addedBy(shoppingList.userId())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            shoppingItemRepository.save(item1, shoppingList);
            shoppingItemRepository.save(item2, shoppingList);
            shoppingItemRepository.save(item3, shoppingList);

            long count = shoppingItemRepository.countByShoppingListId(shoppingList.id());

            assertThat(count).isEqualTo(3);
        }

        @Test
        void givenShoppingListWithNoItems_shouldReturnZero() {
            long count = shoppingItemRepository.countByShoppingListId(shoppingList.id());

            assertThat(count).isZero();
        }
    }

    @Nested
    class DeleteById {

        @Test
        void givenExistingItemId_shouldDeleteThatItem() {
            shoppingListJpaRepository.saveAndFlush(toShoppingListEntity(shoppingList));
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
                    .addedBy(shoppingList.userId())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            ShoppingItem saved = shoppingItemRepository.save(item, shoppingList);
            UUID itemId = saved.id();

            assertThat(shoppingItemRepository.findByShoppingListId(shoppingList.id())).hasSize(1);

            shoppingItemRepository.deleteById(itemId);

            assertThat(shoppingItemRepository.findByShoppingListId(shoppingList.id())).isEmpty();
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
            shoppingListJpaRepository.saveAndFlush(toShoppingListEntity(shoppingList));
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
                    .addedBy(shoppingList.userId())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
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
                    .addedBy(shoppingList.userId())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            shoppingItemRepository.save(item1, shoppingList);
            shoppingItemRepository.save(item2, shoppingList);

            assertThat(shoppingItemRepository.findByShoppingListId(shoppingList.id())).hasSize(2);
            assertThat(shoppingItemRepository.countByShoppingListId(shoppingList.id())).isEqualTo(2);

            shoppingItemRepository.deleteByShoppingListId(shoppingList.id());

            assertThat(shoppingItemRepository.findByShoppingListId(shoppingList.id())).isEmpty();
            assertThat(shoppingItemRepository.countByShoppingListId(shoppingList.id())).isZero();

            verify(jpaShoppingItemRepository, times(1)).deleteByShoppingListId(shoppingList.id());
        }

        @Test
        void givenNonExistentShoppingListId_shouldNotThrow() {
            assertThatCode(() -> shoppingItemRepository.deleteByShoppingListId(UUID.randomUUID()))
                    .doesNotThrowAnyException();
        }
    }

    private ShoppingListEntity toShoppingListEntity(ShoppingList shoppingList) {
        return ShoppingListEntity.builder()
                .id(shoppingList.id())
                .name(shoppingList.name())
                .status(shoppingList.status())
                .userId(shoppingList.userId())
                .createdAt(shoppingList.createdAt())
                .updatedAt(shoppingList.updatedAt())
                .build();
    }
}
