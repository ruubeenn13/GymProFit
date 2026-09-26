package com.gymprofit.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

// ============================================================
// WebConfig — negociación de contenido (GP-075)
//
// Una petición sin Accept admite cualquier tipo, y los errores salían en XML porque
// hay un conversor de XML en el classpath (jackson-dataformat-xml llega con el SDK de
// Firebase). Ningún cliente de la API lee XML: sin Accept, la respuesta es JSON. Quien
// quiera otro tipo lo sigue pidiendo con Accept.
//
// Hace falta en dos sitios porque Spring Data REST registra su PROPIO resolvedor de
// excepciones delante del de Spring MVC, con una negociación que solo mira la
// cabecera; y es ese el que ejecuta ControllerExceptionHandler. Sin el segundo
// ajuste, las respuestas normales salen en JSON y los errores siguen en XML.
// ============================================================
@Configuration
public class WebConfig implements WebMvcConfigurer, RepositoryRestConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    public void configureExceptionHandlerExceptionResolver(ExceptionHandlerExceptionResolver resolver) {
        resolver.setContentNegotiationManager(new ContentNegotiationManager(
                new HeaderContentNegotiationStrategy(),
                new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON)));
    }
}
