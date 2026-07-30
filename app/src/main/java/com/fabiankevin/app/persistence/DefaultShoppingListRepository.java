package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.shopping_list.ShoppingList;
import com.fabiankevin.app.persistence.entities.ShoppingListEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaShoppingListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class DefaultShoppingListRepository implements ShoppingListRepository {
    private final JpaShoppingListRepository jpaShoppingListRepository;

    @Override
    public ShoppingList save(ShoppingList shoppingList) {
        ShoppingListEntity saved = jpaShoppingListRepository.save(ShoppingListEntity.from(shoppingList));
        return saved.toModel();
    }

    @Override
    public Optional<ShoppingList> findById(UUID id) {
        return jpaShoppingListRepository.findById(id).map(ShoppingListEntity::toModel);
    }
}
