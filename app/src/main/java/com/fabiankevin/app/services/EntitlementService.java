package com.fabiankevin.app.services;

import java.util.UUID;

public interface EntitlementService {

    void grant(UUID userId);

    void revoke(UUID userId);
}
