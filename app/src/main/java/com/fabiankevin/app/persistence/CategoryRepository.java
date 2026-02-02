package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Category;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    Optional<Category> findById(UUID id);
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByNameAndUserId(String name, UUID userId);
    Category save(Category category);
    void deleteById(UUID id);
    void deleteByIdAndUserId(UUID id, UUID userId);
}
