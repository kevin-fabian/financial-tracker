package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.enums.ItemPriority;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import com.fabiankevin.app.models.shopping_list.ShoppingList;
import com.fabiankevin.app.persistence.jpa_repositories.JpaShoppingListRepository;
import org.assertj.core.api.Assertions;
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
import java.util.UUID;

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
    class FindAllByUserId {
        @Test
        void givenOwnedShoppingList_returnsIt() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();

            ShoppingList ownedList = ShoppingList.builder()
                    .id(null)
                    .name("Owned List")
                    .status(ShoppingListStatus.ACTIVE)
                    .items(List.of())
                    .userId(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            shoppingListRepository.save(ownedList);

            List<ShoppingList> result = shoppingListRepository.findAllByUserId(userId);

            Assertions.assertThat(result).hasSize(1);
            Assertions.assertThat(result.getFirst().name()).isEqualTo("Owned List");
            Assertions.assertThat(result.getFirst().userId()).isEqualTo(userId);
        }

        @Test
        void givenSharedShoppingList_returnsItToSharedUser() {
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

            Assertions.assertThat(result).hasSize(1);
            Assertions.assertThat(result.getFirst().name()).isEqualTo("Shared List");
            Assertions.assertThat(result.getFirst().userId()).isEqualTo(ownerUserId);
        }

        @Test
        void givenOwnedAndSharedLists_returnsBoth() {
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

            Assertions.assertThat(result).hasSize(2);
            Assertions.assertThat(result).extracting(ShoppingList::name)
                    .containsExactlyInAnyOrder("My List", "Shared With Me");
        }

        @Test
        void givenNoMatchingLists_returnsEmptyList() {
            UUID userId = UUID.randomUUID();

            List<ShoppingList> result = shoppingListRepository.findAllByUserId(userId);

            Assertions.assertThat(result).isEmpty();
        }
    }

    @Nested
    class Save {
        @Test
        void givenValidShoppingList_persistsAndRetrievesAllFieldsIncludingCascadeItems() {
            ShoppingList saved = shoppingListRepository.save(shoppingList);

            Assertions.assertThat(saved.id()).isNotNull();
            Assertions.assertThat(saved.name()).isEqualTo("Weekly Groceries");
            Assertions.assertThat(saved.description()).isEqualTo("Weekly grocery run");
            Assertions.assertThat(saved.status()).isEqualTo(ShoppingListStatus.ACTIVE);
            Assertions.assertThat(saved.userId()).isNotNull();
            Assertions.assertThat(saved.budget()).isEqualTo(100.00);
            Assertions.assertThat(saved.items()).hasSize(1);

            ShoppingItem savedItem = saved.items().getFirst();
            Assertions.assertThat(savedItem.id()).isNotNull();
            Assertions.assertThat(savedItem.name()).isEqualTo("Milk");
            Assertions.assertThat(savedItem.category()).isEqualTo("Groceries");
            Assertions.assertThat(savedItem.quantity()).isEqualTo(2.0);
            Assertions.assertThat(savedItem.unit()).isEqualTo("liters");
            Assertions.assertThat(savedItem.price()).isEqualTo(3.50);
            Assertions.assertThat(savedItem.purchased()).isFalse();
            Assertions.assertThat(savedItem.priority()).isEqualTo(ItemPriority.HIGH);
            Assertions.assertThat(savedItem.notes()).isEqualTo("Whole milk");
            Assertions.assertThat(savedItem.addedBy()).isNotNull();

            verify(jpaShoppingListRepository, times(1)).save(any());
        }

        @Test
        void givenShoppingListWithEmptyItems_persistsSuccessfully() {
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

            Assertions.assertThat(saved.id()).isNotNull();
            Assertions.assertThat(saved.name()).isEqualTo("Empty List");
            Assertions.assertThat(saved.items()).isEmpty();

            verify(jpaShoppingListRepository, times(1)).save(any());
        }

        @Test
        void givenNullShoppingList_throwsException() {
            Assertions.assertThatThrownBy(() -> shoppingListRepository.save(null))
                    .isInstanceOf(InvalidDataAccessApiUsageException.class);

            verify(jpaShoppingListRepository, times(1)).save(any());
        }
    }
}
