package es.pmdm.gymprofit.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.button.MaterialButton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.alimento.Racion;
import es.pmdm.gymprofit.network.AlimentoApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.utils.AvisoDescartar;
import es.pmdm.gymprofit.utils.Categorias;
import es.pmdm.gymprofit.utils.EncajeDia;
import es.pmdm.gymprofit.utils.EtiquetaAlimento;
import es.pmdm.gymprofit.utils.EtiquetaAlimento.Campo;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.Numeros;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// CrearAlimentoActivity — crear o editar un alimento propio, como en la etiqueta
// (decisión 17, tablero 13, GP-175, lote 1.6.2)
//
// Arriba, el código si llega del escáner: se ve y no se cambia. Nombre (obligatorio),
// marca y la categoría en fichas, sin ninguna elegida (sin elegir, se guarda «Otro»);
// lo que viaja es siempre la clave canónica (Categorias). Las cifras van en el orden del
// envase, por 100 g (100 ml en «Bebidas») o por la ración si abajo hay una con sus gramos,
// y la app las pasa a 100 g, que es como las guarda la API (EtiquetaAlimento). El aviso
// de las calorías no impide guardar. «Así queda» enseña lo que se guarda, en la base que
// se esté escribiendo. Los errores salen en su campo, no en un toast.
// «Guardar y añadir a…» guarda y abre la ficha del alimento nuevo con su ración (o 100 g):
// nada se apunta sin ver cuánto. «Solo guardar» guarda y vuelve.
// Con EXTRA_EDITAR, la misma pantalla edita un alimento propio: «Editar alimento» y un
// solo botón, «Guardar cambios». Salir con algo sin guardar pregunta (AvisoDescartar).
// ============================================================
public class CrearAlimentoActivity extends BaseActivity {

    /** Extra: el código de barras con que se crea (del escáner). */
    public static final String EXTRA_CODIGO = "codigo";
    /** Extra: el id del alimento propio que se edita. */
    public static final String EXTRA_EDITAR = "editar";

    private static final String[] CLAVES_RACION = {"UNIDAD", "RACION", "ENVASE", "REBANADA"};
    private static final int[] NOMBRES_RACION = {R.string.crear_racion_unidad, R.string.crear_racion_racion,
            R.string.crear_racion_envase, R.string.crear_racion_rebanada};

    private final AlimentoApi alimentoApi = ApiClient.service(AlimentoApi.class);

    @Nullable private String codigo;
    private int editarId = -1;
    @Nullable private Alimento editando;
    @Nullable private String tipoComida;

    private EditText etNombre, etMarca, etGramosRacion;
    private final Map<Campo, EditText> cifras = new EnumMap<>(Campo.class);
    private final Map<Campo, TextView> errores = new EnumMap<>(Campo.class);
    private final Map<Campo, TextView> unidades = new EnumMap<>(Campo.class);
    private final List<View> fichasCategoria = new ArrayList<>();
    private final List<View> fichasRacion = new ArrayList<>();
    @Nullable private String categoria;
    @Nullable private String claveRacion;
    // Las cifras se escriben por 100 g (false) o por la ración (true).
    private boolean porRacion;
    // Los gramos de la ración con que se escribieron las cifras, por si la ración se quita.
    private double gramosBase = 100;
    private boolean guardando;
    private boolean pintando;
    @Nullable private String estadoInicial;
    private VistaEstado estado;

    private ActivityResultLauncher<Intent> fichaLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_alimento);

        codigo = getIntent().getStringExtra(EXTRA_CODIGO);
        editarId = getIntent().getIntExtra(EXTRA_EDITAR, -1);
        tipoComida = getIntent().getStringExtra(AnadirAlimentoActivity.EXTRA_TIPO);
        estado = new VistaEstado(findViewById(R.id.estadoCrear));

        etNombre = findViewById(R.id.etNombre);
        etMarca = findViewById(R.id.etMarca);
        etGramosRacion = findViewById(R.id.etGramosRacion);
        ((TextView) findViewById(R.id.tvRotuloMarca)).setText(conOpcional(R.string.crear_marca));
        ((TextView) findViewById(R.id.tvTituloRacion)).setText(conOpcional(R.string.crear_racion_titulo));

        construirCategorias();
        construirCifras();
        construirRaciones();
        findViewById(R.id.btnBaseCien).setOnClickListener(v -> cambiarBase(false));
        findViewById(R.id.btnBaseRacion).setOnClickListener(v -> cambiarBase(true));
        etNombre.addTextChangedListener(new Cambio(() -> ocultar(R.id.tvErrorNombre)));
        etGramosRacion.addTextChangedListener(new Cambio(() -> {
            ocultar(R.id.tvErrorRacion);
            pintar();
        }));

        // Lo que se añade desde la ficha del alimento creado cierra también esta pantalla.
        fichaLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            setResult(r.getResultCode() == RESULT_OK ? RESULT_OK : RESULT_CANCELED);
            finish();
        });

        // Salir con algo sin guardar pregunta antes de tirarlo (GP-108).
        AvisoDescartar.instalar(this, null, this::hayCambios);
        findViewById(R.id.btnCerrarCrear).setOnClickListener(v -> AvisoDescartar.salir(this, this::hayCambios));

        MaterialButton principal = findViewById(R.id.btnGuardarAnadir);
        MaterialButton solo = findViewById(R.id.btnSoloGuardar);
        if (editarId > 0) {
            ((TextView) findViewById(R.id.tvTituloCrear)).setText(R.string.crear_titulo_editar);
            principal.setText(R.string.crear_guardar_cambios);
            principal.setOnClickListener(v -> guardar(false));
            solo.setVisibility(View.GONE);
            cargarParaEditar();
        } else {
            if (tipoComida != null) {
                principal.setText(guardarYAnadir(tipoComida));
                principal.setOnClickListener(v -> guardar(true));
                solo.setOnClickListener(v -> guardar(false));
            } else {
                principal.setText(R.string.crear_solo_guardar);
                principal.setOnClickListener(v -> guardar(false));
                solo.setVisibility(View.GONE);
            }
            pintarCodigo(codigo, true);
            pintar();
            estadoInicial = foto();
        }
    }

    // ── Construir ───────────────────────────────────────────────────────────

    private CharSequence conOpcional(@StringRes int rotulo) {
        SpannableStringBuilder s = new SpannableStringBuilder(getString(rotulo)).append(' ');
        int desde = s.length();
        s.append(getString(R.string.crear_opcional));
        s.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.NORMAL), desde, s.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new ForegroundColorSpan(color(com.google.android.material.R.attr.colorOnSurfaceVariant)), desde,
                s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    private View ficha(ViewGroup grupo, @Nullable Integer icono, String texto) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_opcion_ficha, grupo, false);
        ((TextView) v.findViewById(R.id.tvOpcion)).setText(texto);
        if (icono != null) {
            ImageView iv = v.findViewById(R.id.ivIconoOpcion);
            iv.setImageResource(icono);
            iv.setVisibility(View.VISIBLE);
        }
        v.setContentDescription(texto);
        ViewCompat.setAccessibilityDelegate(v, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName("android.widget.RadioButton");
                info.setCheckable(true);
                info.setChecked(host.isSelected());
            }
        });
        return v;
    }

    private void construirCategorias() {
        LinearLayout grupo = findViewById(R.id.grupoCategorias);
        grupo.setContentDescription(getString(R.string.crear_categoria));
        float d = getResources().getDisplayMetrics().density;
        for (String clave : Categorias.CLAVES) {
            View f = ficha(grupo, Categorias.icono(clave), Categorias.nombre(this, clave));
            f.setTag(clave);
            f.setOnClickListener(v -> {
                // Tocar la elegida la quita: sin elegir, se guarda «Otro».
                elegirCategoria(clave.equals(categoria) ? null : clave, true);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (!fichasCategoria.isEmpty()) lp.setMarginStart(Math.round(8 * d));
            grupo.addView(f, lp);
            fichasCategoria.add(f);
        }
    }

    private void elegirCategoria(@Nullable String clave, boolean conMovimiento) {
        categoria = clave;
        for (View f : fichasCategoria) {
            boolean sel = f.getTag().equals(clave);
            if (sel && conMovimiento && !f.isSelected()) Movimiento.vibrar(f, Movimiento.Vibracion.LIGERA);
            f.setSelected(sel);
        }
        pintar();
    }

    private void construirCifras() {
        LinearLayout grupo = findViewById(R.id.grupoCifras);
        Campo[] orden = {Campo.ENERGIA, Campo.GRASAS, Campo.HIDRATOS, Campo.FIBRA, Campo.PROTEINAS};
        int[] nombres = {R.string.crear_energia, R.string.crear_grasas, R.string.crear_hidratos,
                R.string.crear_fibra, R.string.crear_proteinas};
        int[] puntos = {color(androidx.appcompat.R.attr.colorPrimary),
                ContextCompat.getColor(this, R.color.gp_macro_grasas), ContextCompat.getColor(this, R.color.gp_macro_carbos),
                color(com.google.android.material.R.attr.colorOutline), ContextCompat.getColor(this, R.color.gp_macro_proteinas)};
        for (int i = 0; i < orden.length; i++) {
            Campo c = orden[i];
            View fila = LayoutInflater.from(this).inflate(R.layout.item_campo_etiqueta, grupo, false);
            fila.findViewById(R.id.vPuntoCampo).setBackgroundTintList(ColorStateList.valueOf(puntos[i]));
            TextView nombre = fila.findViewById(R.id.tvNombreCampo);
            if (c == Campo.FIBRA) {
                SpannableStringBuilder s = new SpannableStringBuilder(getString(nombres[i])).append("  ");
                int desde = s.length();
                s.append(getString(R.string.crear_opcional_corto));
                s.setSpan(new ForegroundColorSpan(color(com.google.android.material.R.attr.colorOnSurfaceVariant)),
                        desde, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                s.setSpan(new android.text.style.RelativeSizeSpan(0.85f), desde, s.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                nombre.setText(s);
            } else {
                nombre.setText(nombres[i]);
            }
            EditText et = fila.findViewById(R.id.etCifra);
            et.setId(View.generateViewId());
            nombre.setLabelFor(et.getId());
            if (i == 0) fila.setBackground(null);
            et.addTextChangedListener(new Cambio(() -> {
                ocultarError(c);
                pintar();
            }));
            cifras.put(c, et);
            errores.put(c, fila.findViewById(R.id.tvErrorCampo));
            unidades.put(c, fila.findViewById(R.id.tvUnidadCampo));
            grupo.addView(fila);
        }
    }

    private void construirRaciones() {
        ViewGroup grupo = findViewById(R.id.grupoRaciones);
        grupo.setContentDescription(getString(R.string.crear_racion_nombre_a11y));
        for (int i = 0; i < CLAVES_RACION.length; i++) {
            String clave = CLAVES_RACION[i];
            View f = ficha(grupo, null, getString(NOMBRES_RACION[i]));
            f.setTag(clave);
            f.setOnClickListener(v -> elegirRacion(clave.equals(claveRacion) ? null : clave, true));
            grupo.addView(f);
            fichasRacion.add(f);
        }
    }

    private void elegirRacion(@Nullable String clave, boolean conMovimiento) {
        claveRacion = clave;
        for (View f : fichasRacion) {
            boolean sel = f.getTag().equals(clave);
            if (sel && conMovimiento && !f.isSelected()) Movimiento.vibrar(f, Movimiento.Vibracion.LIGERA);
            f.setSelected(sel);
        }
        findViewById(R.id.filaGramosRacion).setVisibility(clave == null ? View.GONE : View.VISIBLE);
        ocultar(R.id.tvErrorRacion);
        pintar();
    }

    // ── Base: por 100 g o por la ración ─────────────────────────────────────

    @Nullable
    private Double gramosRacion() {
        if (claveRacion == null) return null;
        Double g = Numeros.decimal(texto(etGramosRacion), 0, EtiquetaAlimento.MACRO_MAX * 20);
        return g == null || g <= 0 ? null : g;
    }

    // El conmutador cambia la base y reescribe las cifras en la nueva, para que digan lo mismo.
    private void cambiarBase(boolean aRacion) {
        if (aRacion == porRacion) return;
        Double g = gramosRacion();
        if (aRacion && g == null) return;
        double desde = porRacion ? gramosBase : 100;
        double hasta = aRacion ? g : 100;
        reescribir(desde, hasta);
        porRacion = aRacion;
        gramosBase = aRacion ? g : 100;
        Movimiento.vibrar(findViewById(R.id.conmutadorBase), Movimiento.Vibracion.LIGERA);
        pintar();
    }

    private void reescribir(double desde, double hasta) {
        pintando = true;
        for (Map.Entry<Campo, EditText> e : cifras.entrySet()) {
            Double v = leer(e.getKey());
            if (v == null) continue;
            // Lo que se ve va redondeado a un decimal; el valor exacto se guarda en el campo y
            // se usa mientras nadie lo toque, para que ir y volver no cambie lo que se guarda.
            double exacto = EtiquetaAlimento.convertir(v, desde, hasta);
            e.getValue().setText(formato(exacto));
            e.getValue().setTag(exacto);
        }
        pintando = false;
    }

    private String formato(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(FechaUtils.localeDeLaApp(this));
        nf.setMaximumFractionDigits(1);
        nf.setGroupingUsed(false);
        return nf.format(v);
    }

    // ── Pintar ──────────────────────────────────────────────────────────────

    private boolean esBebida() {
        return Categorias.esBebida(categoria);
    }

    private String nombreRacion() {
        for (int i = 0; i < CLAVES_RACION.length; i++) {
            if (CLAVES_RACION[i].equals(claveRacion)) return getString(NOMBRES_RACION[i]);
        }
        return "";
    }

    private void pintar() {
        if (pintando) return;
        String unidadMasa = getString(esBebida() ? R.string.crear_unidad_ml : R.string.crear_unidad_g);
        Double g = gramosRacion();

        // Si la ración desaparece mientras se escribe por ella, las cifras pasan a 100 g.
        if (porRacion && g == null) {
            reescribir(gramosBase, 100);
            porRacion = false;
            gramosBase = 100;
        } else if (porRacion && g != gramosBase) {
            // Los gramos de la ración cambian: las cifras siguen siendo «por la ración».
            gramosBase = g;
        }

        TextView cien = findViewById(R.id.btnBaseCien);
        TextView racion = findViewById(R.id.btnBaseRacion);
        String cienTexto = getString(esBebida() ? R.string.crear_100ml : R.string.crear_100g);
        cien.setText(cienTexto);
        cien.setSelected(!porRacion);
        String una = getString(R.string.crear_una, nombreRacion());
        racion.setText(una);
        racion.setVisibility(g != null ? View.VISIBLE : View.GONE);
        racion.setSelected(porRacion);
        marcarOpcion(cien, getString(R.string.crear_valores_por) + " " + cienTexto);
        marcarOpcion(racion, getString(R.string.crear_valores_por) + " " + una);

        String base = porRacion ? una : cienTexto;
        for (Map.Entry<Campo, EditText> e : cifras.entrySet()) {
            String u = e.getKey() == Campo.ENERGIA ? getString(R.string.crear_unidad_kcal) : getString(R.string.crear_unidad_g);
            unidades.get(e.getKey()).setText(u);
            TextView nombre = (TextView) ((ViewGroup) e.getValue().getParent().getParent()).findViewById(R.id.tvNombreCampo);
            e.getValue().setContentDescription(getString(R.string.crear_campo_a11y, nombre.getText(), u) + ", " + base);
        }
        ((TextView) findViewById(R.id.tvUnidadRacion)).setText(unidadMasa);
        ((TextView) findViewById(R.id.tvRacionSon)).setText(getString(R.string.crear_racion_son, nombreRacion()));
        etGramosRacion.setContentDescription(getString(R.string.crear_racion_son, nombreRacion()) + " " + unidadMasa);

        pintarAviso();
        pintarQueda(base, g, unidadMasa);
        pintarKcalRacion(g);
    }

    private void marcarOpcion(View v, String descripcion) {
        v.setContentDescription(descripcion);
        ViewCompat.setAccessibilityDelegate(v, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName("android.widget.RadioButton");
                info.setCheckable(true);
                info.setChecked(host.isSelected());
            }
        });
    }

    @Nullable
    private Double leer(Campo c) {
        EditText et = cifras.get(c);
        Double v = Numeros.decimal(texto(et), -Double.MAX_VALUE, Double.MAX_VALUE);
        if (v != null && et.getTag() instanceof Double && formato((Double) et.getTag()).equals(texto(et))) {
            return (Double) et.getTag();
        }
        return v;
    }

    private EtiquetaAlimento.Cifras escritas() {
        return new EtiquetaAlimento.Cifras(leer(Campo.ENERGIA), leer(Campo.GRASAS), leer(Campo.HIDRATOS),
                leer(Campo.FIBRA), leer(Campo.PROTEINAS));
    }

    // «Las calorías cuadran con los macros» o «Con esos macros serían unas 430 kcal»: solo
    // con la energía y los tres macros escritos. No impide guardar.
    private void pintarAviso() {
        TextView aviso = findViewById(R.id.tvAvisoKcal);
        Double e = leer(Campo.ENERGIA);
        Integer segun = EtiquetaAlimento.kcalSegunMacros(escritas());
        if (e == null || segun == null) {
            aviso.setVisibility(View.GONE);
            return;
        }
        int energia = (int) Math.round(e);
        boolean cuadra = EtiquetaAlimento.cuadra(energia, segun);
        int col = cuadra ? ContextCompat.getColor(this, R.color.gp_success)
                : color(com.google.android.material.R.attr.colorOnSurfaceVariant);
        aviso.setText(cuadra ? getString(R.string.crear_cuadra)
                : getString(R.string.crear_no_cuadra, formato(EtiquetaAlimento.redondeoAviso(segun))));
        aviso.setTextColor(col);
        android.graphics.drawable.Drawable ico = ContextCompat.getDrawable(this,
                cuadra ? R.drawable.ic_ms_check_circle_fill : R.drawable.ic_ms_info);
        if (ico != null) {
            ico = ico.mutate();
            int t = Math.round(18 * getResources().getDisplayMetrics().density);
            ico.setBounds(0, 0, t, t);
            ico.setTint(col);
        }
        aviso.setCompoundDrawablesRelative(ico, null, null, null);
        aviso.setVisibility(View.VISIBLE);
    }

    // «Así queda»: las cuatro cifras y el reparto, en la base que se escribe.
    private void pintarQueda(String base, @Nullable Double g, String unidadMasa) {
        NumberFormat nf = NumberFormat.getNumberInstance(FechaUtils.localeDeLaApp(this));
        nf.setMaximumFractionDigits(1);
        double kcal = valor(Campo.ENERGIA), p = valor(Campo.PROTEINAS), c = valor(Campo.HIDRATOS), gr = valor(Campo.GRASAS);
        ((TextView) findViewById(R.id.tvTituloQueda)).setText(porRacion && g != null
                ? getString(R.string.crear_queda_racion, base, nf.format(g) + " " + unidadMasa)
                : getString(R.string.crear_queda, base));
        cifra(R.id.quedaKcal, nf.format(Math.round(kcal)), R.string.ficha_kcal, 0);
        cifra(R.id.quedaProteina, getString(R.string.ficha_gramos_valor, nf.format(p)), etiquetaProteina(),
                ContextCompat.getColor(this, R.color.gp_macro_proteinas));
        cifra(R.id.quedaCarb, getString(R.string.ficha_gramos_valor, nf.format(c)), R.string.ficha_carb, 0);
        cifra(R.id.quedaGrasa, getString(R.string.ficha_gramos_valor, nf.format(gr)), R.string.ficha_grasa, 0);
        findViewById(R.id.cifrasQueda).setContentDescription(getString(R.string.crear_queda_a11y,
                nf.format(Math.round(kcal)), nf.format(p), nf.format(c), nf.format(gr)));

        View bloque = findViewById(R.id.repartoQueda);
        int[] r = EncajeDia.reparto(p, c, gr);
        if (r == null) {
            bloque.setVisibility(View.GONE);
            return;
        }
        bloque.setVisibility(View.VISIBLE);
        int[] tramos = {R.id.tramoQuedaProteina, R.id.tramoQuedaCarb, R.id.tramoQuedaGrasa};
        int[] colores = {R.color.gp_macro_proteinas, R.color.gp_macro_carbos, R.color.gp_macro_grasas};
        int[] textos = {R.string.ficha_reparto_proteina, R.string.ficha_reparto_carb, R.string.ficha_reparto_grasa};
        SpannableStringBuilder leyenda = new SpannableStringBuilder();
        for (int i = 0; i < 3; i++) {
            View tramo = findViewById(tramos[i]);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tramo.getLayoutParams();
            lp.weight = r[i];
            tramo.setLayoutParams(lp);
            tramo.setVisibility(r[i] == 0 ? View.GONE : View.VISIBLE);
            if (i > 0) leyenda.append("   ");
            int desde = leyenda.length();
            leyenda.append("●");
            leyenda.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, colores[i])), desde, leyenda.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            leyenda.append(" ").append(getString(textos[i], r[i]).replace(" ", " "));
        }
        ((TextView) findViewById(R.id.tvLeyendaQueda)).setText(leyenda);
        bloque.setContentDescription(getString(R.string.ficha_reparto_a11y, r[0], r[1], r[2]));
    }

    private double valor(Campo c) {
        Double v = leer(c);
        return v == null || v < 0 ? 0 : v;
    }

    // Al lado de los gramos de la ración, las kcal de una.
    private void pintarKcalRacion(@Nullable Double g) {
        TextView tv = findViewById(R.id.tvKcalRacion);
        Double e = leer(Campo.ENERGIA);
        if (g == null || e == null) {
            tv.setText("");
            return;
        }
        double porUna = porRacion ? e : EtiquetaAlimento.convertir(e, 100, g);
        tv.setText(getString(R.string.crear_racion_kcal, formato(Math.round(porUna))));
    }

    private void cifra(int id, String valor, int etiqueta, int color) {
        View v = findViewById(id);
        TextView tv = v.findViewById(R.id.tvValorCifra);
        tv.setText(valor);
        if (color != 0) tv.setTextColor(color);
        ((TextView) v.findViewById(R.id.tvEtiquetaCifra)).setText(etiqueta);
    }

    // Con letra grande, «proteína» no cabe en su cuarto de fila: «prot.» (como la ficha).
    private int etiquetaProteina() {
        return getResources().getConfiguration().fontScale >= 1.3f ? R.string.ficha_proteina_corta : R.string.ficha_proteina;
    }

    // El código: «8 400000 720021», como en el envase. Del escáner, con «Leído».
    private void pintarCodigo(@Nullable String c, boolean leido) {
        if (c == null || c.isEmpty()) return;
        String visto = c.length() == 13 ? c.charAt(0) + " " + c.substring(1, 7) + " " + c.substring(7)
                : c.length() == 8 ? c.substring(0, 4) + " " + c.substring(4) : c;
        ((TextView) findViewById(R.id.tvCodigoCrear)).setText(visto);
        findViewById(R.id.tvCodigoLeido).setVisibility(leido ? View.VISIBLE : View.GONE);
        View fila = findViewById(R.id.filaCodigo);
        fila.setContentDescription(getString(leido ? R.string.crear_codigo_a11y : R.string.crear_codigo_a11y_editar, c));
        fila.setVisibility(View.VISIBLE);
    }

    // ── Editar ──────────────────────────────────────────────────────────────

    private void cargarParaEditar() {
        estado.cargando();
        findViewById(R.id.scrollCrear).setVisibility(View.INVISIBLE);
        alimentoApi.porId(editarId).enqueue(new ApiCallback<Alimento>() {
            @Override
            public void onOk(Alimento a) {
                if (isDestroyed()) return;
                if (a == null) {
                    onFail(404, null);
                    return;
                }
                editando = a;
                rellenar(a);
                estado.oculto();
                findViewById(R.id.scrollCrear).setVisibility(View.VISIBLE);
                estadoInicial = foto();
            }

            @Override
            public void onFail(int code, String message) {
                if (isDestroyed()) return;
                estado.error(VistaEstado.mensaje(CrearAlimentoActivity.this, R.string.crear_error_carga, code, message),
                        CrearAlimentoActivity.this::cargarParaEditar);
            }
        });
    }

    private void rellenar(Alimento a) {
        pintando = true;
        etNombre.setText(a.getNombre());
        etMarca.setText(a.getMarca());
        poner(Campo.ENERGIA, a.getCalorias());
        poner(Campo.GRASAS, a.getGrasas());
        poner(Campo.HIDRATOS, a.getCarbohidratos());
        if (a.getFibra() != null) poner(Campo.FIBRA, a.getFibra());
        poner(Campo.PROTEINAS, a.getProteinas());
        pintando = false;
        String cat = a.getCategoria();
        elegirCategoria(cat != null && Categorias.CLAVES.contains(cat) ? cat : null, false);
        // La elegida, a la vista: la fila se desliza hasta ella.
        for (View f : fichasCategoria) {
            if (!f.isSelected()) continue;
            View fila = (View) f.getParent().getParent();
            fila.post(() -> fila.scrollTo(Math.max(0, f.getLeft() - Math.round(16 * getResources().getDisplayMetrics().density)), 0));
        }
        // Las raciones del catálogo no se tocan desde aquí (A2): solo en lo propio.
        findViewById(R.id.cardRacion).setVisibility(a.esPropio() ? View.VISIBLE : View.GONE);
        for (Racion r : a.getRaciones()) {
            if (r.getClave() != null) {
                elegirRacion(r.getClave(), false);
                etGramosRacion.setText(formato(r.getGramos()));
                break;
            }
        }
        pintarCodigo(a.getBarcode(), false);
        pintar();
    }

    // La cifra guardada, con su valor exacto aparte (ver reescribir).
    private void poner(Campo c, double v) {
        cifras.get(c).setText(formato(v));
        cifras.get(c).setTag(v);
    }

    // ── Guardar ─────────────────────────────────────────────────────────────

    private void guardar(boolean yAnadir) {
        if (guardando) return;
        boolean bien = true;
        String nombre = texto(etNombre).trim();
        if (nombre.isEmpty()) {
            mostrar(R.id.tvErrorNombre, getString(R.string.crear_error_nombre));
            bien = false;
        }
        // Lo que no se entiende como número se marca en su campo.
        for (Map.Entry<Campo, EditText> e : cifras.entrySet()) {
            if (!texto(e.getValue()).trim().isEmpty() && leer(e.getKey()) == null) {
                error(e.getKey(), getString(R.string.crear_error_numero));
                bien = false;
            }
        }
        Double g = gramosRacion();
        if (claveRacion != null && (g == null || g > 2000)) {
            mostrar(R.id.tvErrorRacion, getString(R.string.crear_error_racion));
            bien = false;
        }
        EtiquetaAlimento.Resultado r = EtiquetaAlimento.comprobar(escritas(), porRacion ? gramosBase : 100);
        for (Map.Entry<Campo, EtiquetaAlimento.Error> e : r.errores.entrySet()) {
            if (errores.get(e.getKey()).getVisibility() == View.VISIBLE) continue;
            error(e.getKey(), getString(textoError(e.getValue())));
            bien = false;
        }
        if (!bien) {
            Movimiento.vibrar(findViewById(R.id.btnGuardarAnadir), Movimiento.Vibracion.ERROR);
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("nombre", nombre);
        body.put("marca", texto(etMarca).trim());
        body.put("categoria", categoria != null ? categoria : Categorias.OTRO);
        body.put("calorias", r.calorias);
        body.put("proteinas", exacto(r.proteinas));
        body.put("carbohidratos", exacto(r.carbohidratos));
        body.put("grasas", exacto(r.grasas));
        if (r.fibra != null) body.put("fibra", exacto(r.fibra));
        boolean propio = editando == null || editando.esPropio();
        if (propio) {
            List<Map<String, Object>> raciones = new ArrayList<>();
            if (claveRacion != null && g != null) {
                Map<String, Object> una = new HashMap<>();
                una.put("unidad", claveRacion);
                una.put("gramos", exacto(g));
                raciones.add(una);
            }
            // Al crear, sin ración no se manda nada; al editar, la lista vacía la quita.
            if (!raciones.isEmpty() || editando != null) body.put("raciones", raciones);
        }
        enCurso(true);
        if (editando != null) {
            alimentoApi.patch(editando.getId(), body).enqueue(new ApiCallback<Void>() {
                @Override
                public void onOk(Void ignorado) {
                    if (isDestroyed()) return;
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onFail(int code, String message) {
                    if (!isDestroyed()) falloAlGuardar(code, message);
                }
            });
            return;
        }
        body.put("usuarioId", prefsManager.getUsuarioId());
        if (codigo != null && !codigo.isEmpty()) body.put("barcode", codigo);
        alimentoApi.crear(body).enqueue(new ApiCallback<Alimento>() {
            @Override
            public void onOk(Alimento creado) {
                if (isDestroyed()) return;
                if (yAnadir && creado != null && creado.getId() > 0 && tipoComida != null) {
                    // A la ficha del alimento creado, con su ración: no se apunta nada sin
                    // ver cuánto.
                    fichaLauncher.launch(FichaAlimentoActivity.paraAnadir(CrearAlimentoActivity.this, creado.getId(),
                            tipoComida, getIntent().getStringExtra(AnadirAlimentoActivity.EXTRA_FECHA)));
                    return;
                }
                // «Solo guardar»: se vuelve a donde se estaba, que recarga y lo enseña.
                setResult(RESULT_CANCELED);
                finish();
            }

            @Override
            public void onFail(int code, String message) {
                if (!isDestroyed()) falloAlGuardar(code, message);
            }
        });
    }

    private void falloAlGuardar(int code, String message) {
        enCurso(false);
        if (code == 409) {
            // Ya tiene un alimento con ese código (único por dueño): se dice en el código.
            mostrar(R.id.tvErrorCodigo, getString(R.string.crear_alimento_codigo_en_uso));
            return;
        }
        UiFeedback.toastError(this, code, message);
    }

    private void enCurso(boolean si) {
        guardando = si;
        MaterialButton principal = findViewById(R.id.btnGuardarAnadir);
        principal.setEnabled(!si);
        findViewById(R.id.btnSoloGuardar).setEnabled(!si);
        if (si) {
            principal.setTag(principal.getText());
            principal.setText(R.string.crear_guardando);
        } else if (principal.getTag() instanceof CharSequence) {
            principal.setText((CharSequence) principal.getTag());
        }
    }

    private static BigDecimal exacto(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    @StringRes
    private static int textoError(EtiquetaAlimento.Error e) {
        switch (e) {
            case FALTA: return R.string.crear_error_energia;
            case KJ:    return R.string.crear_error_kj;
            case SUMA:  return R.string.crear_error_suma;
            default:    return R.string.crear_error_fuera;
        }
    }

    @StringRes
    private static int guardarYAnadir(String tipo) {
        switch (tipo) {
            case "DESAYUNO": return R.string.crear_guardar_anadir_desayuno;
            case "ALMUERZO": return R.string.crear_guardar_anadir_almuerzo;
            case "COMIDA":   return R.string.crear_guardar_anadir_comida;
            case "CENA":     return R.string.crear_guardar_anadir_cena;
            default:         return R.string.crear_guardar_anadir_merienda;
        }
    }

    // ── Errores en su campo ─────────────────────────────────────────────────

    private void error(Campo c, String texto) {
        TextView tv = errores.get(c);
        tv.setText(texto);
        tv.setVisibility(View.VISIBLE);
        Movimiento.temblar((View) cifras.get(c).getParent());
    }

    private void ocultarError(Campo c) {
        errores.get(c).setVisibility(View.GONE);
    }

    private void mostrar(int id, String texto) {
        TextView tv = findViewById(id);
        tv.setText(texto);
        tv.setVisibility(View.VISIBLE);
    }

    private void ocultar(int id) {
        findViewById(id).setVisibility(View.GONE);
    }

    // ── Descartar ───────────────────────────────────────────────────────────

    // Lo que hay escrito y elegido, para saber al salir si algo ha cambiado.
    private String foto() {
        List<String> partes = new ArrayList<>();
        partes.add(texto(etNombre));
        partes.add(texto(etMarca));
        partes.add(String.valueOf(categoria));
        for (EditText e : cifras.values()) partes.add(texto(e));
        partes.add(String.valueOf(claveRacion));
        partes.add(texto(etGramosRacion));
        return String.join("\u0001", Collections.unmodifiableList(partes));
    }

    private boolean hayCambios() {
        return !guardando && estadoInicial != null && !estadoInicial.equals(foto());
    }

    private static String texto(EditText e) {
        return e.getText() != null ? e.getText().toString() : "";
    }

    private int color(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    /** Un TextWatcher que solo avisa después de cada cambio. */
    private static final class Cambio implements TextWatcher {
        private final Runnable tras;

        Cambio(Runnable tras) {
            this.tras = tras;
        }

        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

        @Override
        public void afterTextChanged(Editable s) {
            tras.run();
        }
    }

    /** Para abrir esta pantalla editando un alimento propio. */
    public static Intent paraEditar(Context ctx, int alimentoId) {
        return new Intent(ctx, CrearAlimentoActivity.class).putExtra(EXTRA_EDITAR, alimentoId);
    }
}
