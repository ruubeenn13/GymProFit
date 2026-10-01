package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// ============================================================
// PerfilAlta — lo contestado en el alta, como perfil (GP-103, lote 1.5.1).
//
// Dos sitios lo usan y tienen que hacer lo mismo: «Guarda tu plan», que crea la cuenta
// con el perfil entero en el alta, y quien entra con una cuenta sin onboarding, que lo
// sube con el PATCH. Aquí se arma el cuerpo y se deja en el móvil como perfil de la
// cuenta, igual que hacía el resumen del onboarding de antes.
//
// Con «Prefiero no decirlo» no va ni se guarda nada del cuerpo (sexo, actividad, peso,
// altura y edad): el plan sale sin calorías y se añaden luego en el perfil.
// ============================================================
public final class PerfilAlta {

    private PerfilAlta() {}

    /** Peso que admite el alta, el mismo que Editar perfil. */
    private static final double PESO_MIN = 30;
    private static final double PESO_MAX = 300;

    /**
     * Los campos del perfil para el alta o el PATCH: nivel (en tres), objetivo y, si se
     * contestó «Sobre ti», sexo, actividad, edad, altura y peso.
     */
    @NonNull
    public static Map<String, Object> cuerpo(@NonNull AltaPasos.Respuestas r) {
        Map<String, Object> body = new HashMap<>();
        if (!r.nivel.isEmpty()) body.put("nivelExperiencia", NivelVisible.valor(r.nivel));
        if (!r.objetivo.isEmpty()) body.put("objetivo", r.objetivo);
        if (r.conCalorias()) {
            body.put("sexo", r.sexo);
            body.put("nivelActividad", r.actividad);
            body.put("edad", r.edad);
            body.put("altura", BigDecimal.valueOf(r.altura));
            BigDecimal peso = Numeros.exacto(r.peso, PESO_MIN, PESO_MAX);
            if (peso != null) body.put("peso", peso);
        }
        return body;
    }

    /**
     * Lo contestado pasa a ser el perfil de esta cuenta en el móvil. Con «Prefiero no
     * decirlo», los datos del cuerpo se borran: los que hubiera serían de otra cuenta, y
     * desde ahora el perfil del móvil es de esta.
     *
     * @param usuario la cuenta, ya con su id guardado.
     */
    public static void guardarLocal(@NonNull PreferencesManager prefs, @NonNull AltaPasos.Respuestas r,
                                    @NonNull String usuario) {
        prefs.apuntarDuenoPerfil(usuario);
        if (r.conCalorias()) {
            prefs.saveSexo(r.sexo);
            prefs.saveActividad(r.actividad);
            Double peso = Numeros.decimal(r.peso, PESO_MIN, PESO_MAX);
            if (peso != null) prefs.savePeso(peso);
            prefs.saveAltura(r.altura);
            prefs.saveEdad(r.edad);
        } else {
            prefs.borrar(PerfilCuenta.Campo.SEXO);
            prefs.borrar(PerfilCuenta.Campo.ACTIVIDAD);
            prefs.borrar(PerfilCuenta.Campo.PESO);
            prefs.borrar(PerfilCuenta.Campo.ALTURA);
            prefs.borrar(PerfilCuenta.Campo.EDAD);
        }
        if (!r.objetivo.isEmpty()) prefs.saveObjetivo(r.objetivo);
        if (!r.nivel.isEmpty()) prefs.saveNivel(NivelVisible.valor(r.nivel));
        if (!r.donde.isEmpty()) prefs.saveProgramasFiltros(r.donde, r.dias);
        DiaNutricion.objetivo(prefs);
    }

    /**
     * El alta o el cuestionario han terminado: el onboarding queda hecho para esta
     * cuenta, Inicio la recibe como primer día y el borrador se tira.
     */
    public static void terminar(@NonNull PreferencesManager prefs, @NonNull String usuario) {
        prefs.setOnboardingCompletado(true);
        prefs.setOnboardingCompletadoParaUsuario(usuario);
        prefs.marcarPrimerDia(usuario);
        prefs.limpiarBorradorOnboarding();
    }
}
