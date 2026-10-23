package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

// ============================================================
// FraseProfi — qué dice Profi en el bocadillo de Inicio (GP-216, tablero 13).
//
// Una frase con un dato tuyo que sea verdad, no una de adorno: los récords de la última vez,
// lo que llevas esta semana, la proteína que te falta o lo que toca hoy. De más a menos
// específica; si no hay nada que decir, no hay bocadillo (NINGUNA). Aquí solo se elige y se
// reúnen los datos; el texto, en ES y EN, lo pone la pantalla con cada tipo. Sin dependencias de
// Android, para probarla en la JVM.
// ============================================================
public final class FraseProfi {

    /** Qué frase toca. */
    public enum Tipo { SIN_ENTRENOS, ENTRENADO_HOY, RECORDS_ULTIMA, SEMANA, PROTEINA, HOY_TOCA, NINGUNA }

    /** Gramos de proteína que faltan para que se mencionen: los mismos que «Para recuperar» (DEC-055). */
    public static final int PROTEINA_MINIMA = 20;

    /** Lo que se sabe para elegir. Todo opcional: lo que no llegó no cuenta. */
    public static final class Datos {
        public boolean hayEntrenos;
        public boolean entrenadoHoy;
        @Nullable public String ultimaRutina;
        public int recordsUltima;
        @Nullable public String rutinaHoy;
        @Nullable public String primerEjercicioHoy;
        public int sesionesSemana;
        public int proteinaFalta;
    }

    public final Tipo tipo;
    /** La rutina de la última sesión. */
    @Nullable public final String rutina;
    /** El número que acompaña: récords, sesiones o gramos. */
    public final int n;
    @Nullable public final String rutinaHoy;
    @Nullable public final String ejercicioHoy;

    private FraseProfi(Tipo tipo, @Nullable String rutina, int n, @Nullable String rutinaHoy,
                       @Nullable String ejercicioHoy) {
        this.tipo = tipo;
        this.rutina = rutina;
        this.n = n;
        this.rutinaHoy = rutinaHoy;
        this.ejercicioHoy = ejercicioHoy;
    }

    /** Elige la frase para esos datos. */
    public static FraseProfi elegir(Datos d) {
        if (!d.hayEntrenos) return new FraseProfi(Tipo.SIN_ENTRENOS, null, 0, d.rutinaHoy, d.primerEjercicioHoy);
        if (d.entrenadoHoy) {
            return new FraseProfi(Tipo.ENTRENADO_HOY, d.ultimaRutina, d.recordsUltima, null, null);
        }
        if (d.recordsUltima > 0 && d.ultimaRutina != null) {
            return new FraseProfi(Tipo.RECORDS_ULTIMA, d.ultimaRutina, d.recordsUltima, d.rutinaHoy,
                    d.primerEjercicioHoy);
        }
        if (d.sesionesSemana > 0) {
            return new FraseProfi(Tipo.SEMANA, d.ultimaRutina, d.sesionesSemana, d.rutinaHoy, d.primerEjercicioHoy);
        }
        if (d.proteinaFalta >= PROTEINA_MINIMA) {
            return new FraseProfi(Tipo.PROTEINA, null, d.proteinaFalta, d.rutinaHoy, d.primerEjercicioHoy);
        }
        if (d.rutinaHoy != null) {
            return new FraseProfi(Tipo.HOY_TOCA, null, 0, d.rutinaHoy, d.primerEjercicioHoy);
        }
        return new FraseProfi(Tipo.NINGUNA, null, 0, null, null);
    }
}
