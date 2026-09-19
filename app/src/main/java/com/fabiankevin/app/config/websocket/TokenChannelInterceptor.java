package com.fabiankevin.app.config.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenChannelInterceptor implements ChannelInterceptor {
    private final JwtDecoder jwtDecoder; // Spring Boot's default JWT Decoder

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authorization = accessor.getNativeHeader("Authorization");
            
            if (authorization != null && !authorization.isEmpty()) {
                String bearerToken = authorization.getFirst();
                if (bearerToken.startsWith("Bearer ")) {
                    String token = bearerToken.substring(7);
                    try {
                        // Decode and validate the token
                        Jwt jwt = jwtDecoder.decode(token);

                        String userId = jwt.getSubject();

                        // Authenticate the WebSocket Session
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userId, null,  Collections.emptyList());
                            
                        accessor.setUser(authentication);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid JWT token", e);
                    }
                }
            } else {
                throw new IllegalArgumentException("No Authorization header found");
            }
        }
        return message;
    }
}