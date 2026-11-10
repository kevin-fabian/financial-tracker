package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserEntitlementClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultEntitlementService implements EntitlementService {
    private static final String PARTNERSHIP_ENTITLEMENT = "PARTNERSHIP";

    private final UserEntitlementClient userEntitlementClient;

    @Override
    public void grant(UUID userId) {
        userEntitlementClient.grantPartnershipEntitlement(userId);
    }

    @Override
    public void revoke(UUID userId) {
        userEntitlementClient.revokeEntitlement(userId, PARTNERSHIP_ENTITLEMENT);
    }
}
