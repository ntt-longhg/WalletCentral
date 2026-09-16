package com.gateway.walletcentral.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                final String securitySchemeName = "X-API-Key";

                return new OpenAPI()
                                .info(new Info()
                                                .title("WalletCentral Gateway API")
                                                .version("1.0.0")
                                                .description("WalletCentral Gateway API - Manage tenants, wallets, services, pricing, transactions, and invoices.")
                                                .contact(new Contact()
                                                                .name("WalletCentralGateway Team")
                                                                .email("dev.ntt@dntg.com.vn")))
                                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                                .components(new Components()
                                                .addSecuritySchemes(securitySchemeName,
                                                                new SecurityScheme()
                                                                                .name(securitySchemeName)
                                                                                .type(SecurityScheme.Type.APIKEY)
                                                                                .in(SecurityScheme.In.HEADER)
                                                                                .description("API Key for authentication")));
        }
}
