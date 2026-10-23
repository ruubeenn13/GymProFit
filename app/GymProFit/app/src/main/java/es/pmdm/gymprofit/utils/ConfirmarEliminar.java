package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

// ============================================================
// ConfirmarEliminar — cuándo se puede borrar la cuenta (GP-216, tablero 21).
//
// El borrado es inmediato y no se deshace (DEC-031), así que el botón empieza apagado y lo
// enciende marcar «Entiendo que no se puede deshacer». La contraseña se pide aparte: con
// ella vacía, pulsar no borra, la pide. Sin dependencias de Android, para probarlo en la JVM.
// ============================================================
public final class ConfirmarEliminar {

    /** Qué hace pulsar «Eliminar mi cuenta». */
    public enum Paso { NADA, PEDIR_CLAVE, BORRAR }

    private ConfirmarEliminar() { }

    /** El botón se ve encendido: casilla marcada y sin un borrado en marcha. */
    public static boolean botonHabilitado(boolean casilla, boolean borrando) {
        return casilla && !borrando;
    }

    /**
     * Qué hacer al pulsar.
     *
     * @param casilla  la casilla está marcada.
     * @param password lo escrito en el campo de la contraseña, o null.
     */
    public static Paso alPulsar(boolean casilla, @Nullable String password) {
        if (!casilla) return Paso.NADA;
        if (password == null || password.trim().isEmpty()) return Paso.PEDIR_CLAVE;
        return Paso.BORRAR;
    }
}
