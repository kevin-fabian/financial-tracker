package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserEntitlementClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultEntitlementServiceTest {

    @Mock
    private UserEntitlementClient userEntitlementClient;

    @InjectMocks
    private DefaultEntitlementService service;

    @Test
    void grant_delegatesToClientWithUserId() {
        UUID userId = UUID.randomUUID();

        service.grant(userId);

        verify(userEntitlementClient, times(1)).grantPartnershipEntitlement(userId);
    }

    @Test
    void revoke_delegatesToClientWithUserIdAndPartnership() {
        UUID userId = UUID.randomUUID();

        service.revoke(userId);

        verify(userEntitlementClient, times(1)).revokeEntitlement(userId, "PARTNERSHIP");
    }
}
