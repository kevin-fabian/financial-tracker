package com.fabiankevin.app.services;

import java.util.Set;
import java.util.UUID;

public interface UserCategoryProvisioner {
    void provision(Set<String> categoryInterests, UUID userId);
}
