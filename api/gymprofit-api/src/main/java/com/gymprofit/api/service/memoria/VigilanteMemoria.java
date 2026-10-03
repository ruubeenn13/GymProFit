package com.gymprofit.api.service.memoria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// ============================================================
// VigilanteMemoria — la línea de memoria en el log (GP-187, lote 1.6.5)
//
// Para que la próxima vez que Render mate el contenedor el log diga cómo iba:
//   · Una línea INFO al arrancar y cada 10 minutos (MemoriaProceso.Lectura.linea).
//   · Cada 30 s se mira el contenedor y, si pasa del 90 % de su límite, la misma
//     línea en WARN. Una vez por subida: no vuelve a avisar hasta que baje del 85 %.
//   · Sin cgroup (en local), lo dice una vez al arrancar y sigue con lo de la JVM.
// Leer son unos pocos ficheros de /sys y los MXBeans: no cuesta nada.
// ============================================================
@Component
public class VigilanteMemoria {

    /** Por encima de esto, WARN. */
    static final int AVISO = 90;
    /** Por debajo de esto se rearma el aviso. */
    static final int REARME = 85;

    private static final Logger logger = LoggerFactory.getLogger(VigilanteMemoria.class);

    private final MemoriaProceso memoria;
    private boolean avisado;

    public VigilanteMemoria() {
        this(new MemoriaProceso());
    }

    VigilanteMemoria(MemoriaProceso memoria) {
        this.memoria = memoria;
    }

    /** Al arrancar: la primera línea y, si no hay cgroup, que se sepa. */
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        MemoriaProceso.Lectura l = memoria.leer();
        if (l.contenedor().isEmpty()) {
            logger.info("Memoria: sin cgroup de memoria que leer (normal fuera de un contenedor); solo la JVM");
        }
        logger.info(l.linea());
    }

    /** Cada 10 minutos, la línea. */
    @Scheduled(initialDelay = 600_000, fixedDelay = 600_000)
    public void cadaDiezMinutos() {
        logger.info(memoria.leer().linea());
    }

    /** Cada 30 s, el WARN si el contenedor pasa del 90 %. */
    @Scheduled(initialDelay = 30_000, fixedDelay = 30_000)
    public void vigilar() {
        comprobar();
    }

    /**
     * @return true si ha avisado ahora.
     */
    boolean comprobar() {
        var c = memoria.contenedor();
        if (c.isEmpty() || c.get().limite() <= 0) return false;
        int pct = c.get().porcentaje();
        if (pct < REARME) avisado = false;
        if (pct > AVISO && !avisado) {
            avisado = true;
            logger.warn("{} · más del {} % del límite del contenedor", memoria.leer().linea(), AVISO);
            return true;
        }
        return false;
    }
}
