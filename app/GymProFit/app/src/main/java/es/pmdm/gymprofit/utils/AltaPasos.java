package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// AltaPasos — el orden del alta nueva y qué hace falta para seguir (GP-103, lote 1.5.1).
//
// Cinco preguntas en cuatro capítulos, como el lienzo: objetivo y nivel (1), sobre ti
// (2), dónde y días con tiempo (3) y tu plan (4). La barra de arriba tiene un tramo por
// capítulo, y cada pregunta llena la parte que le toca: medio tramo la primera de un
// capítulo de dos, el tramo entero la segunda.
//
// Sin Android, para que el orden y las reglas se prueben en la JVM (AltaPasosTest).
// ============================================================
public final class AltaPasos {

    private AltaPasos() {}

    /** Los capítulos de la barra. */
    public static final int CAPITULOS = 4;

    /** Cada pantalla del cuestionario, en orden. */
    public enum Paso {
        OBJETIVO(1, 0.5f, true),
        NIVEL(1, 1f, false),
        SOBRE_TI(2, 1f, true),
        DONDE(3, 0.5f, true),
        DIAS(3, 1f, false),
        PLAN(4, 1f, false);

        /** El capítulo, de 1 a 4. */
        public final int capitulo;
        /** Cuánto de su tramo lleno deja esta pantalla. */
        public final float fraccion;
        /** Si es la primera del capítulo y lo abre con su icono (momento 4). */
        public final boolean abreCapitulo;

        Paso(int capitulo, float fraccion, boolean abreCapitulo) {
            this.capitulo = capitulo;
            this.fraccion = fraccion;
            this.abreCapitulo = abreCapitulo;
        }

        /** El progreso de 0 a 100 que lee TalkBack, como el aria-valuenow del lienzo. */
        public int porcentaje() {
            return (int) (((capitulo - 1) + fraccion) * 100f / CAPITULOS);
        }

        /** El siguiente, o null si es el último. */
        @Nullable
        public Paso siguiente() {
            Paso[] todos = values();
            return ordinal() + 1 < todos.length ? todos[ordinal() + 1] : null;
        }

        /** El anterior, o null si es el primero. */
        @Nullable
        public Paso anterior() {
            return ordinal() > 0 ? values()[ordinal() - 1] : null;
        }

        /** El paso con ese número de orden, o el primero si no existe. */
        @NonNull
        public static Paso de(int orden) {
            Paso[] todos = values();
            return orden >= 0 && orden < todos.length ? todos[orden] : OBJETIVO;
        }
    }

    /** Lo contestado hasta ahora, tal como está en el borrador. */
    public static final class Respuestas {
        public final String objetivo;
        public final String nivel;
        public final String sexo;
        public final int edad;
        public final double altura;
        public final String peso;
        public final String actividad;
        /** «Prefiero no decirlo» en «Sobre ti»: el plan sale sin calorías. */
        public final boolean sinDatos;
        public final String donde;
        public final int dias;
        public final int minutos;

        public Respuestas(String objetivo, String nivel, String sexo, int edad, double altura, String peso,
                          String actividad, boolean sinDatos, String donde, int dias, int minutos) {
            this.objetivo = vacioSiNull(objetivo);
            this.nivel = vacioSiNull(nivel);
            this.sexo = vacioSiNull(sexo);
            this.edad = edad;
            this.altura = altura;
            this.peso = vacioSiNull(peso);
            this.actividad = vacioSiNull(actividad);
            this.sinDatos = sinDatos;
            this.donde = vacioSiNull(donde);
            this.dias = dias;
            this.minutos = minutos;
        }

        /** Las cinco de «Sobre ti» contestadas, que es lo que deja calcular calorías. */
        public boolean sobreTiCompleto() {
            return !sexo.isEmpty() && edad >= ReglasEdad.MINIMA && edad <= ReglasEdad.MAXIMA
                    && altura > 0 && !peso.isEmpty() && !actividad.isEmpty();
        }

        /** Si el plan lleva calorías: con «Sobre ti» entero y sin «Prefiero no decirlo». */
        public boolean conCalorias() {
            return !sinDatos && sobreTiCompleto();
        }

        private static String vacioSiNull(String s) {
            return s == null ? "" : s;
        }
    }

    /**
     * Si «Siguiente» se puede pulsar en esa pantalla (decisión 1 del lienzo: apagado
     * hasta responder). En «Sobre ti», con las cinco respuestas; «Prefiero no decirlo»
     * es otro botón y no pasa por aquí.
     */
    public static boolean puedeSeguir(@NonNull Paso paso, @NonNull Respuestas r) {
        switch (paso) {
            case OBJETIVO: return !r.objetivo.isEmpty();
            case NIVEL:    return !r.nivel.isEmpty();
            case SOBRE_TI: return r.sobreTiCompleto();
            case DONDE:    return !r.donde.isEmpty();
            case DIAS:     return r.dias >= DIAS_MIN && r.dias <= DIAS_MAX && esMinutoValido(r.minutos);
            default:       return true;
        }
    }

    /**
     * La primera pantalla sin contestar, para retomar el cuestionario donde se dejó.
     * Si está todo, el plan.
     */
    @NonNull
    public static Paso primeroSinContestar(@NonNull Respuestas r) {
        for (Paso p : Paso.values()) {
            if (p == Paso.PLAN) return p;
            if (p == Paso.SOBRE_TI && r.sinDatos) continue;
            if (!puedeSeguir(p, r)) return p;
        }
        return Paso.PLAN;
    }

    // ── Días y tiempo ───────────────────────────────────────────────────────

    /** Los días por semana que se pueden elegir; los mismos que acepta la API (2 a 6). */
    public static final int DIAS_MIN = 2;
    public static final int DIAS_MAX = 6;
    /** Los minutos por sesión; los que acepta la API para seguir un programa. */
    public static final int[] MINUTOS = {30, 45, 60, 75};

    public static boolean esMinutoValido(int minutos) {
        for (int m : MINUTOS) if (m == minutos) return true;
        return false;
    }

    /**
     * Qué días de la semana enciende el ejemplo, de lunes (0) a domingo (6). Los del
     * lienzo: 2 → lunes y jueves; 3 → lunes, miércoles y viernes; 4 → lunes, martes,
     * jueves y viernes; 5 → de lunes a viernes; 6 → de lunes a sábado.
     */
    @NonNull
    public static boolean[] semanaDeEjemplo(int dias) {
        boolean[] s = new boolean[7];
        int[] encendidos;
        switch (dias) {
            case 2: encendidos = new int[]{0, 3}; break;
            case 3: encendidos = new int[]{0, 2, 4}; break;
            case 4: encendidos = new int[]{0, 1, 3, 4}; break;
            case 5: encendidos = new int[]{0, 1, 2, 3, 4}; break;
            case 6: encendidos = new int[]{0, 1, 2, 3, 4, 5}; break;
            default: encendidos = new int[0];
        }
        for (int d : encendidos) s[d] = true;
        return s;
    }

    /**
     * Las rutinas de un programa repartidas en los días del ejemplo: cada día encendido,
     * por orden, lleva la siguiente rutina, y al acabarse vuelve a empezar.
     *
     * @param dias     días por semana elegidos.
     * @param rutinas  cuántas rutinas distintas tiene la vista previa.
     * @return para cada día de la semana (0 lunes … 6 domingo), el índice de la rutina,
     *         o -1 si ese día no se entrena.
     */
    @NonNull
    public static int[] repartirRutinas(int dias, int rutinas) {
        boolean[] semana = semanaDeEjemplo(dias);
        int[] reparto = new int[7];
        int siguiente = 0;
        for (int d = 0; d < 7; d++) {
            if (semana[d] && rutinas > 0) {
                reparto[d] = siguiente % rutinas;
                siguiente++;
            } else {
                reparto[d] = -1;
            }
        }
        return reparto;
    }

    /**
     * Lo que distingue a una rutina de las demás de su programa: el nombre sin lo que
     * comparte con el del programa («Cuerpo completo A» en «Cuerpo completo» → «A»). Si
     * no comparte nada, o se queda vacío, el nombre entero.
     */
    @NonNull
    public static String rotuloCorto(@Nullable String programa, @Nullable String rutina) {
        if (rutina == null) return "";
        String r = rutina.trim();
        if (programa == null || programa.trim().isEmpty()) return r;
        String p = programa.trim();
        if (r.length() > p.length() && r.regionMatches(true, 0, p, 0, p.length())) {
            String resto = r.substring(p.length()).trim();
            if (!resto.isEmpty()) return resto;
        }
        return r;
    }
}
