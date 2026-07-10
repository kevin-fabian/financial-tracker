package com.fabiankevin.app.services;

import java.util.Set;
import java.util.UUID;

public interface UserProvisioningService {
    void provisionUser(UUID userId, Set<String> accountInterests, Set<String> categoryInterests);
}
