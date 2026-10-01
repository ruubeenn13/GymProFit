package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

// ============================================================
// AvisosCuenta — los tres interruptores de avisos de la cuenta (GP-112, DEC-037).
//
// Entrenar (si pasan tres días sin entrenar), comidas (a su hora, si no se ha apuntado)
// y progreso (logros, el resumen del domingo y pesarse cada mes). Viven en la cuenta y
// se cambian con el PATCH del perfil; los de serie son los de la API: entrenar sí,
// comidas no, progreso sí. Los usan el alta («¿Te avisamos?») y Ajustes › Notificaciones.
//
// El fin del descanso no es uno de ellos: lo avisa la propia app con la sesión en curso.
// ============================================================
public final class AvisosCuenta {

    private AvisosCuenta() {}

    public static final boolean ENTRENAR_DE_SERIE = true;
    public static final boolean COMIDAS_DE_SERIE = false;
    public static final boolean PROGRESO_DE_SERIE = true;

    /** Los tres, como los guarda la cuenta. */
    public static final class Estado {
        public final boolean entrenar, comidas, progreso;

        public Estado(boolean entrenar, boolean comidas, boolean progreso) {
            this.entrenar = entrenar;
            this.comidas = comidas;
            this.progreso = progreso;
        }

        /** Los de serie, para una cuenta nueva o una API que no los manda. */
        @NonNull
        public static Estado deSerie() {
            return new Estado(ENTRENAR_DE_SERIE, COMIDAS_DE_SERIE, PROGRESO_DE_SERIE);
        }

        /** Los de la cuenta; el que no llegue, el de serie. */
        @NonNull
        public static Estado de(@Nullable Boolean entrenar, @Nullable Boolean comidas, @Nullable Boolean progreso) {
            return new Estado(entrenar != null ? entrenar : ENTRENAR_DE_SERIE,
                    comidas != null ? comidas : COMIDAS_DE_SERIE,
                    progreso != null ? progreso : PROGRESO_DE_SERIE);
        }
    }

    /** El cuerpo del PATCH con los tres. */
    @NonNull
    public static Map<String, Object> cuerpo(@NonNull Estado e) {
        Map<String, Object> body = new HashMap<>();
        body.put("avisosEntrenar", e.entrenar);
        body.put("avisosComidas", e.comidas);
        body.put("avisosProgreso", e.progreso);
        return body;
    }
}
