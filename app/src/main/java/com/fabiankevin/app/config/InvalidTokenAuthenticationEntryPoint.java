package com.fabiankevin.app.config;

import com.github.fabiankevin.lemon.web.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class InvalidTokenAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final JsonMapper jsonMapper;
    private static final String DEFAULT_UNAUTHORIZED_TITLE = "Unauthorized";
    private static final String DEFAULT_UNAUTHORIZED_DETAILS = "Invalid or expired token";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        String description = authException != null && authException.getMessage() != null ? authException.getMessage() : DEFAULT_UNAUTHORIZED_TITLE;
        // RFC 6750 header for 401
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
                "Bearer error=\"invalid_token\", error_description=\"" + sanitize(description) + "\"");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .title(DEFAULT_UNAUTHORIZED_TITLE)
                .details(DEFAULT_UNAUTHORIZED_DETAILS)
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .build();

        jsonMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private String sanitize(String input) {
        return input.replace("\"", "'");
    }
}
