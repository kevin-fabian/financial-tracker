package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class DefaultCategoryRepository implements CategoryRepository {
    private final JpaCategoryRepository jpaCategoryRepository;

    @Override
    public Optional<Category> findById(UUID id) {
        return jpaCategoryRepository.findById(id)
                .map(CategoryEntity::toModel);
    }

    @Override
    public Category save(Category category) {
        CategoryEntity saved = jpaCategoryRepository.save(CategoryEntity.from(category));
        return saved.toModel();
    }

    @Override
    public void deleteById(UUID id) {
        jpaCategoryRepository.deleteById(id);
    }
}
