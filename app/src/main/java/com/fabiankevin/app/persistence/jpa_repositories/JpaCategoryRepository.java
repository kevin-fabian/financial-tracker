package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaCategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    Optional<CategoryEntity> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByNameAndTransactionTypeAndUserId(String name, TransactionType type, UUID userId);
    int deleteByIdAndUserId(UUID id, UUID userId);
    Page<CategoryEntity> findAllByUserId(UUID userId, Pageable pageable);
    Page<CategoryEntity> findAllByUserIdAndTransactionType(UUID userId, TransactionType type, Pageable pageable);
    Optional<CategoryEntity> findFirstByActiveFalseAndNameAndTransactionTypeAndUserId(String name, TransactionType type, UUID userId);
}
