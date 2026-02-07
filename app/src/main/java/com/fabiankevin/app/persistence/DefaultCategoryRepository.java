package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.services.queries.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class DefaultCategoryRepository implements CategoryRepository {
    private final JpaCategoryRepository jpaCategoryRepository;

    @Override
    public Optional<Category> findByIdAndUserId(UUID id, UUID userId) {
        return jpaCategoryRepository.findByIdAndUserId(id, userId)
                .map(CategoryEntity::toModel);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return jpaCategoryRepository.findById(id)
                .map(CategoryEntity::toModel);
    }

    @Override
    public boolean existsByNameAndUserId(String name, UUID userId) {
        return jpaCategoryRepository.existsByNameAndUserId(name, userId);
    }

    @Override
    public Category save(Category category) {
        CategoryEntity saved = jpaCategoryRepository.save(CategoryEntity.from(category));
        return saved.toModel();
    }

    @Override
    public int deleteByIdAndUserId(UUID id, UUID userId) {
        return jpaCategoryRepository.deleteByIdAndUserId(id, userId);
    }

    @Override
    public Page<Category> findAllByPageQuery(PageQuery query, UUID userId) {
        var pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.fromString(query.direction()), query.sort())
        );
        var page = jpaCategoryRepository.findAllByUserId(userId, pageable)
                .map(CategoryEntity::toModel);
        return  new Page<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }
}
