package com.gymprofit.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// ============================================================
// WebConfig — negociación de contenido (GP-075)
//
// Una petición sin Accept admite cualquier tipo, y los errores salían en XML porque
// hay un conversor de XML en el classpath (jackson-dataformat-xml llega con el SDK de
// Firebase). Ningún cliente de la API lee XML: sin Accept, la respuesta es JSON. Quien
// quiera otro tipo lo sigue pidiendo con Accept.
//
// Vale también para los errores: ControllerExceptionHandler lo ejecuta el resolvedor de
// excepciones de Spring MVC, que usa esta misma negociación. Hasta la 1.6.5 hacía falta un
// segundo ajuste para el resolvedor propio de Spring Data REST, que ya no está (DEC-046).
// ============================================================
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }
}
