package com.gymprofit.api.integration;

import io.sentry.IScopes;
import io.sentry.Sentry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ============================================================
// SentrySinDsnTest — sin SENTRY_DSN, Sentry no se enciende y un 500 no sale a ningún
// sitio (GP-221). Es lo que pasa en local y en el CI. Se ha corrido también con
// SENTRY_DSN= (vacía) en el entorno, que es lo que dejaría Render si no se rellena.
//
// ServidorSentryFalso escucha igualmente: si algo intentara mandar, llegaría ahí.
// ============================================================
@DisplayName("Sentry sin DSN — no se inicializa y no sale nada")
@Import(SentryConDsnTest.ErroresDePrueba.class)
class SentrySinDsnTest extends AbstractOwnershipTest {

    @Autowired
    private ApplicationContext contexto;

    @Test
    @DisplayName("ni bean de Sentry, ni SDK encendido, ni avisos con un 500")
    void sin_dsn_no_sale_nada() throws Exception {
        // Sin la variable no hay propiedad ni beans. Con la variable vacía (un sync:false de
        // Render sin rellenar) la autoconfiguración sí arranca, pero el SDK se queda apagado.
        String dsn = contexto.getEnvironment().getProperty("sentry.dsn");
        assertThat(dsn).isNullOrEmpty();
        if (dsn == null) assertThat(contexto.getBeanNamesForType(IScopes.class)).isEmpty();
        assertThat(Sentry.isEnabled()).isFalse();

        ServidorSentryFalso.vaciar();
        mockMvc.perform(post("/prueba-sentry/500")
                        .header("Authorization", "Bearer " + token(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + SentryConDsnTest.CORREO + "\"}"))
                .andExpect(status().isInternalServerError());
        Sentry.flush(2000);

        assertThat(ServidorSentryFalso.recibidos()).isEmpty();
    }
}
