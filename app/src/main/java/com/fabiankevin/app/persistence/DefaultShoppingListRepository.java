package com.fabiankevin.app.persistence;

import com.fabiankevin.app.categories.domain.models.Category;
import com.fabiankevin.app.categories.persistence.entities.CategoryEntity;
import com.fabiankevin.app.models.shopping_list.ShoppingList;
import com.fabiankevin.app.persistence.entities.ShoppingItemEntity;
import com.fabiankevin.app.persistence.entities.ShoppingListEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaShoppingItemRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaShoppingListRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class DefaultShoppingListRepository implements ShoppingListRepository {
    private final JpaShoppingListRepository jpaShoppingListRepository;
    private final JpaShoppingItemRepository jpaShoppingItemRepository;
    private final EntityManager entityManager;

    @Transactional
    @Override
    public ShoppingList save(ShoppingList shoppingList) {
        if (shoppingList == null) {
            throw new InvalidDataAccessApiUsageException("ShoppingList must not be null");
        }
        ShoppingListEntity entity = ShoppingListEntity.from(shoppingList);
        entity.setId(Optional.ofNullable(shoppingList.id()).orElse(UUID.randomUUID()));
        for (ShoppingItemEntity item : entity.getItems()) {
            item.setId(Optional.ofNullable(shoppingList.id()).orElse(UUID.randomUUID()));
        }
        entityManager.persist(entity);

        return entity.toModel();
    }

    @Override
    @Transactional
    public ShoppingList update(ShoppingList shoppingList) {
        if (shoppingList == null) {
            throw new InvalidDataAccessApiUsageException("ShoppingList must not be null");
        }
        return jpaShoppingListRepository.save(ShoppingListEntity.from(shoppingList)).toModel();
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<ShoppingList> findById(UUID id) {
        return jpaShoppingListRepository.findByIdWithDetails(id)
                .map(entity -> {
                    Set<ShoppingItemEntity> itemEntities = jpaShoppingItemRepository.findByShoppingListId(id);
                    entity.getItems().addAll(itemEntities);
                    return entity;
                })
                .map(ShoppingListEntity::toModel);
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
        List<ShoppingListEntity> entities = jpaShoppingListRepository.findAllByUserId(userId);
        for (ShoppingListEntity entity : entities) {
            Set<ShoppingItemEntity> itemEntities = jpaShoppingItemRepository.findByShoppingListId(entity.getId());
            entity.getItems().addAll(itemEntities);
        }
        return entities.stream()
                .map(ShoppingListEntity::toModel)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaShoppingItemRepository.deleteByShoppingListId(id);
        jpaShoppingListRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteAllByUserId(UUID userId) {
        List<ShoppingListEntity> ownedLists = jpaShoppingListRepository.findOwnedByUserId(userId);
        for (ShoppingListEntity entity : ownedLists) {
            jpaShoppingItemRepository.deleteByShoppingListId(entity.getId());
        }
        jpaShoppingListRepository.deleteAll(ownedLists);
        jpaShoppingListRepository.deleteAllByUserId(userId);
    }
}
