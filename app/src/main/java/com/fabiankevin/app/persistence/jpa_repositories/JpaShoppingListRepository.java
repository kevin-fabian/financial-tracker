package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.ShoppingListEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaShoppingListRepository extends JpaRepository<ShoppingListEntity, UUID> {
    List<ShoppingListEntity> findAllByUserId(UUID userId);
}
