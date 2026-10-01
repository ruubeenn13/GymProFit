package com.gymprofit.api.service.busqueda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// ============================================================
// CalentadorBusqueda — la primera búsqueda de verdad no paga lo frío (GP-168)
// Cuando el índice de productos está listo (al arrancar y tras cada importación),
// lanza unas cuantas búsquedas habituales por el camino entero. Con 0,1 de CPU, la
// primera búsqueda de un servicio recién desplegado tardaba cientos de ms más que las
// siguientes: el JIT y la caché de planes de Hibernate estaban fríos. Corre en el hilo
// que construye el índice, en segundo plano; si falla, solo se pierde el calentamiento.
// ============================================================
@Component
public class CalentadorBusqueda {

    private static final Logger logger = LoggerFactory.getLogger(CalentadorBusqueda.class);

    private final BusquedaAlimentosService busqueda;

    public CalentadorBusqueda(BusquedaAlimentosService busqueda) {
        this.busqueda = busqueda;
    }

    @EventListener
    void alIndexar(IndiceAlimentos.ProductosIndexados evento) {
        long inicio = System.nanoTime();
        try {
            busqueda.calentar();
            logger.info("Búsqueda calentada en {} ms", (System.nanoTime() - inicio) / 1_000_000);
        } catch (RuntimeException e) {
            // No impide buscar: solo la primera búsqueda real será más lenta.
            logger.warn("No se pudo calentar la búsqueda", e);
        }
    }
}
