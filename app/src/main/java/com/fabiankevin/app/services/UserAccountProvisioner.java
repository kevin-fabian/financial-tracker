package com.fabiankevin.app.services;

import java.util.Set;
import java.util.UUID;

public interface UserAccountProvisioner {
    void provision(Set<String> accountInterests, UUID userId);
}
