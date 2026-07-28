package com.fabiankevin.app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
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
public class Oauth2ResourceServerConfig {
    private static final String USER_ROLE = "USER";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           InvalidTokenAuthenticationEntryPoint invalidTokenAuthenticationEntryPoint,
                                           BearerAccessDeniedHandler bearerAccessDeniedHandler) {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/users")
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/accounts", "/api/accounts/**",
                                "/api/categories", "/api/categories/**",
                                "/api/stats", "/api/stats*",
                                "/api/parties", "/api/party/**",
                                "/api/budgets", "/api/budgets/**").hasAnyAuthority(USER_ROLE)
                        .requestMatchers("/api/recurring-transactions/process-due").hasAnyAuthority("zeny:operator")
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasAnyAuthority("user:provision")
                        .anyRequest().authenticated()
                )
                .cors(Customizer.withDefaults())
                .oauth2ResourceServer(
                        oauth2 -> oauth2
                                .accessDeniedHandler(bearerAccessDeniedHandler)
                                .authenticationEntryPoint(invalidTokenAuthenticationEntryPoint)
                                .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }


    @Bean
    public RoleHierarchyImpl roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ADMIN > USER");
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            List<String> scopes = jwt.getClaimAsStringList("scope");
            List<GrantedAuthority> authorities = new ArrayList<>(scopes.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList());

            Optional.ofNullable(jwt.getClaimAsStringList("roles"))
                    .ifPresent(roles -> authorities.addAll(
                            roles.stream().map(SimpleGrantedAuthority::new).toList()
                    ));

            return authorities;
        });
        return converter;
    }
}
