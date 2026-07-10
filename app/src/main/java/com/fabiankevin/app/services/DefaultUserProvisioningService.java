package com.fabiankevin.app.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultUserProvisioningService implements UserProvisioningService {
    private final UserAccountProvisioner accountProvider;
    private final UserCategoryProvisioner categoryProvider;

    @Override
    public void provisionUser(UUID userId, Set<String> accountInterests, Set<String> categoryInterests) {
        accountProvider.provision(accountInterests, userId);
        categoryProvider.provision(categoryInterests, userId);
    }
}
