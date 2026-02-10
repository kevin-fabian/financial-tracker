package com.fabiankevin.app.config;

import com.github.fabiankevin.lemon.web.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class BearerAccessDeniedHandler implements AccessDeniedHandler {
    private static final String DEFAULT_FORBIDDEN_TITLE = "Forbidden";
    private static final String DEFAULT_FORBIDDEN_DETAILS = "Insufficient scope";
    private final JsonMapper jsonMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
        // RFC 6750 header for 403
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"insufficient_scope\"");

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse body = ApiErrorResponse.builder()
                .title(DEFAULT_FORBIDDEN_TITLE)
                .details(DEFAULT_FORBIDDEN_DETAILS)
                .status(HttpServletResponse.SC_FORBIDDEN)
                .build();

        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}