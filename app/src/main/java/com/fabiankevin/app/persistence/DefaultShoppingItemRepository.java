package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.shopping_list.ShoppingItem;
import com.fabiankevin.app.persistence.entities.ShoppingItemEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaShoppingItemRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class DefaultShoppingItemRepository implements ShoppingItemRepository {
    private final JpaShoppingItemRepository jpaShoppingItemRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public ShoppingItem save(ShoppingItem shoppingItem, UUID shoppingListId) {
        if (shoppingItem == null) {
            throw new InvalidDataAccessApiUsageException("ShoppingItem must not be null");
        }
        ShoppingItemEntity entity = ShoppingItemEntity.from(shoppingItem, shoppingListId);
        entity.setId(Optional.ofNullable(entity.getId()).orElse(UUID.randomUUID()));
        entityManager.persist(entity);
        return entity.toModel();
    }

    @Override
    @Transactional
    public ShoppingItem update(ShoppingItem shoppingItem, UUID shoppingListId) {
        if (shoppingItem == null) {
            throw new InvalidDataAccessApiUsageException("ShoppingItem must not be null");
        }
        ShoppingItemEntity entity = ShoppingItemEntity.from(shoppingItem, shoppingListId);
        return jpaShoppingItemRepository.save(entity).toModel();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<ShoppingItem> findByShoppingListId(UUID shoppingListId) {
        return jpaShoppingItemRepository.findByShoppingListId(shoppingListId).stream()
                .map(ShoppingItemEntity::toModel)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByShoppingListId(UUID shoppingListId) {
        return jpaShoppingItemRepository.countByShoppingListId(shoppingListId);
    }

    @Override
    @Transactional
    public void deleteByShoppingListId(UUID shoppingListId) {
        jpaShoppingItemRepository.deleteByShoppingListId(shoppingListId);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaShoppingItemRepository.deleteById(id);
    }
}
