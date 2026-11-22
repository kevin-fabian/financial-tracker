package com.fabiankevin.app.persistence;

import com.fabiankevin.app.categories.domain.models.Category;
import com.fabiankevin.app.categories.persistence.entities.CategoryEntity;
import com.fabiankevin.app.models.shopping_list.ShoppingList;
import com.fabiankevin.app.persistence.entities.ShoppingListEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaShoppingListRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class DefaultShoppingListRepository implements ShoppingListRepository {
    private final JpaShoppingListRepository jpaShoppingListRepository;
    private final EntityManager entityManager;

    @Transactional
    @Override
    public ShoppingList save(ShoppingList shoppingList) {
        if (shoppingList == null) {
            throw new InvalidDataAccessApiUsageException("ShoppingList must not be null");
        }
        ShoppingListEntity entity = ShoppingListEntity.from(shoppingList);
        entity.setId(Optional.ofNullable(entity.getId()).orElse(UUID.randomUUID()));
        entityManager.persist(entity);
        return entity.toModel();
    }

    @Override
    public ShoppingList update(ShoppingList shoppingList) {
        if (shoppingList == null) {
            throw new InvalidDataAccessApiUsageException("ShoppingList must not be null");
        }
        Optional.ofNullable(shoppingList.id())
                .orElseThrow(() -> new IllegalArgumentException("ShoppingList id must not be null for update"));
        ShoppingListEntity saved = jpaShoppingListRepository.save(ShoppingListEntity.from(shoppingList));
        return saved.toModel();
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<ShoppingList> findById(UUID id) {
        return jpaShoppingListRepository.findByIdWithDetails(id).map(ShoppingListEntity::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findCategoryById(UUID id) {
        return jpaShoppingListRepository.findByIdWithDetails(id)
                .map(ShoppingListEntity::getCategory)
                .map(CategoryEntity::toModel);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ShoppingList> findAllByUserId(UUID userId) {
        return jpaShoppingListRepository.findAllByUserId(userId).stream()
                .map(ShoppingListEntity::toModel)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaShoppingListRepository.deleteById(id);
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        jpaShoppingListRepository.deleteAllByUserId(userId);
    }
}
