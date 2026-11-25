package com.gymprofit.api.service.notificacion;

import com.gymprofit.api.repository.jpa.INotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ============================================================
// VigiaProgramadasTest — el barrido de programadas solo va a la base cuando toca (GP-283).
// Reloj fijo que se adelanta a mano: cada «minuto» es una llamada a comprobar().
// ============================================================
@DisplayName("VigiaProgramadas — sin programadas, la base no se toca cada minuto")
class VigiaProgramadasTest {

    private static final ZoneId ZONA = ZoneOffset.UTC;

    private final INotificacionRepository repositorio = mock(INotificacionRepository.class);
    private final NotificacionProgramadaTask barrido = mock(NotificacionProgramadaTask.class);
    private final ProximaProgramada proxima = new ProximaProgramada();
    private RelojMovil reloj;
    private VigiaProgramadas vigia;

    @BeforeEach
    void preparar() {
        reloj = new RelojMovil(Instant.parse("2026-11-25T10:00:00Z"));
        vigia = new VigiaProgramadas(barrido, repositorio, proxima, reloj);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    @Test
    @DisplayName("sin pendientes: una lectura al arrancar y otra cada 15 min, ningún barrido")
    void sinPendientes() {
        for (int minuto = 0; minuto < 60; minuto++) {
            vigia.comprobar();
            reloj.avanzar(Duration.ofMinutes(1));
        }
        verify(repositorio, times(4)).proximaProgramadaPendiente(); // min 0, 15, 30 y 45
        verify(barrido, never()).enviarProgramadasVencidas();
    }

    @Test
    @DisplayName("una creada por la API sale en el minuto en que vence, sin esperar a la relectura")
    void avisoSaleEnSuMinuto() {
        vigia.comprobar(); // lee: nada
        proxima.avisar(ahora().plusMinutes(3)); // fuera de transacción: se anota ya

        reloj.avanzar(Duration.ofMinutes(1));
        vigia.comprobar();
        reloj.avanzar(Duration.ofMinutes(1));
        vigia.comprobar();
        verify(barrido, never()).enviarProgramadasVencidas();

        reloj.avanzar(Duration.ofMinutes(1));
        vigia.comprobar();
        verify(barrido, times(1)).enviarProgramadasVencidas();
    }

    @Test
    @DisplayName("tras barrer, la hora siguiente sale de la base")
    void trasBarrerRelee() {
        when(repositorio.proximaProgramadaPendiente()).thenReturn(ahora().minusMinutes(1), ahora().plusMinutes(2));
        vigia.comprobar(); // lee: vencida → barre → relee: dentro de 2 min
        verify(barrido, times(1)).enviarProgramadasVencidas();
        assertThat(proxima.proxima()).isEqualTo(ahora().plusMinutes(2));
    }

    @Test
    @DisplayName("lo escrito por otro camino se ve en la relectura de los 15 min")
    void otroCaminoEnLaRelectura() {
        vigia.comprobar(); // nada
        reloj.avanzar(Duration.ofMinutes(15));
        when(repositorio.proximaProgramadaPendiente()).thenReturn(ahora().plusMinutes(5));
        vigia.comprobar();
        assertThat(proxima.proxima()).isEqualTo(LocalDateTime.of(2026, 11, 25, 10, 20));
    }

    @Test
    @DisplayName("un aviso que llega mientras se lee la base no se pierde")
    void avisoDuranteLaLectura() {
        LocalDateTime pronto = ahora().plusMinutes(1);
        when(repositorio.proximaProgramadaPendiente()).thenAnswer(i -> {
            proxima.avisar(pronto); // llega a mitad de la lectura; la base aún no la ve
            return null;
        });
        vigia.comprobar();
        assertThat(proxima.proxima()).isEqualTo(pronto);
    }

    /** Reloj que solo avanza cuando el test lo dice. */
    private static final class RelojMovil extends Clock {
        private Instant instante;

        RelojMovil(Instant inicio) {
            this.instante = inicio;
        }

        void avanzar(Duration d) {
            instante = instante.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZONA;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
