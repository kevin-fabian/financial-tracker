package com.fabiankevin.app.sync_devices.web;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

/**
 * Utility for extracting common claims from JWT authentication tokens
 * used across sync-related controllers.
 */
public final class JwtTokenUtil {

    private JwtTokenUtil() {
        // utility class
    }

    /**
     * JWT claim name for the user device identifier.
     */
    public static final String USER_DEVICE_CLAIM = "userDevice";

    /**
     * Extracts the user ID from the JWT subject claim.
     */
    public static UUID getUserId(JwtAuthenticationToken token) {
        return UUID.fromString(token.getToken().getSubject());
    }

    /**
     * Extracts the user device identifier from the JWT custom claim.
     */
    public static String getUserDevice(JwtAuthenticationToken token) {
        return token.getToken().getClaimAsString(USER_DEVICE_CLAIM);
    }
}
