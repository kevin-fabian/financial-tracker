package com.fabiankevin.app.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(
        info = @Info(title = "${springdoc.api-docs.info.title}",
                version = "${springdoc.api-docs.info.version}",
                description = "${springdoc.api-docs.info.description}")
)
@SecurityScheme(
        name = "Spring Oauth2",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(
                clientCredentials = @OAuthFlow(
                        tokenUrl = "http://localhost:9000/oauth2/token",
                        scopes = {
                                @io.swagger.v3.oas.annotations.security.OAuthScope(name = "user:read", description = "Read access"),
                                @io.swagger.v3.oas.annotations.security.OAuthScope(name = "user:write", description = "Write access"),
                                @io.swagger.v3.oas.annotations.security.OAuthScope(name = "user:manage", description = "Read and write access"),
                        }
                )
        )
)
public class OpenApiConfig {
}