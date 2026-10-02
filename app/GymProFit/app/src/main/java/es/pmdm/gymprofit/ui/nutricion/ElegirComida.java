package es.pmdm.gymprofit.ui.nutricion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.ComidaQueToca;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// ElegirComida — la etiqueta de la comida y la hoja «¿A qué comida?» (decisión 15,
// tablero 4, lote 1.6.2)
//
// La etiqueta va a la derecha del título: icono de la comida, su nombre y la flecha,
// 36 dp de alto dentro de 48 de toque, con borde y sin relleno, para no competir con el
// título. Si no cabe (letra grande, idioma largo), baja a una segunda línea debajo del
// título: nunca se recorta.
// La hoja enseña el día, y en cada comida lo que ya lleva o «Sin apuntar»; «· ahora toca»
// solo en la que toca por la hora y solo si el día es hoy. La elegida va con fondo y un
// check que salta. Tocar una fila elige y cierra; tocar fuera cierra sin cambiar nada.
// Para TalkBack es un grupo de botones de opción.
// Es la misma pieza para Añadir y, cuando lleguen, Favoritos y Mis platos.
// ============================================================
public final class ElegirComida {

    private ElegirComida() { }

    /** Recibe la comida elegida (DESAYUNO…CENA). */
    public interface Elegida { void elegida(@NonNull String tipo); }

    // Iconos del README del diseño. El desayuno es free_breakfast, que en Material Symbols
    // es el mismo glifo que local_cafe (código eb44); el paquete de SVG solo trae ese.
    private static final int[] ICONOS = {R.drawable.ic_ms_local_cafe, R.drawable.ic_ms_nutrition,
            R.drawable.ic_ms_restaurant, R.drawable.ic_ms_cookie, R.drawable.ic_ms_dinner_dining};
    private static final int[] ANADIDO_A = {R.string.anadido_a_desayuno, R.string.anadido_a_almuerzo,
            R.string.anadido_a_comida, R.string.anadido_a_merienda, R.string.anadido_a_cena};

    private static int indice(String tipo) {
        for (int i = 0; i < ComidaQueToca.TIPOS.length; i++) if (ComidaQueToca.TIPOS[i].equals(tipo)) return i;
        return 3;
    }

    /** El icono de una comida. */
    @DrawableRes
    public static int icono(String tipo) {
        return ICONOS[indice(tipo)];
    }

    /** Las kcal de una comida del día, o null si no hay datos o está sin apuntar. */
    @Nullable
    static Integer kcal(@Nullable Map<String, Integer> kcal, String tipo) {
        if (kcal == null) return null;
        Integer k = kcal.get(tipo);
        return k == null || k <= 0 ? null : k;
    }

    /** «· ahora toca»: solo en la comida que toca, y solo si el día es hoy. */
    static boolean tocaAhora(String tipo, boolean esHoy, String queToca) {
        return esHoy && tipo.equals(queToca);
    }

    /** «Añadido a la cena». */
    @StringRes
    public static int anadidoA(String tipo) {
        return ANADIDO_A[indice(tipo)];
    }

    /** El aviso al volver si se añadió a otra comida que la de origen; null si es la misma. */
    @Nullable
    public static Integer avisoAlVolver(String desde, String a) {
        return desde.equals(a) ? null : anadidoA(a);
    }

    // ── La etiqueta ─────────────────────────────────────────────────────────

    /** Pinta la etiqueta (view_etiqueta_comida) con la comida. */
    public static void pintarEtiqueta(@NonNull View etiqueta, @NonNull String tipo) {
        Context ctx = etiqueta.getContext();
        String nombre = ctx.getString(ComidaQueToca.titulo(tipo));
        ((ImageView) etiqueta.findViewById(R.id.ivIconoEtiqueta)).setImageResource(icono(tipo));
        ((TextView) etiqueta.findViewById(R.id.tvEtiquetaComida)).setText(nombre);
        etiqueta.setContentDescription(ctx.getString(R.string.etiqueta_comida_a11y, nombre));
    }

    /**
     * Pone la etiqueta a la derecha del título si cabe en la línea; si no, debajo. Se
     * vuelve a decidir cada vez que cambia el ancho (giro, ventana partida).
     *
     * @param fila    la línea del título, que lleva la ranura de la derecha.
     * @param derecha ranura junto al título.
     * @param debajo  ranura de la segunda línea.
     */
    public static void colocarEtiqueta(@NonNull View etiqueta, @NonNull ViewGroup fila, @NonNull TextView titulo,
                                       @NonNull ViewGroup derecha, @NonNull ViewGroup debajo) {
        fila.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l == or - ol && etiqueta.getParent() != null) return;
            fila.post(() -> decidir(etiqueta, fila, titulo, derecha, debajo));
        });
    }

    private static void decidir(View etiqueta, ViewGroup fila, TextView titulo, ViewGroup derecha, ViewGroup debajo) {
        etiqueta.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        float textoTitulo = titulo.getPaint().measureText(titulo.getText().toString())
                + titulo.getPaddingStart() + titulo.getPaddingEnd();
        int libre = fila.getWidth() - fila.getPaddingStart() - fila.getPaddingEnd() - (titulo.getLeft() - fila.getPaddingStart());
        boolean cabe = textoTitulo + etiqueta.getMeasuredWidth() <= libre;
        ViewGroup destino = cabe ? derecha : debajo;
        if (etiqueta.getParent() == destino) return;
        if (etiqueta.getParent() != null) ((ViewGroup) etiqueta.getParent()).removeView(etiqueta);
        destino.addView(etiqueta);
        debajo.setVisibility(cabe ? View.GONE : View.VISIBLE);
    }

    // ── La hoja ─────────────────────────────────────────────────────────────

    /**
     * Abre «¿A qué comida?».
     *
     * @param fecha  el día al que se añade (yyyy-MM-dd), o null para hoy.
     * @param actual la comida elegida ahora.
     * @param kcal   lo que lleva cada comida ese día, o null si aún no se sabe (cargando o
     *               fallo): entonces las filas solo dicen su nombre.
     */
    public static void abrirHoja(@NonNull Context ctx, @Nullable String fecha, @NonNull String actual,
                                 @Nullable Map<String, Integer> kcal, @NonNull Elegida alElegir) {
        BottomSheetDialog hoja = new BottomSheetDialog(ctx);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_elegir_comida, null, false);
        boolean esHoy = esHoy(fecha);
        ((TextView) v.findViewById(R.id.tvDiaHoja)).setText(dia(ctx, fecha, esHoy));

        LinearLayout grupo = v.findViewById(R.id.grupoComidasHoja);
        ViewCompat.setAccessibilityDelegate(grupo, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setCollectionInfo(AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(
                        ComidaQueToca.TIPOS.length, 1, false,
                        AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_SINGLE));
            }
        });
        String queToca = ComidaQueToca.ahora();
        NumberFormat nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(ctx));
        View checkElegida = null;
        LayoutInflater inflater = LayoutInflater.from(ctx);
        for (int i = 0; i < ComidaQueToca.TIPOS.length; i++) {
            String tipo = ComidaQueToca.TIPOS[i];
            boolean elegida = tipo.equals(actual);
            View fila = inflater.inflate(R.layout.item_comida_hoja, grupo, false);
            String nombre = ctx.getString(ComidaQueToca.titulo(tipo));
            ((TextView) fila.findViewById(R.id.tvNombreComidaHoja)).setText(nombre);
            ImageView ico = fila.findViewById(R.id.ivIconoComidaHoja);
            ico.setImageResource(icono(tipo));
            ico.setImageTintList(android.content.res.ColorStateList.valueOf(color(ctx, elegida
                    ? com.google.android.material.R.attr.colorOnPrimaryContainer
                    : com.google.android.material.R.attr.colorOnSurfaceVariant)));

            TextView detalle = fila.findViewById(R.id.tvDetalleComidaHoja);
            String texto = detalle(ctx, kcal, tipo, nf);
            boolean toca = tocaAhora(tipo, esHoy, queToca);
            if (toca) {
                texto = texto == null ? ctx.getString(R.string.comida_ahora_toca)
                        : ctx.getString(R.string.comida_resumen, texto, ctx.getString(R.string.comida_ahora_toca));
                detalle.setTextColor(color(ctx, com.google.android.material.R.attr.colorOnPrimaryContainer));
            }
            detalle.setText(texto);
            detalle.setVisibility(texto == null ? View.GONE : View.VISIBLE);

            fila.setBackgroundResource(elegida ? R.drawable.bg_comida_hoja_elegida : R.drawable.bg_comida_hoja);
            View check = fila.findViewById(R.id.ivCheckComidaHoja);
            check.setVisibility(elegida ? View.VISIBLE : View.INVISIBLE);
            if (elegida) checkElegida = check;
            ViewCompat.setAccessibilityDelegate(fila, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName("android.widget.RadioButton");
                    info.setCheckable(true);
                    info.setChecked(elegida);
                }
            });
            fila.setContentDescription(texto == null ? nombre : nombre + ". " + texto);
            fila.setOnClickListener(x -> {
                Movimiento.vibrar(x, Movimiento.Vibracion.LIGERA);
                hoja.dismiss();
                if (!elegida) alElegir.elegida(tipo);
            });
            grupo.addView(fila);
        }

        hoja.setContentView(v);
        BottomSheetBehavior<?> b = hoja.getBehavior();
        b.setSkipCollapsed(true);
        b.setState(BottomSheetBehavior.STATE_EXPANDED);
        // La hoja sube con el momento 17 (HOJA) y el check de la elegida salta (CHECK).
        if (hoja.getWindow() != null) hoja.getWindow().setWindowAnimations(0);
        View check = checkElegida;
        hoja.setOnShowListener(d -> {
            View contenedor = hoja.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (contenedor != null && !Movimiento.quieto(ctx)) {
                contenedor.setTranslationY(contenedor.getHeight());
                contenedor.animate().translationY(0f).setDuration(Movimiento.HOJA)
                        .setInterpolator(Movimiento.ENFATIZADA).start();
            }
            if (check != null) {
                check.setScaleX(0.4f);
                check.setScaleY(0.4f);
                check.animate().scaleX(1f).scaleY(1f).setStartDelay(Movimiento.HOJA / 2)
                        .setDuration(Movimiento.CHECK).setInterpolator(Movimiento.REBOTE).start();
            }
        });
        hoja.show();
    }

    @Nullable
    private static String detalle(Context ctx, @Nullable Map<String, Integer> kcal, String tipo, NumberFormat nf) {
        if (kcal == null) return null;
        Integer k = kcal(kcal, tipo);
        return k == null ? ctx.getString(R.string.hoja_comida_sin_apuntar)
                : ctx.getString(R.string.nutricion_kcal_valor, nf.format(k));
    }

    /** «Hoy, jueves 1 de octubre»; otro día, «Martes, 29 de septiembre». */
    @NonNull
    public static String textoDia(@NonNull Context ctx, @Nullable String fecha) {
        return dia(ctx, fecha, esHoy(fecha));
    }

    /** Si el día (yyyy-MM-dd, o null) es hoy. */
    public static boolean esHoy(@Nullable String fecha) {
        if (fecha == null) return true;
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        return hoy.equals(fecha);
    }

    private static String dia(Context ctx, @Nullable String fecha, boolean esHoy) {
        Locale idioma = FechaUtils.localeDeLaApp(ctx);
        Date d = new Date();
        if (fecha != null) {
            try {
                d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(fecha);
            } catch (ParseException e) {
                // Una fecha ilegible no llega nunca (la pone el diario); si llegara, se
                // enseña tal cual en vez de inventar un día.
                return fecha;
            }
        }
        if (esHoy) {
            return ctx.getString(R.string.hoja_comida_hoy,
                    new SimpleDateFormat(ctx.getString(R.string.hoja_comida_patron), idioma).format(d));
        }
        String s = new SimpleDateFormat(ctx.getString(R.string.home_fecha_patron), idioma).format(d);
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(idioma) + s.substring(1);
    }

    private static int color(Context ctx, int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        ctx.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
