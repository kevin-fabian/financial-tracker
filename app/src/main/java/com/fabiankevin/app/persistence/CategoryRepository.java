package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.services.queries.PageQuery;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);
    Optional<Category> findById(UUID id);
    boolean existsByNameAndUserId(String name, UUID userId);
    Category save(Category category);
    void deleteByIdAndUserId(UUID id, UUID userId);
    Page<Category> findAllByPageQuery(PageQuery query, UUID userId);
}
