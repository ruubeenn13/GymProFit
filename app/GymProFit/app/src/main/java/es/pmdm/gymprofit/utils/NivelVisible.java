package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// NivelVisible — el nivel en tres: Experto se enseña como Avanzado (GP-103, lote 1.5.1).
//
// La API conserva los cuatro niveles y los programas ya trataban Experto como Avanzado
// (decisión 3 del lienzo del alta). La app deja de ofrecerlo y, donde lo encuentra
// guardado, lo enseña como Avanzado SIN cambiar lo guardado: Editar perfil solo manda
// AVANZADO si la persona elige otro nivel y vuelve.
//
// La administración sí distingue los cuatro, porque escribe el valor exacto.
// ============================================================
public final class NivelVisible {

    private NivelVisible() {}

    /** Los tres que se ofrecen. */
    public static final String[] OFRECIDOS = {"PRINCIPIANTE", "INTERMEDIO", "AVANZADO"};

    /** El nivel tal como se enseña: EXPERTO pasa a AVANZADO; sin nivel, "". */
    @NonNull
    public static String valor(@Nullable String nivel) {
        if (nivel == null) return "";
        return "EXPERTO".equalsIgnoreCase(nivel) ? "AVANZADO" : nivel.toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * Lo que hay que mandar al guardar desde una lista de tres.
     *
     * @param elegido  el que está elegido en la lista (de OFRECIDOS).
     * @param guardado el que tenía la cuenta.
     * @return el guardado si es Experto y se ha dejado en Avanzado; si no, el elegido.
     */
    @NonNull
    public static String aGuardar(@NonNull String elegido, @Nullable String guardado) {
        if ("AVANZADO".equals(elegido) && "EXPERTO".equalsIgnoreCase(guardado)) return "EXPERTO";
        return elegido;
    }
}
