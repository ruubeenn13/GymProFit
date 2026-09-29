package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ============================================================
// PerfilCuenta — de quién es el sexo y la actividad guardados en el móvil, y qué se
// trae o se sube a la API al entrar (GP-111).
//
// Hasta la 1.1.3 solo vivían en el móvil, en claves globales que cerrarSesion()
// conserva a propósito. En un móvil con dos cuentas pueden ser de la otra, así que la
// app apunta el dueño (al terminar el onboarding, al guardar en Editar perfil y al
// traerlos de la API) y solo usa o sube los de la cuenta que entra.
//
// Las instalaciones de antes no tienen dueño apuntado: cuentan como de esta cuenta
// solo si es la única con el onboarding hecho en el móvil. Si no, no se inventa: la app
// sigue como hasta ahora, usando lo que hay sin subirlo.
//
// Sin Android dentro: el almacén es una interfaz que implementa PreferencesManager, y
// así la lógica se prueba en la JVM (PerfilCuentaTest).
// ============================================================
public final class PerfilCuenta {

    /** Valores que entiende la API (enums Sexo y NivelActividad). */
    static final List<String> SEXOS = Arrays.asList("HOMBRE", "MUJER");
    static final List<String> ACTIVIDADES = Arrays.asList(
            CalculadoraNutricional.ACTIVIDAD_SEDENTARIO, CalculadoraNutricional.ACTIVIDAD_LIGERO,
            CalculadoraNutricional.ACTIVIDAD_MODERADO, CalculadoraNutricional.ACTIVIDAD_ACTIVO);

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
        @Nullable String getSexoGuardado();
        @Nullable String getActividadGuardada();
        @Nullable String getDuenoPerfil();
        /** Usuarios con {@code onboarding_done_<usuario>} en el móvil. */
        @NonNull Set<String> getCuentasConOnboarding();
        void saveSexo(String sexo);
        void saveActividad(String actividad);
        void borrarSexo();
        void borrarActividad();
        void apuntarDuenoPerfil(String usuario);
    }

    private PerfilCuenta() {}

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
     * móvil es de esta cuenta y se eligió de verdad (la clave existe), se devuelve para
     * subirlo. Si la API trae algo y el perfil local no era de esta cuenta, el campo que
     * la API no tenga se tira, para que el de la otra cuenta no pase a ser de esta.
     * Un valor que esta build no conoce se deja estar: ni se guarda ni se pisa.
     *
     * @return el cuerpo del PATCH a mandar; vacío si no hay nada que subir.
     */
    @NonNull
    public static Map<String, Object> alEntrar(@NonNull Almacen almacen, @NonNull String usuario,
                                               @Nullable String apiSexo, @Nullable String apiActividad) {
        Propiedad propiedad = propiedad(almacen.getDuenoPerfil(), almacen.getCuentasConOnboarding(), usuario);
        boolean mia = propiedad == Propiedad.MIA;
        boolean apiTrae = apiSexo != null || apiActividad != null;
        Map<String, Object> subir = new HashMap<>();

        if (apiSexo != null) {
            if (SEXOS.contains(apiSexo)) almacen.saveSexo(apiSexo);
        } else if (mia && almacen.getSexoGuardado() != null) {
            subir.put("sexo", almacen.getSexoGuardado());
        } else if (apiTrae && !mia) {
            almacen.borrarSexo();
        }

        if (apiActividad != null) {
            if (ACTIVIDADES.contains(apiActividad)) almacen.saveActividad(apiActividad);
        } else if (mia && almacen.getActividadGuardada() != null) {
            subir.put("nivelActividad", almacen.getActividadGuardada());
        } else if (apiTrae && !mia) {
            almacen.borrarActividad();
        }

        // Una instalación antigua que es de esta cuenta, o un perfil que se acaba de
        // traer, pasa a tener dueño. Uno ajeno o sin dueño claro se queda como está.
        if (mia || apiTrae) almacen.apuntarDuenoPerfil(usuario);
        return subir;
    }
}
