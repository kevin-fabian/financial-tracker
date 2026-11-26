package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.ShoppingItemEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;
import java.util.UUID;

public interface JpaShoppingItemRepository extends JpaRepository<ShoppingItemEntity, UUID> {
    Set<ShoppingItemEntity> findByShoppingListId(UUID shoppingListId);

    long countByShoppingListId(UUID shoppingListId);

    void deleteByShoppingListId(UUID shoppingListId);

    void deleteById(@NonNull UUID id);
}
