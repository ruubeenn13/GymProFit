package com.gymprofit.api.service.programa;

import com.gymprofit.api.enums.EquipamientoPrograma;
import com.gymprofit.api.enums.NivelExperiencia;

// ============================================================
// RecomendacionPrograma — la tabla del punto 1 del catálogo (GP-074, lote 1.2.1)
//
// | Nivel                 | 2 y 3 días      | 4 días          | 5 días                   | 6 días          |
// |-----------------------|-----------------|-----------------|--------------------------|-----------------|
// | Principiante          | Para empezar    | Para empezar    | Para empezar             | Para empezar    |
// | Intermedio y avanzado | Cuerpo completo | Torso y pierna  | Torso y pierna + E y T   | Empuje, tirón y pierna |
//
// · Principiante con 4 o más días: «Para empezar», a 3, y el porqué.
// · Peso corporal con 5 o 6: Torso y pierna (no hay de más días).
// · Sin nivel en el perfil: como principiante, y que puede ponerlo en Editar perfil.
// · AVANZADO y EXPERTO: como intermedio; al seguirlo se le suben las series.
//
// Lógica pura: da el código del programa y la clave del mensaje del porqué (null si
// es el obvio). El texto lo resuelve el servicio en el idioma de la petición.
// ============================================================
public final class RecomendacionPrograma {

    private RecomendacionPrograma() { }

    /** Código del programa y clave del porqué, o null si no hace falta explicarlo. */
    public record Recomendacion(String codigo, String motivo) { }

    static final String SIN_NIVEL = "programa.recomendado.sinNivel";
    static final String SIN_NIVEL_MAS_DIAS = "programa.recomendado.sinNivelMasDias";
    static final String PRINCIPIANTE_MAS_DIAS = "programa.recomendado.principianteMasDias";
    static final String PESO_CORPORAL_MAS_DIAS = "programa.recomendado.pesoCorporalMasDias";
    static final String AVANZADO = "programa.recomendado.avanzado";
    static final String AVANZADO_PESO_CORPORAL_MAS_DIAS = "programa.recomendado.avanzadoPesoCorporalMasDias";

    /**
     * @param equipamiento dónde entrena.
     * @param dias         de 2 a 6.
     * @param nivel        el del perfil; null si no tiene.
     */
    public static Recomendacion elegir(EquipamientoPrograma equipamiento, int dias, NivelExperiencia nivel) {
        String prefijo = switch (equipamiento) {
            case GIMNASIO -> "GIM";
            case MANCUERNAS -> "MAN";
            case PESO_CORPORAL -> "PC";
        };

        if (nivel == null || nivel == NivelExperiencia.PRINCIPIANTE) {
            String motivo;
            if (nivel == null) motivo = dias >= 4 ? SIN_NIVEL_MAS_DIAS : SIN_NIVEL;
            else motivo = dias >= 4 ? PRINCIPIANTE_MAS_DIAS : null;
            return new Recomendacion(prefijo + "-PE", motivo);
        }

        boolean pesoCorporal = equipamiento == EquipamientoPrograma.PESO_CORPORAL;
        boolean topeDePesoCorporal = pesoCorporal && dias >= 5;
        String programa;
        if (dias <= 3) programa = "CC";
        else if (dias == 4 || pesoCorporal) programa = "TP";
        else if (dias == 5) programa = "TPET";
        else programa = "ETP";

        boolean avanzado = nivel == NivelExperiencia.AVANZADO || nivel == NivelExperiencia.EXPERTO;
        String motivo;
        if (avanzado) motivo = topeDePesoCorporal ? AVANZADO_PESO_CORPORAL_MAS_DIAS : AVANZADO;
        else motivo = topeDePesoCorporal ? PESO_CORPORAL_MAS_DIAS : null;
        return new Recomendacion(prefijo + "-" + programa, motivo);
    }
}
