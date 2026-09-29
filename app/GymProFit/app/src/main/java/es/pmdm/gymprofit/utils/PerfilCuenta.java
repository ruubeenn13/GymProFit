package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.pmdm.gymprofit.model.usuario.Usuario;

// ============================================================
// PerfilCuenta — de quién es el perfil guardado en el móvil, y qué se trae o se sube
// a la API al entrar: el sexo y la actividad (GP-111) y el peso, la altura, la edad,
// el objetivo y el nivel (GP-129).
//
// Viven en claves globales que cerrarSesion() conserva a propósito. En un móvil con
// dos cuentas pueden ser de la otra, así que la app apunta el dueño (al terminar el
// onboarding, al guardar en Editar perfil y al traerlos de la API) y solo usa o sube
// los de la cuenta que entra.
//
// Las instalaciones de antes no tienen dueño apuntado: cuentan como de esta cuenta
// solo si es la única con el onboarding hecho en el móvil. Si no, no se inventa: la app
// sigue como hasta ahora, usando lo que hay sin subirlo.
//
// Sin Android dentro: el almacén es una interfaz que implementa PreferencesManager, y
// así la lógica se prueba en la JVM (PerfilCuentaTest).
// ============================================================
public final class PerfilCuenta {

    /**
     * Cada dato del perfil que se cruza con la API, con su nombre en UsuarioDTO y en el
     * PATCH, y lo que esta build entiende de él.
     */
    public enum Campo {
        SEXO("sexo", Arrays.asList("HOMBRE", "MUJER")),
        ACTIVIDAD("nivelActividad", Arrays.asList(
                CalculadoraNutricional.ACTIVIDAD_SEDENTARIO, CalculadoraNutricional.ACTIVIDAD_LIGERO,
                CalculadoraNutricional.ACTIVIDAD_MODERADO, CalculadoraNutricional.ACTIVIDAD_ACTIVO)),
        /** Kilos. Los rangos son los que acepta el onboarding. */
        PESO("peso", 30, 300),
        /** Centímetros. */
        ALTURA("altura", 100, 250),
        /** Años. */
        EDAD("edad", 10, 120),
        OBJETIVO("objetivo", Arrays.asList(
                CalculadoraNutricional.OBJETIVO_PERDER_PESO, CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR,
                CalculadoraNutricional.OBJETIVO_MANTENER_PESO, CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA)),
        NIVEL("nivelExperiencia", Arrays.asList("PRINCIPIANTE", "INTERMEDIO", "AVANZADO", "EXPERTO"));

        /** Nombre del campo en UsuarioDTO y en el PATCH. */
        public final String claveApi;
        private final List<String> valores;
        private final double minimo, maximo;

        Campo(String claveApi, List<String> valores) {
            this.claveApi = claveApi;
            this.valores = valores;
            this.minimo = 0;
            this.maximo = 0;
        }

        Campo(String claveApi, double minimo, double maximo) {
            this.claveApi = claveApi;
            this.valores = null;
            this.minimo = minimo;
            this.maximo = maximo;
        }

        /**
         * El valor en la forma en que lo guarda el móvil, o null si esta build no lo
         * entiende: un objetivo o un nivel nuevo, o un número fuera de rango.
         */
        @Nullable
        String entender(@Nullable String valor) {
            if (valor == null) return null;
            if (valores != null) return valores.contains(valor) ? valor : null;
            try {
                double d = Double.parseDouble(valor.trim());
                if (d < minimo || d > maximo) return null;
                return this == EDAD ? String.valueOf((int) d) : String.valueOf((float) d);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        /** Lo que se manda en el PATCH: número para los numéricos, texto para el resto. */
        Object paraApi(String entendido) {
            if (this == EDAD) return Integer.valueOf(entendido);
            if (valores == null) return new BigDecimal(entendido);
            return entendido;
        }
    }

    /** Campos que se cruzan con la API al entrar: todos. */
    static final Set<Campo> SINCRONIZADOS = Collections.unmodifiableSet(EnumSet.allOf(Campo.class));

    /**
     * Guarda un dato que el usuario puede dejar en blanco, o borra la clave si no lo dio
     * o no vale: una clave que existe cuenta como elegida y se sube a la API.
     */
    public static void guardarOpcional(@NonNull Almacen almacen, @NonNull Campo campo, @Nullable String valor) {
        String entendido = campo.entender(valor);
        if (entendido != null) almacen.guardar(campo, entendido);
        else almacen.borrar(campo);
    }

    /** De quién es el perfil local respecto a la cuenta que entra. */
    public enum Propiedad {
        /** De esta cuenta: se usa y, si la API no lo tiene, se sube. */
        MIA,
        /** De otra cuenta del mismo móvil: para esta, como si no hubiera nada. */
        AJENA,
        /** Instalación antigua sin dueño y con varias cuentas: se usa como antes, sin subirlo. */
        DESCONOCIDA
    }

    /** Lo que PerfilCuenta necesita del almacén local. Una clave que no existe es null. */
    public interface Almacen {
        /** El valor guardado, como texto, o null si la clave no existe. */
        @Nullable String leer(@NonNull Campo campo);
        void guardar(@NonNull Campo campo, @NonNull String valor);
        void borrar(@NonNull Campo campo);
        @Nullable String getDuenoPerfil();
        /** Usuarios con {@code onboarding_done_<usuario>} en el móvil. */
        @NonNull Set<String> getCuentasConOnboarding();
        void apuntarDuenoPerfil(String usuario);
    }

    /** Lo que sale de cruzar el perfil al entrar. */
    public static final class Resultado {
        /** El cuerpo del PATCH a mandar; vacío si no hay nada que subir. */
        @NonNull public final Map<String, Object> subir;
        /** Si ha cambiado algo en el móvil: entonces hay que recalcular el objetivo nutricional. */
        public final boolean cambiado;

        Resultado(@NonNull Map<String, Object> subir, boolean cambiado) {
            this.subir = subir;
            this.cambiado = cambiado;
        }
    }

    private PerfilCuenta() {}

    /**
     * Los campos del perfil que trae la API, de UsuarioDTO. Uno que la API no tiene no
     * entra. La altura y la edad llegan como primitivos en el modelo, así que un null de
     * la API se lee como 0: un 0 es que no lo tiene (ninguna pantalla deja guardarlo).
     */
    @NonNull
    public static Map<Campo, String> deUsuario(@NonNull Usuario u) {
        Map<Campo, String> m = new EnumMap<>(Campo.class);
        poner(m, Campo.SEXO, u.getSexo());
        poner(m, Campo.ACTIVIDAD, u.getNivelActividad());
        poner(m, Campo.PESO, u.getPeso());
        if (u.getAltura() > 0) m.put(Campo.ALTURA, String.valueOf(u.getAltura()));
        if (u.getEdad() > 0) m.put(Campo.EDAD, String.valueOf(u.getEdad()));
        poner(m, Campo.OBJETIVO, u.getObjetivo());
        poner(m, Campo.NIVEL, u.getNivelExperiencia());
        return m;
    }

    private static void poner(Map<Campo, String> m, Campo campo, @Nullable String valor) {
        if (valor != null && !valor.trim().isEmpty()) m.put(campo, valor);
    }

    /**
     * De quién es el perfil local.
     *
     * @param dueno usuario apuntado como dueño, o null en una instalación anterior a GP-111.
     * @param cuentasConOnboarding usuarios que terminaron el onboarding en este móvil.
     * @param usuario la cuenta que entra.
     */
    @NonNull
    public static Propiedad propiedad(@Nullable String dueno, @NonNull Collection<String> cuentasConOnboarding,
                                      @Nullable String usuario) {
        if (usuario == null || usuario.isEmpty()) return Propiedad.DESCONOCIDA;
        if (dueno != null) return dueno.equals(usuario) ? Propiedad.MIA : Propiedad.AJENA;
        boolean unica = cuentasConOnboarding.size() == 1 && cuentasConOnboarding.contains(usuario);
        return unica ? Propiedad.MIA : Propiedad.DESCONOCIDA;
    }

    /** Si la app puede usar el perfil local para la cuenta que entra. */
    public static boolean usable(@NonNull Propiedad propiedad) {
        return propiedad != Propiedad.AJENA;
    }

    /**
     * Cruza lo que trae la API con lo guardado en el móvil, al entrar.
     *
     * <p>Por campo: si la API lo tiene, se guarda en el móvil. Si no lo tiene y el del
     * móvil es de esta cuenta y se eligió de verdad (la clave existe: nunca un valor por
     * defecto), se devuelve para subirlo. Si la API trae algo y el perfil local no era de
     * esta cuenta, el campo que la API no tenga se tira, para que el de la otra cuenta
     * no pase a ser de esta. Un valor que esta build no entiende se deja estar: ni se
     * guarda ni se pisa.
     *
     * @param api los campos que trae la API; uno ausente es que no lo tiene.
     */
    @NonNull
    public static Resultado alEntrar(@NonNull Almacen almacen, @NonNull String usuario,
                                     @NonNull Map<Campo, String> api) {
        Propiedad propiedad = propiedad(almacen.getDuenoPerfil(), almacen.getCuentasConOnboarding(), usuario);
        boolean mia = propiedad == Propiedad.MIA;
        boolean apiTrae = false;
        for (Campo c : SINCRONIZADOS) apiTrae |= api.get(c) != null;

        Map<String, Object> subir = new HashMap<>();
        boolean cambiado = false;
        for (Campo campo : SINCRONIZADOS) {
            String deApi = api.get(campo);
            String local = almacen.leer(campo);
            if (deApi != null) {
                String entendido = campo.entender(deApi);
                if (entendido != null && !entendido.equals(local)) {
                    almacen.guardar(campo, entendido);
                    cambiado = true;
                }
            } else if (mia && local != null) {
                String entendido = campo.entender(local);
                if (entendido != null) subir.put(campo.claveApi, campo.paraApi(entendido));
            } else if (apiTrae && !mia && local != null) {
                almacen.borrar(campo);
                cambiado = true;
            }
        }

        // Una instalación antigua que es de esta cuenta, o un perfil que se acaba de
        // traer, pasa a tener dueño. Uno ajeno o sin dueño claro se queda como está.
        if (mia || apiTrae) almacen.apuntarDuenoPerfil(usuario);
        return new Resultado(subir, cambiado);
    }
}
