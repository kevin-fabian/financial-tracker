package com.fabiankevin.app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ResourceServerConfig {
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String USER_ROLE = ROLE_PREFIX + "USER";
//    private final InvalidJwtAuthenticationEntryPoint invalidJwtAuthenticationEntryPoint;
//    private final BearerAccessDeniedHandler bearerAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)  {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers( "/api/categories","/api/categories/**").hasAnyAuthority(USER_ROLE)
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(
                        oauth2 -> oauth2
                                .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }


    @Bean
    public RoleHierarchyImpl roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_USER");
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            List<String> scopes = List.of(jwt.getClaimAsString("scope").split(" "));
            List<GrantedAuthority> authorities = new ArrayList<>(scopes.stream()
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .toList());

            Optional.ofNullable(jwt.getClaimAsStringList("roles"))
                    .ifPresent(roles -> authorities.addAll(
                            roles.stream().map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role)).toList()
                    ));

            return authorities;
        });
        return converter;
    }
}
