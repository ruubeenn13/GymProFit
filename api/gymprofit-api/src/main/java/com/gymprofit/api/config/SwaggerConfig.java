package com.gymprofit.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// ============================================================
// SwaggerConfig — configuración de la documentación OpenAPI/Swagger
// Define los metadatos generales de la API (título, versión, descripción)
// y el esquema de seguridad Bearer JWT que Swagger UI usa para autenticar
// las peticiones de prueba contra los endpoints de GymProFit.
// ============================================================
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "GymProFit API",
                version = "1.0",
                description = "API REST para la gestión de gimnasio"
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Token JWT de autenticación",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class SwaggerConfig {
}
