package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.CategorySummary;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.queries.PageQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);
    Optional<CategorySummary> findByIdAndUserIdWithSummary(UUID id, UUID userId);
    Optional<Category> findById(UUID id);
    boolean existsByNameAndTypeAndUserId(String name, TransactionType type, UUID userId);
    Optional<Category> findInactiveByNameAndTypeAndUserId(String name, TransactionType type, UUID userId);
    Optional<Category> findByNameAndTypeAndUserId(String name, TransactionType type, UUID userId);
    List<Category> findAllByNamesAndUserId(List<String> names, UUID userId);
    Category save(Category category);
    List<Category> saveAll(List<Category> categories);
    int deleteByIdAndUserId(UUID id, UUID userId);
    Page<Category> findAllByPageQuery(PageQuery query, UUID userId, TransactionType type);
    Page<CategorySummary> findAllByPageQueryWithSummary(PageQuery query, UUID userId, TransactionType type, List<UUID> userIds);
    List<Category> findAllByNamesIn(List<String> names);

    List<Category> findAllByUserId(UUID userId);

    long deleteAllByUserId(UUID userId);
}
