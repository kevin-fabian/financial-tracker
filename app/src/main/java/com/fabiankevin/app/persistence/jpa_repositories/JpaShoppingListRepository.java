package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.ShoppingListEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaShoppingListRepository extends JpaRepository<ShoppingListEntity, UUID> {
    @EntityGraph(attributePaths = {"items", "category"})
    @Query("""
            SELECT DISTINCT sl FROM ShoppingListEntity sl
            LEFT JOIN FETCH sl.sharedWithUserIds su
            WHERE sl.userId = :userId OR su = :userId
            ORDER BY sl.createdAt DESC
            """)
    List<ShoppingListEntity> findAllByUserId(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"items", "category"})
    @Query("""
            SELECT DISTINCT sl FROM ShoppingListEntity sl
            LEFT JOIN FETCH sl.items i
            WHERE sl.id = :id
            ORDER BY i.createdAt ASC
            """)
    Optional<ShoppingListEntity> findByIdWithDetails(@Param("id") UUID id);
}
