//package com.fabiankevin.app.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.github.fabiankevin.quickstart.web.dto.ApiErrorResponse;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.web.access.AccessDeniedHandler;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.time.Instant;
//
//@Component
//@RequiredArgsConstructor
//public class BearerAccessDeniedHandler implements AccessDeniedHandler {
//    private final ObjectMapper objectMapper;
//
//    @Override
//    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
//        // RFC 6750 header for 403
//        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"insufficient_scope\"");
//
//        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
//        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//
//        ApiErrorResponse body = new ApiErrorResponse();
//        body.setMessage("Forbidden");
//        body.setDetails("Insufficient scope");
//        body.setTimestamp(Instant.now());
//
//        objectMapper.writeValue(response.getOutputStream(), body);
//    }
//}