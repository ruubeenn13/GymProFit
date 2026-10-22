package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// ============================================================
// SemanaNutricion — «Tu semana de nutrición» (tablero 12 del lienzo de nutrición, decisión 13,
// GP-210 paso 1), en lógica pura para poder probarla.
//
// La semana, de lunes a domingo, se agrega en el cliente con lo que ya dan
// /comidas/usuario/{id}/resumen (kcal y proteína de cada día) y /diario (días completos,
// agua y entrenos): son siete números por cuenta y no piden endpoint. Lo único que no se
// puede agregar así, los alimentos más apuntados, sí viene del servidor (/diario/mas-apuntados).
//
// Reglas:
//   · Solo cuentan los días que han llegado: una semana en curso no se rebaja con los días
//     que faltan.
//   · Las medias son de los días con comida apuntada: un día sin nada no es un cero comido,
//     es un día sin apuntar, y no baja la media.
//   · Sin objetivo (GP-103, «Prefiero no decirlo») hay medias pero nada que comparar.
//   · Una observación solo se dice si pasó; nunca un reproche («solo dice lo que hubo»).
// ============================================================
public final class SemanaNutricion {

    /** Lo que se sabe de un día de la semana, ya reducido a números. */
    public static final class Entrada {
        /** «yyyy-MM-dd». */
        @NonNull public final String fecha;
        /** ¿Hay comida apuntada ese día? Sin ella, no hay media que contar. */
        public final boolean hayComida;
        public final double proteina;
        public final int kcal;
        public final boolean completo;
        public final boolean entreno;
        public final int aguaMl;

        public Entrada(@NonNull String fecha, boolean hayComida, double proteina, int kcal, boolean completo,
                       boolean entreno, int aguaMl) {
            this.fecha = fecha;
            this.hayComida = hayComida;
            this.proteina = proteina;
            this.kcal = kcal;
            this.completo = completo;
            this.entreno = entreno;
            this.aguaMl = aguaMl;
        }
    }

    /** Una frase que dice lo que pasó esa semana. */
    public static final class Observacion {
        public enum Tipo {
            /** «Los 3 días que entrenaste, llegaste a tu proteína.» valor = días que entrenó. */
            ENTRENO_Y_PROTEINA,
            /** «Entre semana llegas siempre; el fin de semana te quedas en 104 g.» valor = gramos del fin de semana. */
            ENTRE_SEMANA_Y_FIN_DE_SEMANA
        }

        @NonNull public final Tipo tipo;
        public final int valor;

        Observacion(@NonNull Tipo tipo, int valor) {
            this.tipo = tipo;
            this.valor = valor;
        }
    }

    private final List<Entrada> dias;
    private final String hoy;
    private final int objetivoProteinas, objetivoKcal, objetivoAguaMl;

    private SemanaNutricion(List<Entrada> dias, String hoy, int objetivoProteinas, int objetivoKcal,
                            int objetivoAguaMl) {
        this.dias = Collections.unmodifiableList(dias);
        this.hoy = hoy;
        this.objetivoProteinas = objetivoProteinas;
        this.objetivoKcal = objetivoKcal;
        this.objetivoAguaMl = objetivoAguaMl;
    }

    /**
     * @param siete          los siete días, de lunes a domingo.
     * @param hoy            hoy, «yyyy-MM-dd»: los días posteriores no han llegado.
     * @param objetivoProteinas objetivo de proteína del día en gramos; 0 si no hay objetivo.
     * @param objetivoKcal   objetivo de kcal del día; 0 si no hay.
     * @param objetivoAguaMl objetivo de agua del día en ml; 0 si no hay.
     */
    @NonNull
    public static SemanaNutricion de(@NonNull List<Entrada> siete, @NonNull String hoy, int objetivoProteinas,
                                     int objetivoKcal, int objetivoAguaMl) {
        return new SemanaNutricion(new ArrayList<>(siete), hoy, objetivoProteinas, objetivoKcal, objetivoAguaMl);
    }

    @NonNull
    public List<Entrada> dias() { return dias; }

    public int objetivoProteinas() { return objetivoProteinas; }

    public int objetivoKcal() { return objetivoKcal; }

    public int objetivoAguaMl() { return objetivoAguaMl; }

    public boolean hayObjetivoProteina() { return objetivoProteinas > 0; }

    /** ¿Ha llegado ese día? Hoy cuenta. */
    public boolean haLlegado(@NonNull Entrada e) {
        return e.fecha.compareTo(hoy) <= 0;
    }

    private boolean cuenta(Entrada e) {
        return haLlegado(e) && e.hayComida;
    }

    /** Días de la semana que han llegado (7 en una semana pasada). */
    public int diasQueHanLlegado() {
        int n = 0;
        for (Entrada e : dias) if (haLlegado(e)) n++;
        return n;
    }

    /** Días que han llegado y tienen comida apuntada. */
    public int diasConDatos() {
        int n = 0;
        for (Entrada e : dias) if (cuenta(e)) n++;
        return n;
    }

    /** ¿No hay nada apuntado en ningún día que haya llegado? */
    public boolean vacia() {
        return diasConDatos() == 0;
    }

    // ── Proteína ────────────────────────────────────────────────────────────

    /** Días en que se llegó al objetivo de proteína; 0 si no hay objetivo. */
    public int proteinaConseguida() {
        if (!hayObjetivoProteina()) return 0;
        int n = 0;
        for (Entrada e : dias) if (cuenta(e) && e.proteina >= objetivoProteinas) n++;
        return n;
    }

    /** Media de proteína de los días con comida, en gramos; null si no hay ninguno. */
    @Nullable
    public Integer mediaProteina() {
        double suma = 0;
        int n = 0;
        for (Entrada e : dias) if (cuenta(e)) { suma += e.proteina; n++; }
        return n == 0 ? null : (int) Math.round(suma / n);
    }

    // ── Días completos ──────────────────────────────────────────────────────

    /** Días completos de los que han llegado. */
    public int diasCompletos() {
        int n = 0;
        for (Entrada e : dias) if (haLlegado(e) && e.completo) n++;
        return n;
    }

    // ── Calorías ────────────────────────────────────────────────────────────

    /** Media de kcal de los días con comida; null si no hay ninguno. */
    @Nullable
    public Integer mediaKcal() {
        long suma = 0;
        int n = 0;
        for (Entrada e : dias) if (cuenta(e)) { suma += e.kcal; n++; }
        return n == 0 ? null : (int) Math.round((double) suma / n);
    }

    /** La media de kcal como % del objetivo; 0 si no hay objetivo o no hay media. */
    public int porcentajeKcal() {
        Integer media = mediaKcal();
        if (media == null || objetivoKcal <= 0) return 0;
        return (int) Math.round(media * 100.0 / objetivoKcal);
    }

    // ── Agua ────────────────────────────────────────────────────────────────

    /** Media de agua de los días en que se bebió algo y han llegado, en ml; null si ninguno. */
    @Nullable
    public Integer mediaAguaMl() {
        long suma = 0;
        int n = 0;
        for (Entrada e : dias) if (haLlegado(e) && e.aguaMl > 0) { suma += e.aguaMl; n++; }
        return n == 0 ? null : (int) Math.round((double) suma / n);
    }

    /** Días en que se llegó al objetivo de agua; 0 si no hay objetivo. */
    public int diasConAguaCumplida() {
        if (objetivoAguaMl <= 0) return 0;
        int n = 0;
        for (Entrada e : dias) if (haLlegado(e) && e.aguaMl >= objetivoAguaMl) n++;
        return n;
    }

    // ── Observaciones ───────────────────────────────────────────────────────

    /**
     * Las frases de la semana, solo las que pasaron. Sin objetivo de proteína no hay ninguna.
     * Entre semana es de lunes a viernes; el fin de semana, sábado y domingo.
     */
    @NonNull
    public List<Observacion> observaciones() {
        List<Observacion> o = new ArrayList<>();
        if (!hayObjetivoProteina()) return o;

        int entrenados = 0;
        boolean todosLlegan = true;
        for (Entrada e : dias) {
            if (!cuenta(e) || !e.entreno) continue;
            entrenados++;
            if (e.proteina < objetivoProteinas) todosLlegan = false;
        }
        if (entrenados > 0 && todosLlegan) o.add(new Observacion(Observacion.Tipo.ENTRENO_Y_PROTEINA, entrenados));

        int entreSemana = 0;
        boolean entreSemanaLlega = true;
        double sumaFinde = 0;
        int finde = 0;
        for (int i = 0; i < dias.size(); i++) {
            Entrada e = dias.get(i);
            if (!cuenta(e)) continue;
            if (i < 5) {
                entreSemana++;
                if (e.proteina < objetivoProteinas) entreSemanaLlega = false;
            } else {
                finde++;
                sumaFinde += e.proteina;
            }
        }
        // Entre semana tiene que haber al menos tres días para decir «siempre».
        if (entreSemana >= 3 && entreSemanaLlega && finde > 0 && sumaFinde / finde < objetivoProteinas) {
            o.add(new Observacion(Observacion.Tipo.ENTRE_SEMANA_Y_FIN_DE_SEMANA, (int) Math.round(sumaFinde / finde)));
        }
        return o;
    }
}
