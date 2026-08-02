package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.ShoppingListEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaShoppingListRepository extends JpaRepository<ShoppingListEntity, UUID> {
    @EntityGraph(attributePaths = {"sharedWithUserIds", "items"})
    List<ShoppingListEntity> findAllByUserId(UUID userId);
}
