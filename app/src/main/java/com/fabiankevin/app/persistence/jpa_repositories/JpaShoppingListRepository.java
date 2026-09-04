package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.ShoppingListEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaShoppingListRepository extends JpaRepository<ShoppingListEntity, UUID> {
    @EntityGraph(attributePaths = {"items"})
    @Query("""
            SELECT DISTINCT sl FROM ShoppingListEntity sl
            LEFT JOIN FETCH sl.sharedWithUserIds su
            WHERE sl.userId = :userId OR su = :userId
            ORDER BY sl.id ASC
            """)
    List<ShoppingListEntity> findAllByUserId(@Param("userId") UUID userId);
}
