package com.bookease.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI bookEaseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BookEase API")
                        .version("v1")
                        .description("""
                                BookEase service discovery and appointment booking backend.

                                Every operation is exposed as an independent, deterministic REST call so it can
                                later be wrapped as an Amazon Bedrock AgentCore tool. Authentication is JWT
                                bearer based: obtain a token from POST /api/v1/auth/login and send it as
                                `Authorization: Bearer <token>`.

                                Identity always comes from the token, never from a client-supplied user id.""")
                        .contact(new Contact().name("BookEase Platform")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token issued by POST /api/v1/auth/login")));
    }
}
