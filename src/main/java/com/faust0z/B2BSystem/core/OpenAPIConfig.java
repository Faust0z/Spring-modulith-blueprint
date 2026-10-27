package com.faust0z.B2BSystem.core;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenAPIConfig {

    @Bean
    public OpenAPI myOpenAPI() {
        SecurityScheme tenantHeaderScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-Tenant-ID")
                .description("Solo para Administradores Globales: UUID del negocio que deseas inspeccionar.");

        Info info = new Info()
                .title("B2BSystem")
                .version("1.0")
                .description("""
                        <h2>A SpringBoot API to manage multiple tenants.</h2>
                        <br>
                        
                        <h2>🚀 To use: Execute the <code>/auth/login</code> endpoint to receive a valid access token.</h2>
                        
                        <br>
                        
                        <h3>(Admins Only)</b> Paste the UUID of a business in the X-Tenant-ID field of the <code>Authorize 🔓</code> button
                        to act inside that business and see only their data.</h3></li>
                        
                        <br>
                        <br>
                        """)
                .contact(new Contact().name("Faust0z").email("fausto.z77@hotmail.com"));

        return new OpenAPI()
                .info(info)
                .addSecurityItem(new SecurityRequirement().addList("tenantHeader"))
                .components(new Components().addSecuritySchemes("tenantHeader", tenantHeaderScheme));
    }
}
