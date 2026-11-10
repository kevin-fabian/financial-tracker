package com.fabiankevin.app.clients;

import com.fabiankevin.app.clients.dtos.EntitlementClientResponse;

import java.util.UUID;

public interface UserEntitlementClient {

    EntitlementClientResponse grantPartnershipEntitlement(UUID userId);

    void revokeEntitlement(UUID userId, String entitlement);
}
