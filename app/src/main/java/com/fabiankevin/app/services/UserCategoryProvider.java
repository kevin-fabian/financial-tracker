package com.fabiankevin.app.services;

import java.util.Set;
import java.util.UUID;

public interface UserCategoryProvider {
    void provide(Set<String> categoryInterests, UUID userId);
}
