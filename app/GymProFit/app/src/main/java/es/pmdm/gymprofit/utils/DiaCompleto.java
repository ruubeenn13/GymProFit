package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.pmdm.gymprofit.model.comida.DiaDiario;

// ============================================================
// DiaCompleto — el día completo (lote 1.6.8, GP-162, decisión 10 del lienzo, tablero 3)
//
// Un día está completo cuando las cinco comidas del diario tienen algo apuntado o están
// marcadas «Hoy no». Sin calorías clavadas: completo no es llegar al objetivo, es haberlo
// apuntado todo. Sin rachas ni culpa: un día a medias no rompe nada, solo no suma.
//
// El punto de cada día en la tira de la semana: verde si está completo; ámbar si un día
// que ya pasó tiene algo pero no todo; en curso (un aro) si es hoy y tiene algo; nada si
// no tiene nada o no ha llegado. Nunca rojo.
// ============================================================
public final class DiaCompleto {

    /** El punto de un día en la tira de la semana. */
    public enum Punto { NINGUNO, A_MEDIAS, EN_CURSO, COMPLETO }

    /**
     * La frase de debajo de «N días completos esta semana»: dice lo que hubo de verdad ese
     * día (entreno y proteína hecha) y solo dice «hoy» si lo es.
     */
    public enum Frase {
        HOY_ENTRENO_Y_PROTEINA, HOY_ENTRENO, HOY_PROTEINA, HOY,
        ESE_DIA_ENTRENO_Y_PROTEINA, ESE_DIA_ENTRENO, ESE_DIA_PROTEINA, ESE_DIA
    }

    private DiaCompleto() {
    }

    /**
     * ¿Están las cinco comidas apuntadas o marcadas «Hoy no»?
     *
     * @param apuntadas tipos con algo apuntado; lo que no es del diario no cuenta.
     * @param hoyNo     tipos marcados «Hoy no».
     */
    public static boolean completo(@Nullable Collection<String> apuntadas, @Nullable Collection<String> hoyNo) {
        return faltan(apuntadas, hoyNo) == 0;
    }

    /** Cuántas de las cinco comidas no están ni apuntadas ni marcadas «Hoy no». */
    public static int faltan(@Nullable Collection<String> apuntadas, @Nullable Collection<String> hoyNo) {
        Set<String> hechas = new HashSet<>();
        if (apuntadas != null) hechas.addAll(apuntadas);
        if (hoyNo != null) hechas.addAll(hoyNo);
        int faltan = 0;
        for (String tipo : ComidaQueToca.TIPOS) if (!hechas.contains(tipo)) faltan++;
        return faltan;
    }

    /** ¿Está completo ese día? null cuenta como vacío. */
    public static boolean completo(@Nullable DiaDiario dia) {
        return dia != null && completo(dia.getComidasApuntadas(), dia.getHoyNo());
    }

    /**
     * El punto de un día en la tira.
     *
     * @param dia    el día, o null si no se sabe (sin red): sin punto.
     * @param esHoy  si es hoy: a medias se pinta «en curso».
     * @param futuro si no ha llegado: nunca lleva punto.
     */
    @NonNull
    public static Punto punto(@Nullable DiaDiario dia, boolean esHoy, boolean futuro) {
        if (dia == null || futuro) return Punto.NINGUNO;
        if (completo(dia)) return Punto.COMPLETO;
        if (dia.getComidasApuntadas().isEmpty() && dia.getHoyNo().isEmpty()) return Punto.NINGUNO;
        return esHoy ? Punto.EN_CURSO : Punto.A_MEDIAS;
    }

    /** Los días completos de una semana; los null (sin datos) no cuentan. */
    public static int completosEnSemana(@Nullable List<DiaDiario> semana) {
        if (semana == null) return 0;
        int n = 0;
        for (DiaDiario d : semana) if (completo(d)) n++;
        return n;
    }

    /** La frase que acompaña al recuento de la semana. */
    @NonNull
    public static Frase frase(boolean esHoy, boolean entreno, boolean proteina) {
        if (esHoy) {
            if (entreno && proteina) return Frase.HOY_ENTRENO_Y_PROTEINA;
            if (entreno) return Frase.HOY_ENTRENO;
            return proteina ? Frase.HOY_PROTEINA : Frase.HOY;
        }
        if (entreno && proteina) return Frase.ESE_DIA_ENTRENO_Y_PROTEINA;
        if (entreno) return Frase.ESE_DIA_ENTRENO;
        return proteina ? Frase.ESE_DIA_PROTEINA : Frase.ESE_DIA;
    }

    /**
     * ¿Toca celebrarlo (momento 21)? Solo al pasar a completo mientras se mira, no al abrir
     * un día que ya lo estaba, y una vez al día aunque se quite algo y se vuelva a completar.
     *
     * @param antes      si estaba completo antes de recargar; null si no se sabía.
     * @param ahora      si lo está ahora.
     * @param fecha      el día, yyyy-MM-dd.
     * @param celebrado  el último día celebrado en este móvil, o null.
     */
    public static boolean celebrar(@Nullable Boolean antes, boolean ahora, @NonNull String fecha,
                                   @Nullable String celebrado) {
        return Boolean.FALSE.equals(antes) && ahora && !fecha.equals(celebrado);
    }
}
