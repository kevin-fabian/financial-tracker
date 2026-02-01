package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Category;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    Optional<Category> findById(UUID id);
}
