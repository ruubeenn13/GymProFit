package com.gymprofit.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// RepositoriosNoExportadosTest — ningún repositorio se publica como recurso REST (GP-186)
//
// La API se sirve con controladores propios, cada uno con sus reglas de acceso; Spring
// Data REST está en el classpath solo por su resolvedor de excepciones (WebConfig). Un
// repositorio publicado tendría rutas que no pasan por ningún servicio. Este test recorre
// TODOS los repositorios del contexto, sin nombrar ninguno: si mañana se añade uno que se
// publique, falla.
// ============================================================
@DisplayName("GP-186 — ningún repositorio se publica como recurso REST")
class RepositoriosNoExportadosTest extends AbstractOwnershipTest {

    @Autowired
    private ApplicationContext contexto;

    @Autowired
    private ResourceMappings mapeos;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping controladores;

    @Test
    @DisplayName("ningún repositorio está exportado")
    void ninguno_exportado() {
        List<String> exportados = new ArrayList<>();
        for (Class<?> dominio : new Repositories(contexto)) {
            ResourceMetadata m = mapeos.getMetadataFor(dominio);
            if (m != null && m.isExported()) exportados.add(dominio.getSimpleName() + " → " + m.getPath());
        }
        assertThat(exportados).isEmpty();
        assertThat(new Repositories(contexto)).isNotEmpty();
    }

    @Test
    @DisplayName("la ruta que tendría cada repositorio da 404 a un USER y a un invitado, en lectura y en escritura")
    void sus_rutas_dan_404() throws Exception {
        List<String> malas = new ArrayList<>();
        int probadas = 0;
        for (Class<?> dominio : new Repositories(contexto)) {
            ResourceMetadata m = mapeos.getMetadataFor(dominio);
            if (m == null) continue;
            String ruta = m.getPath().toString();
            // Si un controlador propio atiende esa ruta («/usuarios», «/comidas»), no es la del
            // repositorio: sus reglas las prueban los tests de ese controlador.
            if (deUnControlador(ruta)) continue;
            probadas++;
            for (String verbo : new String[]{"GET", "POST"}) {
                for (var quien : List.of(owner, guest)) {
                    int estado = estado(quien, verbo + " " + ruta);
                    if (estado != 404) malas.add(verbo + " " + ruta + " como " + quien.getUsername() + ": " + estado);
                }
            }
            for (var quien : List.of(owner, guest)) {
                for (String verbo : new String[]{"GET", "PUT", "DELETE"}) {
                    int estado = estado(quien, verbo + " " + ruta + "/1");
                    if (estado != 404) malas.add(verbo + " " + ruta + "/1 como " + quien.getUsername() + ": " + estado);
                }
            }
        }
        assertThat(probadas).isPositive();
        assertThat(malas).isEmpty();
    }

    @Test
    @DisplayName("la raíz de la API no enumera repositorios")
    void la_raiz_no_enumera() throws Exception {
        String cuerpo = pedir(guest, "GET /").andReturn().getResponse().getContentAsString();
        for (Class<?> dominio : new Repositories(contexto)) {
            ResourceMetadata m = mapeos.getMetadataFor(dominio);
            if (m != null) assertThat(cuerpo).doesNotContain("\"" + m.getRel().value() + "\"");
        }
    }

    private boolean deUnControlador(String ruta) throws Exception {
        for (String verbo : new String[]{"GET", "POST"}) {
            MockHttpServletRequest peticion = new MockHttpServletRequest(verbo, ruta);
            try {
                HandlerExecutionChain h = controladores.getHandler(peticion);
                if (h != null) return true;
            } catch (Exception otroVerbo) {
                // La ruta existe en un controlador, con otros verbos.
                return true;
            }
        }
        return false;
    }
}
