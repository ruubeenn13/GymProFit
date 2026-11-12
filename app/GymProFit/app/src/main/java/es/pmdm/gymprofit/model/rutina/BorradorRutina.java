package es.pmdm.gymprofit.model.rutina;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.DuracionRutina;
import es.pmdm.gymprofit.utils.Enumeracion;
import es.pmdm.gymprofit.utils.FechaUtils;

// ============================================================
// BorradorRutina — la rutina que se está creando o editando (GP-244).
//
// Viaja entre los tres pasos (qué y cuánto, eliges los ejercicios, tu rutina) como JSON en
// el Intent, y lo que la pantalla dice sale de aquí, sin Android, para probarlo en la JVM:
//   · el nombre propuesto: las zonas elegidas («Pecho y hombros») o, buscando entre todos,
//     las de los ejercicios, de más a menos series (como las tarjetas, FrasesTarjeta);
//   · los minutos, calculados (DuracionRutina, la fórmula de la API) y su frase contra el
//     tiempo elegido: te cabe uno más, justo o te pasas;
//   · subir, bajar y quitar, para ordenar sin arrastrar (también con TalkBack);
//   · y que sin ejercicios o sin nombre no se guarda.
// Al editar no hay tiempo elegido (minutos es null) y cada ejercicio recuerda su fila.
// ============================================================
public class BorradorRutina {

    /** Las zonas del paso 1, en el orden de sus tarjetas: los GrupoMuscular de la API. */
    public static final List<String> ZONAS =
            Collections.unmodifiableList(Arrays.asList("PECHO", "ESPALDA", "PIERNAS", "HOMBROS", "BRAZOS", "ABDOMEN"));

    /** Cuántas zonas nombra como mucho lo que sale de los ejercicios. */
    private static final int MAX_ZONAS = 3;

    /** El tiempo «75+»: por encima no hay techo. */
    public static final int MINUTOS_SIN_TECHO = 75;

    /** Un ejercicio de la rutina con su pauta. */
    public static class Ejercicio {
        public int ejercicioId;
        /** La fila de rutina-ejercicio, al editar; null si se acaba de añadir. */
        @Nullable public Integer filaId;
        public String nombre;
        /** GrupoMuscular del catálogo (PECHO…, CARDIO, FULLBODY). */
        @Nullable public String grupo;
        @Nullable public String equipo;
        public int series = 3;
        public int min = 8;
        public int max = 12;
        /** Si la serie se mide en segundos y no en repeticiones. */
        public boolean segundos;
        /** PIERNA, BRAZO, LADO o null: se conserva al editar, no se cambia aquí. */
        @Nullable public String porLado;
        public int descanso = 90;

        DuracionRutina.Pieza pieza() {
            return new DuracionRutina.Pieza(series, min, max, segundos, porLado != null, descanso);
        }
    }

    /** El rango con que entra un ejercicio por tiempo, en segundos (y al pasar a segundos). */
    public static final int MIN_SEGUNDOS = 30;
    public static final int MAX_SEGUNDOS = 45;

    /** null al crear. */
    @Nullable public Integer rutinaId;
    @Nullable public String nombre;
    /** Si la persona ha escrito el nombre: entonces ya no se propone otro. */
    public boolean nombreTocado;
    @Nullable public String descripcion;
    /** PRINCIPIANTE, INTERMEDIO o AVANZADO. */
    @Nullable public String nivel;
    /** Las zonas elegidas en el paso 1; vacío si se busca entre todos o al editar. */
    public List<String> zonas = new ArrayList<>();
    /** 30, 45, 60 o 75 (75+); null al editar. */
    @Nullable public Integer minutos;
    public List<Ejercicio> ejercicios = new ArrayList<>();

    // ── Entre pantallas ─────────────────────────────────────────────────

    /** En JSON, para el Intent. */
    @NonNull
    public String aJson() {
        return new com.google.gson.Gson().toJson(this);
    }

    /** Del JSON del Intent; uno vacío si no hay o no se entiende. */
    @NonNull
    public static BorradorRutina deJson(@Nullable String json) {
        if (json == null || json.isEmpty()) return new BorradorRutina();
        try {
            BorradorRutina b = new com.google.gson.Gson().fromJson(json, BorradorRutina.class);
            if (b == null) return new BorradorRutina();
            if (b.zonas == null) b.zonas = new ArrayList<>();
            if (b.ejercicios == null) b.ejercicios = new ArrayList<>();
            return b;
        } catch (com.google.gson.JsonParseException e) {
            // Solo lo escribe la propia app (aJson): uno ilegible no debería pasar, y con uno vacío
            // la pantalla sigue siendo usable en vez de cerrarse.
            return new BorradorRutina();
        }
    }

    /** Los ids de los ejercicios, en su orden: para saber si algo ha cambiado. */
    @NonNull
    public List<Integer> ids() {
        List<Integer> l = new ArrayList<>();
        for (Ejercicio e : ejercicios) l.add(e.ejercicioId);
        return l;
    }

    // ── Los textos ──────────────────────────────────────────────────────

    /** Una frase con dos cifras. */
    public interface Dos { String de(int a, int b); }

    /** Una frase con una cifra. */
    public interface Una { String de(int a); }

    /** El nombre en frase de una zona por su GrupoMuscular («pecho»). */
    public interface NombreZona { String de(String grupo); }

    /** Los textos, sacados de los recursos (o de las cadenas de verdad en un test). */
    public static final class Textos {
        final String y, ySerie, porDefecto;
        final Locale idioma;
        final Dos cabe, pasas;
        final Una justo, sinTecho;
        final NombreZona zona;

        public Textos(String y, String ySerie, Locale idioma, String porDefecto, Dos cabe, Una justo, Dos pasas,
                      Una sinTecho, NombreZona zona) {
            this.y = y;
            this.ySerie = ySerie;
            this.idioma = idioma;
            this.porDefecto = porDefecto;
            this.cabe = cabe;
            this.justo = justo;
            this.pasas = pasas;
            this.sinTecho = sinTecho;
            this.zona = zona;
        }

        /** Los de la app, en su idioma. */
        @NonNull
        public static Textos de(@NonNull Context c) {
            return new Textos(c.getString(R.string.enumeracion_y), c.getString(R.string.tarjeta_y_serie),
                    FechaUtils.localeDeLaApp(c), c.getString(R.string.crear_nombre_por_defecto),
                    (m, e) -> c.getString(R.string.crear_tiempo_cabe, m, e),
                    e -> c.getString(R.string.crear_tiempo_justo, e),
                    (d, e) -> c.getString(R.string.crear_tiempo_te_pasas, d, e),
                    m -> c.getString(R.string.crear_tiempo_75, m),
                    g -> c.getString(nombreZona(g)));
        }
    }

    /** El nombre en frase de una zona («pecho», «abdomen»), el de las tarjetas. */
    public static int nombreZona(@Nullable String grupo) {
        if (grupo == null) return R.string.zona_core;
        switch (grupo) {
            case "PECHO": return R.string.zona_pecho;
            case "ESPALDA": return R.string.zona_espalda;
            case "PIERNAS": return R.string.zona_pierna;
            case "HOMBROS": return R.string.zona_hombros;
            case "BRAZOS": return R.string.zona_brazos;
            default: return R.string.zona_core;
        }
    }

    // ── La lista ────────────────────────────────────────────────────────

    /** Si el ejercicio ya está en la rutina. */
    public boolean contiene(int ejercicioId) {
        for (Ejercicio e : ejercicios) if (e.ejercicioId == ejercicioId) return true;
        return false;
    }

    /**
     * Añade un ejercicio al final, con 3 series de 8 a 12 y 90 s de descanso.
     *
     * @return false si ya estaba.
     */
    public boolean anadir(int ejercicioId, String nombre, @Nullable String grupo, @Nullable String equipo) {
        return anadir(ejercicioId, nombre, grupo, equipo, false);
    }

    /**
     * Añade un ejercicio con la medida de su catálogo (GP-130): uno por tiempo entra en
     * segundos, de 30 a 45, como al cambiarlo de medida en la pauta; los demás, de 8 a 12.
     * La pauta deja cambiarla después. Falso si ya estaba.
     */
    public boolean anadir(int ejercicioId, String nombre, @Nullable String grupo, @Nullable String equipo,
                          boolean porTiempo) {
        if (contiene(ejercicioId)) return false;
        Ejercicio e = new Ejercicio();
        e.ejercicioId = ejercicioId;
        e.nombre = nombre;
        e.grupo = grupo;
        e.equipo = equipo;
        if (porTiempo) {
            e.segundos = true;
            e.min = MIN_SEGUNDOS;
            e.max = MAX_SEGUNDOS;
        }
        ejercicios.add(e);
        return true;
    }

    /** Quita el ejercicio con ese id, si está. */
    public void quitarId(int ejercicioId) {
        for (int i = 0; i < ejercicios.size(); i++) {
            if (ejercicios.get(i).ejercicioId == ejercicioId) {
                ejercicios.remove(i);
                return;
            }
        }
    }

    /** Quita el de esa posición. */
    public void quitar(int i) {
        if (i >= 0 && i < ejercicios.size()) ejercicios.remove(i);
    }

    /** Sube uno un puesto. @return false si es el primero o no existe. */
    public boolean subir(int i) {
        if (i <= 0 || i >= ejercicios.size()) return false;
        Collections.swap(ejercicios, i, i - 1);
        return true;
    }

    /** Baja uno un puesto. @return false si es el último o no existe. */
    public boolean bajar(int i) {
        if (i < 0 || i >= ejercicios.size() - 1) return false;
        Collections.swap(ejercicios, i, i + 1);
        return true;
    }

    /** Mueve uno de una posición a otra (arrastrar). */
    public void mover(int desde, int hasta) {
        if (desde < 0 || hasta < 0 || desde >= ejercicios.size() || hasta >= ejercicios.size()) return;
        ejercicios.add(hasta, ejercicios.remove(desde));
    }

    /** Las series de todos los ejercicios. */
    public int seriesTotales() {
        int n = 0;
        for (Ejercicio e : ejercicios) n += e.series;
        return n;
    }

    /** Se guarda con un nombre y al menos un ejercicio. */
    public boolean sePuedeGuardar() {
        return !ejercicios.isEmpty() && nombre != null && !nombre.trim().isEmpty();
    }

    // ── Los minutos ─────────────────────────────────────────────────────

    /** Los minutos de la rutina, con la fórmula de la API. */
    public int minutosEstimados() {
        return DuracionRutina.minutos(piezas(false));
    }

    private List<DuracionRutina.Pieza> piezas(boolean unoMas) {
        List<DuracionRutina.Pieza> l = new ArrayList<>();
        for (Ejercicio e : ejercicios) l.add(e.pieza());
        if (unoMas) l.add(DuracionRutina.Pieza.porDefecto());
        return l;
    }

    /**
     * «35 de tus 45 minutos: te cabe un ejercicio más.», «Justo en tus 45 minutos.» o «Te
     * pasas unos 10 minutos de tus 45.». Con 75+, por encima de 75 no se pasa: «Unos 80
     * minutos, en tus 75 o más.».
     *
     * @return null al editar (sin tiempo elegido) o sin ejercicios.
     */
    @Nullable
    public String fraseTiempo(@NonNull Textos t) {
        if (minutos == null || ejercicios.isEmpty()) return null;
        int m = minutosEstimados();
        int e = minutos;
        if (m > e) {
            return e >= MINUTOS_SIN_TECHO ? t.sinTecho.de(m) : t.pasas.de(m - e, e);
        }
        if (DuracionRutina.minutos(piezas(true)) <= e) return t.cabe.de(m, e);
        return t.justo.de(e);
    }

    /** Cuánto de la barra del tiempo se llena: de 0 a 1. */
    public float partesDelTiempo() {
        if (minutos == null || minutos <= 0) return 0f;
        return Math.min(1f, minutosEstimados() / (float) minutos);
    }

    // ── El nombre y las zonas ───────────────────────────────────────────

    /**
     * El nombre propuesto: las zonas elegidas, en el orden de sus tarjetas; o, sin zonas, las
     * de los ejercicios. Sin ninguna, «Mi rutina».
     */
    @NonNull
    public String nombrePropuesto(@NonNull Textos t) {
        List<String> grupos = new ArrayList<>();
        if (!zonas.isEmpty()) {
            for (String z : ZONAS) if (zonas.contains(z)) grupos.add(z);
        } else {
            grupos = zonasDeLosEjercicios();
        }
        if (grupos.isEmpty()) return t.porDefecto;
        return frase(t, grupos);
    }

    /** «Pecho y hombros»: las zonas de los ejercicios, de más a menos series; "" si no hay. */
    @NonNull
    public String zonasEnFrase(@NonNull Textos t) {
        List<String> grupos = zonasDeLosEjercicios();
        return grupos.isEmpty() ? "" : frase(t, grupos);
    }

    private static String frase(Textos t, List<String> grupos) {
        List<String> nombres = new ArrayList<>();
        for (String g : grupos) nombres.add(t.zona.de(g));
        String unida = Enumeracion.unir(nombres, nombres.size() <= 2 ? t.y : t.ySerie);
        return Enumeracion.alPrincipio(unida, t.idioma);
    }

    // Las zonas de los ejercicios (sin cardio ni cuerpo completo), de más a menos series y, a
    // igualdad, en el orden de las tarjetas del paso 1; como mucho tres.
    private List<String> zonasDeLosEjercicios() {
        Map<String, Integer> series = new LinkedHashMap<>();
        for (String z : ZONAS) series.put(z, 0);
        for (Ejercicio e : ejercicios) {
            if (e.grupo != null && series.containsKey(e.grupo)) series.put(e.grupo, series.get(e.grupo) + e.series);
        }
        List<String> grupos = new ArrayList<>();
        for (Map.Entry<String, Integer> s : series.entrySet()) if (s.getValue() > 0) grupos.add(s.getKey());
        // Orden estable: los empates conservan el de las tarjetas.
        grupos.sort((a, b) -> Integer.compare(series.get(b), series.get(a)));
        return grupos.size() > MAX_ZONAS ? new ArrayList<>(grupos.subList(0, MAX_ZONAS)) : grupos;
    }
}
