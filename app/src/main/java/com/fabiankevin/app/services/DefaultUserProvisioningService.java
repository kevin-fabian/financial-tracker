package com.fabiankevin.app.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultUserProvisioningService implements UserProvisioningService {
    private final UserAccountProvisioner accountProvider;
    private final UserCategoryProvisioner categoryProvider;

    @Transactional
    @Override
    public void provisionUser(UUID userId, Set<String> accountInterests, Set<String> categoryInterests) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        accountProvider.provision(accountInterests, userId);
        categoryProvider.provision(categoryInterests, userId);
    }
}
