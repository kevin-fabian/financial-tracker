package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Category;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserCategoryProvider {
    List<Category> provide(Set<String> categoryInterests, UUID userId);
}
