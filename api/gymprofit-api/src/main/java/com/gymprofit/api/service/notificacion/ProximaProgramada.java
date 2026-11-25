package com.gymprofit.api.service.notificacion;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

// ============================================================
// ProximaProgramada — cuándo vence la próxima notificación programada, en memoria (GP-283).
//
// El barrido de las programadas corría cada minuto aunque no hubiera ninguna: un bloqueo de
// ShedLock, una consulta y su transacción, 1.440 veces al día, contra una base que cobra por
// sentencia (TiDB Cloud Starter, en RU). Ahora el barrido de cada minuto solo va a la base
// cuando sabe que algo vence: la hora sale de aquí.
//
// Quién la mantiene al día:
//   · NotificacionService, al crear o reprogramar una notificación con fecha, avisa en
//     cuanto la transacción se confirma (antes, el barrido podría no verla todavía). Así
//     una notificación creada por la API sale en el mismo minuto que antes.
//   · VigiaProgramadas la vuelve a leer de la base al arrancar, tras cada barrido y cada
//     SINCRONIZAR, por lo que no haya pasado por la API (otra instancia, un UPDATE a mano).
//
// La única regla delicada: una lectura de la base no puede borrar un aviso que llegó
// mientras se leía. Por eso cada aviso sube un contador y fijar() solo sustituye la hora
// si nadie ha avisado desde que empezó la lectura; si alguien lo ha hecho, se queda la
// más temprana de las dos.
// ============================================================
@Component
public class ProximaProgramada {

    /** null: no hay ninguna pendiente que se sepa. */
    private LocalDateTime proxima;
    private long avisos;

    /**
     * Anota que hay una notificación pendiente para esa hora. Dentro de una transacción,
     * espera a que se confirme; fuera, anota ya.
     *
     * @param fecha la fecha programada; null no hace nada
     */
    public void avisar(LocalDateTime fecha) {
        if (fecha == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    anotar(fecha);
                }
            });
        } else {
            anotar(fecha);
        }
    }

    synchronized void anotar(LocalDateTime fecha) {
        avisos++;
        if (proxima == null || fecha.isBefore(proxima)) proxima = fecha;
    }

    /** @return la marca que hay que pasar a {@link #fijar} al terminar de leer la base. */
    synchronized long empezarLectura() {
        return avisos;
    }

    /**
     * Sustituye la hora por la que dice la base, salvo que haya llegado un aviso durante
     * la lectura: entonces se queda la más temprana.
     *
     * @param desdeBase la más temprana pendiente en la base, o null si no hay
     * @param marca     lo que devolvió {@link #empezarLectura} antes de leer
     */
    synchronized void fijar(LocalDateTime desdeBase, long marca) {
        if (avisos == marca || proxima == null) {
            proxima = desdeBase;
        } else if (desdeBase != null && desdeBase.isBefore(proxima)) {
            proxima = desdeBase;
        }
    }

    /**
     * @param ahora la hora actual
     * @return true si hay alguna pendiente que ya ha vencido
     */
    synchronized boolean vence(LocalDateTime ahora) {
        return proxima != null && !proxima.isAfter(ahora);
    }

    /** @return la próxima conocida, o null; para los tests y el log. */
    synchronized LocalDateTime proxima() {
        return proxima;
    }
}
