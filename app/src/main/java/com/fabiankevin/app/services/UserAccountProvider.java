package com.fabiankevin.app.services;

import java.util.Set;
import java.util.UUID;

public interface UserAccountProvider {
    void provide(Set<String> accountInterests, UUID userId);
}
