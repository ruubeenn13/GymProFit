package es.pmdm.gymprofit.model.envivo;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

// ============================================================
// SesionEnCurso — la sesión en vivo mientras dura (GP-012), tal cual se escribe al
// fichero de la cuenta.
//
// Lleva todo lo que hace falta para seguir aunque Android cierre la app, se reinicie
// el móvil o la rutina se borre en el servidor: la rutina (id y nombres, copiados al
// empezar), el inicio del reloj, los ejercicios con su pauta y sus series TAL COMO SE
// ESCRIBIERON (texto: «57,» a medio teclear también), lo de la última vez de cada uno
// (GP-014), el cronómetro en marcha y el estado del guardado.
//
// Es un objeto de datos plano a propósito: Gson lo escribe y lo lee sin adaptadores,
// y la lógica vive en LogicaSesion, que se prueba sin Android. Los campos son públicos
// por lo mismo; quien los cambia es SesionEnCursoRepositorio, que escribe el fichero
// después de cada cambio.
// ============================================================
public class SesionEnCurso {

    /** Versión del formato del fichero. Sube si cambia la forma de lo guardado. */
    public static final int FORMATO = 1;

    /** Series como máximo por ejercicio: lo que admite la API. */
    public static final int MAX_SERIES = 20;

    /** Series vacías con las que entra un ejercicio añadido durante la sesión. */
    public static final int SERIES_AL_ANADIR = 3;

    /** Estado del guardado. Desde el primer intento, la sesión ya no se edita. */
    public enum Guardado {
        /** Entrenando: todo se puede cambiar. */
        EDITANDO,
        /** Hay un envío en el aire. Si el proceso muere así, al volver es un fallo. */
        GUARDANDO,
        /** No se pudo guardar (o no se sabe): sigue en el móvil, con reintentar. */
        FALLO
    }

    /** Estado de la carga de los ejercicios de la rutina. */
    public enum Carga { CARGANDO, LISTA, ERROR }

    public int formato = FORMATO;
    /** Dueño de la sesión: el fichero es por cuenta, y esto lo confirma al leerlo. */
    public int usuarioId;

    @Nullable public Integer rutinaId;
    @Nullable public String rutinaNombre;
    @Nullable public String programaNombre;

    /** Cuándo se tocó «Empezar», en milisegundos de época. El reloj cuenta desde aquí. */
    public long inicioMs;

    public Carga carga = Carga.LISTA;
    public List<Ejercicio> ejercicios = new ArrayList<>();
    /** Para dar ids estables a las series nuevas. */
    public long siguienteId = 1;

    @Nullable public Cronometro cronometro;

    /** El descanso en marcha (GP-013), o null. Vive aquí para seguir si Android cierra la app. */
    @Nullable public Descanso descanso;

    // Lo de la hoja de terminar: se guarda con la sesión para que no se pierda.
    /** Minutos ajustados a mano; null = los del reloj. */
    @Nullable public Integer duracionAjustada;
    /** De 1 a 5, o null sin valorar. */
    @Nullable public Integer valoracion;
    @Nullable public String notas;

    public Guardado guardado = Guardado.EDITANDO;
    /** Nace en el primer intento de guardar y se reusa en cada reintento (GP-006). */
    @Nullable public String claveIdempotencia;
    /** Lo que se mandó en el primer intento: los reintentos mandan exactamente esto. */
    @Nullable public Integer duracionEnviada;
    /** Código del último fallo: HTTP, -1 red, 0 sin sesión en la respuesta. */
    public int codigoFallo;
    @Nullable public String mensajeFallo;

    /** Un ejercicio de la sesión: su pauta, lo hecho y lo de la última vez. */
    public static class Ejercicio {
        public long id;
        public int ejercicioId;
        @Nullable public String nombre;

        // Pauta de la rutina, como en el detalle. Nulos en un ejercicio añadido aquí.
        public int seriesPauta;
        public int repeticionesPauta;
        @Nullable public Integer minimo;
        @Nullable public Integer maximo;
        /** REPETICIONES, SEGUNDOS o null. */
        @Nullable public String medida;
        /** PIERNA, BRAZO, LADO o null. */
        @Nullable public String porLado;
        @Nullable public Integer descanso;
        @Nullable public String notasPauta;
        /** Si viene de la rutina (con pauta) o se añadió durante la sesión. */
        public boolean deRutina;

        public List<Serie> series = new ArrayList<>();

        /** Si ya se preguntó a /sesiones/ultima-vez por él. Sin preguntar, no se sabe. */
        public boolean anteriorCargado;
        /** Día de la última vez, «yyyy-MM-ddTHH:mm:ss»; null si no la hubo. */
        @Nullable public String anteriorFecha;
        public List<SerieAnterior> anterior = new ArrayList<>();
    }

    /** Una serie tal como se va escribiendo. */
    public static class Serie {
        public long id;
        public String peso = "";
        public String repeticiones = "";
        /** En las series por tiempo: «40» o «0:40». */
        public String segundos = "";
        public boolean hecha;
    }

    /** Una serie de la última vez, como la da la API. */
    public static class SerieAnterior {
        public int numero;
        /** Kilos como texto exacto de la API («57.50»); null en peso corporal. */
        @Nullable public String peso;
        public int repeticiones;
        @Nullable public Integer segundos;

        public SerieAnterior() { }

        public SerieAnterior(int numero, @Nullable String peso, int repeticiones, @Nullable Integer segundos) {
            this.numero = numero;
            this.peso = peso;
            this.repeticiones = repeticiones;
            this.segundos = segundos;
        }
    }

    /** El cronómetro de una serie por tiempo, en marcha. Cuenta desde {@link #inicioMs}. */
    public static class Cronometro {
        public long serieId;
        public long inicioMs;
        public boolean avisoMinimo;
        public boolean avisoMaximo;
    }

    /**
     * El descanso entre series, en marcha (GP-013). Termina en {@link #finMs}; la cuenta
     * atrás se pinta desde ahí, así que no se para aunque Android cierre la app.
     */
    public static class Descanso {
        /** La serie cuyo marcado lo empezó: desmarcarla lo quita. */
        public long serieId;
        /** Cuándo acaba, en milisegundos de época. */
        public long finMs;
        /** Lo que dura en total, con los ±15 s: para la barra de progreso. */
        public long duracionMs;
    }
}
