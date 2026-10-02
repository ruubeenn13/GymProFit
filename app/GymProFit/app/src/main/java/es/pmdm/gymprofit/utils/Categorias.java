package es.pmdm.gymprofit.utils;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import es.pmdm.gymprofit.R;

// ============================================================
// Categorias — la categoría de un alimento: su nombre y su icono (GP-175, lote 1.6.2)
//
// La API guarda y manda la clave canónica, en español («Carnes y aves»,
// AlimentoController.CATEGORIAS); la app la traduce aquí y le pone el icono de la tabla
// del diseño (documentacion/diseno/2026-10-02-nutricion/README.md). Lo que se manda a la
// API es siempre la clave, nunca el nombre traducido: si no, un alimento creado en inglés
// quedaría en una categoría que no existe (DEC-043).
// Una clave desconocida lleva el icono general y se escribe tal cual; sin categoría, «Otro».
// ============================================================
public final class Categorias {

    private Categorias() { }

    /** La categoría con que se guarda un alimento si no se elige ninguna. */
    public static final String OTRO = "Otro";

    /** Las catorce claves, en el orden de la API. */
    public static final List<String> CLAVES = Collections.unmodifiableList(Arrays.asList(
            "Carnes y aves", "Pescado y marisco", "Huevos", "Lácteos", "Legumbres",
            "Cereales y pan", "Frutas", "Verduras", "Frutos secos", "Aceites y grasas",
            "Bebidas", "Suplementos", "Snacks", OTRO));

    // En el mismo orden que CLAVES.
    private static final int[] ICONOS = {
            R.drawable.ic_ms_kebab_dining, R.drawable.ic_ms_set_meal, R.drawable.ic_ms_egg_alt,
            R.drawable.ic_ms_breakfast_dining, R.drawable.ic_ms_grain, R.drawable.ic_ms_bakery_dining,
            R.drawable.ic_ms_nutrition, R.drawable.ic_ms_eco, R.drawable.ic_ms_spa, R.drawable.ic_ms_opacity,
            R.drawable.ic_ms_local_cafe, R.drawable.ic_ms_pill, R.drawable.ic_ms_cookie, R.drawable.ic_ms_restaurant};

    private static final int[] NOMBRES = {
            R.string.categoria_carnes, R.string.categoria_pescado, R.string.categoria_huevos,
            R.string.categoria_lacteos, R.string.categoria_legumbres, R.string.categoria_cereales,
            R.string.categoria_frutas, R.string.categoria_verduras, R.string.categoria_frutos_secos,
            R.string.categoria_aceites, R.string.categoria_bebidas, R.string.categoria_suplementos,
            R.string.categoria_snacks, R.string.categoria_otro};

    private static int posicion(@Nullable String clave) {
        if (clave == null || clave.trim().isEmpty()) return CLAVES.indexOf(OTRO);
        return CLAVES.indexOf(clave.trim());
    }

    /** El icono de la categoría; el general si es desconocida o no tiene. */
    @DrawableRes
    public static int icono(@Nullable String clave) {
        int i = posicion(clave);
        return i < 0 ? R.drawable.ic_ms_restaurant : ICONOS[i];
    }

    /** El nombre traducido; 0 si la clave es desconocida (se escribe tal cual). */
    @StringRes
    public static int nombre(@Nullable String clave) {
        int i = posicion(clave);
        return i < 0 ? 0 : NOMBRES[i];
    }

    /** El nombre que se ve, en el idioma de la app. */
    @NonNull
    public static String nombre(@NonNull Context ctx, @Nullable String clave) {
        int id = nombre(clave);
        return id == 0 ? clave.trim() : ctx.getString(id);
    }

    /** Las bebidas se escriben por 100 ml en vez de por 100 g. */
    public static boolean esBebida(@Nullable String clave) {
        return "Bebidas".equals(clave);
    }
}
