package es.pmdm.gymprofit.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.model.comida.AnadirRespuesta;
import es.pmdm.gymprofit.model.comida.Comida;
import es.pmdm.gymprofit.network.AlimentoApi;
import es.pmdm.gymprofit.network.AlimentoComidaApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.utils.CantidadFicha;
import es.pmdm.gymprofit.utils.DiaNutricion;
import es.pmdm.gymprofit.utils.EncajeDia;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.Numeros;
import es.pmdm.gymprofit.utils.PedidoCantidad;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// FichaAlimentoActivity — la ficha del alimento y su cantidad (tablero 7, lote 1.6.1)
//
// Para añadir (desde Añadir, la búsqueda o el escáner) o para editar una línea de una
// comida (desde ComidaActivity), sustituye al diálogo de gramos de las dos pantallas.
//   · Nombre, de dónde es (básico de Ciqual o USDA, producto, tuyo) y la insignia azul.
//   · Las cuatro cifras de la cantidad elegida y el reparto de sus kcal entre los tres
//     macros (las barras se llenan con BARRA).
//   · «Cómo encaja en tu día»: de lo que llevas hoy a lo que llevarías, en kcal y
//     proteína, y lo que te quedará; sin objetivo en el perfil, no sale (EncajeDia).
//   · La información por 100 g, plegada, y «Reportar un problema», con sus motivos.
//   · Abajo, la cantidad (CantidadFicha): − y + de uno en uno en raciones y de 25 en 25
//     en gramos; tocar la cifra la deja escribir. La unidad entre sus raciones y gramos
//     (con más de dos, un selector). «Añadir a la merienda» va por POST /comidas/anadir
//     y vuelve al diario; al editar, «Actualizar» va por el PATCH de la línea.
// ============================================================
public class FichaAlimentoActivity extends BaseActivity {

    private static final String EXTRA_ALIMENTO = "alimento";
    private static final String EXTRA_ALIMENTO_ID = "alimentoId";
    private static final String EXTRA_LINEA_ID = "lineaId";
    private static final String EXTRA_RACION_ID = "racionId";
    private static final String EXTRA_RACIONES = "raciones";
    private static final String EXTRA_GRAMOS = "gramos";
    private static final String EXTRA_KCAL_LINEA = "kcalLinea";
    private static final String EXTRA_PROT_LINEA = "protLinea";

    private final AlimentoApi alimentoApi = ApiClient.service(AlimentoApi.class);
    private final ComidaApi comidaApi = ApiClient.service(ComidaApi.class);
    private final AlimentoComidaApi lineaApi = ApiClient.service(AlimentoComidaApi.class);

    private String tipoComida;
    private String fecha;
    private int lineaId = -1;
    @Nullable private Alimento alimento;
    @Nullable private CantidadFicha cantidad;
    @Nullable private Bundle estadoGuardado;

    // Lo que llevas hoy, para «Cómo encaja»; null mientras no se sabe (o si no hay objetivo).
    @Nullable private DiaNutricion dia;
    private boolean infoAbierta;

    private VistaEstado estado;
    private NumberFormat nf;

    /** Para añadir un alimento que ya se tiene (de la lista o del escáner). */
    @NonNull
    public static Intent paraAnadir(@NonNull Context ctx, @NonNull Alimento alimento,
                                    @NonNull String tipoComida, @Nullable String fecha) {
        return new Intent(ctx, FichaAlimentoActivity.class)
                .putExtra(EXTRA_ALIMENTO, new Gson().toJson(alimento))
                .putExtra(AnadirAlimentoActivity.EXTRA_TIPO, tipoComida)
                .putExtra(AnadirAlimentoActivity.EXTRA_FECHA, fecha);
    }

    /** Para añadir un alimento del que solo se sabe el id (el recién creado). */
    @NonNull
    public static Intent paraAnadir(@NonNull Context ctx, int alimentoId, @NonNull String tipoComida,
                                    @Nullable String fecha) {
        return new Intent(ctx, FichaAlimentoActivity.class)
                .putExtra(EXTRA_ALIMENTO_ID, alimentoId)
                .putExtra(AnadirAlimentoActivity.EXTRA_TIPO, tipoComida)
                .putExtra(AnadirAlimentoActivity.EXTRA_FECHA, fecha);
    }

    /** Para editar la cantidad de una línea de una comida. */
    @NonNull
    public static Intent paraEditar(@NonNull Context ctx, @NonNull AlimentoComida linea,
                                    @NonNull String tipoComida, @Nullable String fecha) {
        Intent i = new Intent(ctx, FichaAlimentoActivity.class)
                .putExtra(EXTRA_ALIMENTO_ID, linea.getAlimentoId())
                .putExtra(EXTRA_LINEA_ID, linea.getId())
                .putExtra(EXTRA_GRAMOS, linea.getCantidadGramos())
                .putExtra(EXTRA_KCAL_LINEA, (double) linea.getCaloriasTotales())
                .putExtra(EXTRA_PROT_LINEA, linea.getProteinasTotales())
                .putExtra(AnadirAlimentoActivity.EXTRA_TIPO, tipoComida)
                .putExtra(AnadirAlimentoActivity.EXTRA_FECHA, fecha);
        if (linea.getRacionId() != null && linea.getRaciones() != null) {
            i.putExtra(EXTRA_RACION_ID, linea.getRacionId().intValue());
            i.putExtra(EXTRA_RACIONES, linea.getRaciones().doubleValue());
        }
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ficha_alimento);
        nf = NumberFormat.getNumberInstance(FechaUtils.localeDeLaApp(this));
        nf.setMaximumFractionDigits(1);
        estadoGuardado = savedInstanceState;

        tipoComida = getIntent().getStringExtra(AnadirAlimentoActivity.EXTRA_TIPO);
        if (tipoComida == null) tipoComida = "MERIENDA";
        fecha = getIntent().getStringExtra(AnadirAlimentoActivity.EXTRA_FECHA);
        lineaId = getIntent().getIntExtra(EXTRA_LINEA_ID, -1);

        findViewById(R.id.btnAtrasFicha).setOnClickListener(v -> finish());
        estado = new VistaEstado(findViewById(R.id.estadoFicha));
        findViewById(R.id.btnInfo100).setOnClickListener(v -> alternarInfo());
        findViewById(R.id.btnReportar).setOnClickListener(v -> reportar());
        findViewById(R.id.btnMenos).setOnClickListener(v -> { if (cantidad != null) { cantidad.menos(); pintarCantidad(); } });
        findViewById(R.id.btnMas).setOnClickListener(v -> { if (cantidad != null) { cantidad.mas(); pintarCantidad(); } });
        findViewById(R.id.tvCantidad).setOnClickListener(v -> escribirCantidad());
        ((MaterialButton) findViewById(R.id.btnAnadirFicha)).setText(lineaId > 0 ? getString(R.string.ficha_actualizar)
                : getString(AnadirAlimentoActivity.textoAnadirA(tipoComida)));
        findViewById(R.id.btnAnadirFicha).setOnClickListener(v -> guardar());

        String json = getIntent().getStringExtra(EXTRA_ALIMENTO);
        if (json != null) {
            mostrar(new Gson().fromJson(json, Alimento.class));
        } else {
            cargarAlimento();
        }
        cargarDia();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (cantidad != null) {
            out.putInt("unidad", cantidad.elegida());
            out.putDouble("valor", cantidad.valor());
        }
        out.putBoolean("info", infoAbierta);
    }

    // ── Cargar ──────────────────────────────────────────────────────────────

    private void cargarAlimento() {
        int id = getIntent().getIntExtra(EXTRA_ALIMENTO_ID, -1);
        estado.cargando();
        alimentoApi.porId(id).enqueue(new ApiCallback<Alimento>() {
            @Override
            public void onOk(Alimento a) {
                if (isDestroyed()) return;
                if (a == null) {
                    estado.error(getString(R.string.ficha_error), FichaAlimentoActivity.this::cargarAlimento);
                    return;
                }
                mostrar(a);
            }

            @Override
            public void onFail(int code, String message) {
                if (isDestroyed()) return;
                estado.error(VistaEstado.mensaje(FichaAlimentoActivity.this, R.string.ficha_error, code, message),
                        FichaAlimentoActivity.this::cargarAlimento);
            }
        });
    }

    // Lo que llevas hoy, para «Cómo encaja en tu día».
    private void cargarDia() {
        if (fecha == null || DiaNutricion.objetivo(prefsManager) == null) return;
        comidaApi.getDeUsuarioFecha(prefsManager.getUsuarioId(), fecha).enqueue(new ApiCallback<List<Comida>>() {
            @Override
            public void onOk(List<Comida> comidas) {
                if (isDestroyed()) return;
                dia = DiaNutricion.de(comidas, DiaNutricion.objetivo(prefsManager));
                pintarEncaje();
            }

            @Override
            public void onFail(int code, String message) {
                // «Cómo encaja» es una ayuda, no lo que se hace aquí: sin el día no sale y
                // se puede añadir igual. El fallo de red, si lo hay, lo dirá al añadir.
                dia = null;
                if (!isDestroyed()) pintarEncaje();
            }
        });
    }

    // ── Pintar ──────────────────────────────────────────────────────────────

    private void mostrar(@NonNull Alimento a) {
        alimento = a;
        estado.oculto();
        findViewById(R.id.scrollFicha).setVisibility(View.VISIBLE);
        findViewById(R.id.pieFicha).setVisibility(View.VISIBLE);

        if (lineaId > 0) {
            Integer racionId = getIntent().hasExtra(EXTRA_RACION_ID) ? getIntent().getIntExtra(EXTRA_RACION_ID, -1) : null;
            Double raciones = getIntent().hasExtra(EXTRA_RACIONES) ? getIntent().getDoubleExtra(EXTRA_RACIONES, 0) : null;
            cantidad = CantidadFicha.deLinea(a.getRaciones(), racionId, raciones, getIntent().getDoubleExtra(EXTRA_GRAMOS, 100));
        } else {
            cantidad = CantidadFicha.nueva(a.getRaciones());
        }
        if (estadoGuardado != null && estadoGuardado.containsKey("unidad")) {
            cantidad.elegir(estadoGuardado.getInt("unidad"));
            cantidad.escribir(estadoGuardado.getDouble("valor"));
            if (estadoGuardado.getBoolean("info")) alternarInfo();
        }

        ((TextView) findViewById(R.id.tvNombreFicha)).setText(a.getNombre());
        ((TextView) findViewById(R.id.tvOrigenFicha)).setText(origen(a));
        ((ImageView) findViewById(R.id.ivIconoFicha)).setImageResource(a.esProducto()
                ? R.drawable.ic_ms_inventory_2 : R.drawable.ic_ms_restaurant);
        findViewById(R.id.ivRevisadoFicha).setVisibility(a.isRevisado() ? View.VISIBLE : View.GONE);
        // Lo tuyo no se reporta: se edita, y el administrador no ve los alimentos de nadie.
        findViewById(R.id.btnReportar).setVisibility(a.esPropio() ? View.GONE : View.VISIBLE);
        pintarUnidades();
        pintarReparto();
        pintarInfo100();
        pintarCantidad();

        if (estadoGuardado == null) {
            Movimiento.entrar(0, findViewById(R.id.cabezaFicha), findViewById(R.id.tvPorFicha),
                    findViewById(R.id.cifrasFicha), findViewById(R.id.repartoFicha), findViewById(R.id.cardEncaje));
        }
    }

    private String origen(Alimento a) {
        if (a.esPropio()) return getString(R.string.ficha_origen_tuyo);
        if ("CIQUAL".equals(a.getFuente())) return getString(R.string.ficha_origen_ciqual);
        if ("USDA".equals(a.getFuente())) return getString(R.string.ficha_origen_usda);
        if (a.esProducto()) {
            String marca = a.getMarca() != null && !a.getMarca().trim().isEmpty() ? a.getMarca().trim() : null;
            return marca != null ? getString(R.string.ficha_origen_producto, marca) : getString(R.string.fila_producto_envasado);
        }
        return getString(R.string.ficha_origen_catalogo);
    }

    private void pintarUnidades() {
        if (cantidad == null) return;
        List<CantidadFicha.Unidad> u = cantidad.unidades();
        MaterialButtonToggleGroup grupo = findViewById(R.id.grupoUnidad);
        MaterialButton varias = findViewById(R.id.btnUnidadVarias);
        if (u.size() == 1) {
            // Solo gramos: no hay nada que elegir.
            grupo.setVisibility(View.GONE);
            varias.setVisibility(View.GONE);
            return;
        }
        if (u.size() == 2) {
            varias.setVisibility(View.GONE);
            grupo.setVisibility(View.VISIBLE);
            MaterialButton b0 = findViewById(R.id.btnUnidad0);
            MaterialButton b1 = findViewById(R.id.btnUnidad1);
            b0.setText(nombre(u.get(0)));
            b1.setText(nombre(u.get(1)));
            grupo.clearOnButtonCheckedListeners();
            grupo.check(cantidad.elegida() == 0 ? R.id.btnUnidad0 : R.id.btnUnidad1);
            grupo.addOnButtonCheckedListener((g, id, marcado) -> {
                if (!marcado || cantidad == null) return;
                cantidad.elegir(id == R.id.btnUnidad0 ? 0 : 1);
                pintarCantidad();
            });
            return;
        }
        grupo.setVisibility(View.GONE);
        varias.setVisibility(View.VISIBLE);
        varias.setText(nombre(cantidad.unidad()));
        varias.setContentDescription(getString(R.string.ficha_unidad_elegida_a11y, nombre(cantidad.unidad())));
        varias.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, v);
            for (int i = 0; i < u.size(); i++) {
                menu.getMenu().add(0, i, i, nombre(u.get(i))).setCheckable(true).setChecked(i == cantidad.elegida());
            }
            menu.getMenu().setGroupCheckable(0, true, true);
            menu.setOnMenuItemClickListener(item -> {
                cantidad.elegir(item.getItemId());
                varias.setText(nombre(cantidad.unidad()));
                varias.setContentDescription(getString(R.string.ficha_unidad_elegida_a11y, nombre(cantidad.unidad())));
                pintarCantidad();
                return true;
            });
            menu.show();
        });
    }

    private String nombre(CantidadFicha.Unidad u) {
        return u.esGramos() || u.nombre == null ? getString(R.string.ficha_unidad_gramos) : CantidadFicha.nombreUnidad(u.nombre);
    }

    /** «1 envase (200 g)», «2 × envase (400 g)» o «150 g». */
    private String porcion() {
        if (cantidad == null) return "";
        CantidadFicha.Unidad u = cantidad.unidad();
        String gramos = nf.format(Math.round(cantidad.gramos()));
        if (u.esGramos() || u.nombre == null) return getString(R.string.cantidad_gramos, gramos);
        if (cantidad.valor() == 1) return getString(R.string.cantidad_una_racion, u.nombre, gramos);
        return getString(R.string.cantidad_raciones, nf.format(cantidad.valor()), CantidadFicha.nombreUnidad(u.nombre), gramos);
    }

    private void pintarCantidad() {
        if (cantidad == null || alimento == null) return;
        TextView tv = findViewById(R.id.tvCantidad);
        tv.setText(nf.format(cantidad.valor()));
        tv.setContentDescription(getString(R.string.ficha_cantidad_a11y, porcion()));
        findViewById(R.id.btnMenos).setEnabled(cantidad.puedeMenos());
        ((TextView) findViewById(R.id.tvPorFicha)).setText(getString(R.string.ficha_por, porcion()));

        double g = cantidad.gramos();
        long kcal = Math.round(alimento.getCalorias() * g / 100.0);
        double p = alimento.getProteinas() * g / 100.0;
        double c = alimento.getCarbohidratos() * g / 100.0;
        double gr = alimento.getGrasas() * g / 100.0;
        cifra(R.id.cifraKcal, nf.format(kcal), R.string.ficha_kcal, 0);
        cifra(R.id.cifraProteina, getString(R.string.ficha_gramos_valor, nf.format(p)), etiquetaProteina(),
                ContextCompat.getColor(this, R.color.gp_macro_proteinas));
        cifra(R.id.cifraCarb, getString(R.string.ficha_gramos_valor, nf.format(c)), R.string.ficha_carb, 0);
        cifra(R.id.cifraGrasa, getString(R.string.ficha_gramos_valor, nf.format(gr)), R.string.ficha_grasa, 0);
        findViewById(R.id.cifrasFicha).setContentDescription(getString(R.string.ficha_cifras_a11y,
                nf.format(kcal), nf.format(p), nf.format(c), nf.format(gr)));
        pintarEncaje();
    }

    private void cifra(int id, String valor, int etiqueta, int color) {
        View v = findViewById(id);
        TextView tv = v.findViewById(R.id.tvValorCifra);
        tv.setText(valor);
        if (color != 0) tv.setTextColor(color);
        ((TextView) v.findViewById(R.id.tvEtiquetaCifra)).setText(etiqueta);
    }

    private void pintarReparto() {
        if (alimento == null) return;
        int[] r = EncajeDia.reparto(alimento.getProteinas(), alimento.getCarbohidratos(), alimento.getGrasas());
        View bloque = findViewById(R.id.repartoFicha);
        if (r == null) {
            bloque.setVisibility(View.GONE);
            return;
        }
        int[] tramos = {R.id.tramoProteina, R.id.tramoCarb, R.id.tramoGrasa};
        int[] colores = {R.color.gp_macro_proteinas, R.color.gp_macro_carbos, R.color.gp_macro_grasas};
        int[] textos = {R.string.ficha_reparto_proteina, R.string.ficha_reparto_carb, R.string.ficha_reparto_grasa};
        boolean animar = estadoGuardado == null && !Movimiento.quieto(this);
        for (int i = 0; i < 3; i++) {
            View tramo = findViewById(tramos[i]);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tramo.getLayoutParams();
            lp.weight = r[i];
            tramo.setLayoutParams(lp);
            tramo.setVisibility(r[i] == 0 ? View.GONE : View.VISIBLE);
            if (animar) {
                // Las barras se llenan de izquierda a derecha, una tras otra (BARRA).
                tramo.setPivotX(0);
                tramo.setScaleX(0f);
                tramo.animate().scaleX(1f).setStartDelay(300 + 100L * i).setDuration(Movimiento.BARRA)
                        .setInterpolator(Movimiento.ESTANDAR).start();
            }
        }
        // La leyenda, un solo texto: con letra grande se reparte en líneas en vez de
        // partir cada palabra en columna. Cada punto lleva el color de su macro.
        android.text.SpannableStringBuilder leyenda = new android.text.SpannableStringBuilder();
        for (int i = 0; i < 3; i++) {
            if (i > 0) leyenda.append("   ");
            int desde = leyenda.length();
            leyenda.append("●");
            leyenda.setSpan(new android.text.style.ForegroundColorSpan(ContextCompat.getColor(this, colores[i])),
                    desde, leyenda.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            leyenda.append(" ").append(getString(textos[i], r[i]).replace(" ", " "));
        }
        ((TextView) findViewById(R.id.tvLeyendaReparto)).setText(leyenda);
        bloque.setContentDescription(getString(R.string.ficha_reparto_a11y, r[0], r[1], r[2]));
    }

    private void pintarEncaje() {
        View card = findViewById(R.id.cardEncaje);
        if (dia == null || dia.sinObjetivo || cantidad == null || alimento == null) {
            card.setVisibility(View.GONE);
            return;
        }
        double g = cantidad.gramos();
        double kcal = alimento.getCalorias() * g / 100.0;
        double prot = alimento.getProteinas() * g / 100.0;
        double kcalLinea = getIntent().getDoubleExtra(EXTRA_KCAL_LINEA, 0);
        double protLinea = getIntent().getDoubleExtra(EXTRA_PROT_LINEA, 0);
        EncajeDia e = EncajeDia.de(dia.kcal, dia.proteinas, dia.objetivoKcal, dia.objetivoProteinas,
                kcal, prot, kcalLinea, protLinea);
        if (e == null) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        String antesK = nf.format(e.antesKcal), despuesK = nf.format(e.despuesKcal), objK = nf.format(e.objetivoKcal);
        String antesP = nf.format(Math.round(e.antesProteina)), despuesP = nf.format(Math.round(e.despuesProteina));
        String objP = nf.format(e.objetivoProteina);
        ((TextView) findViewById(R.id.tvEncajeKcal)).setText(getString(R.string.ficha_encaja_kcal_valor, antesK, despuesK, objK));
        ((TextView) findViewById(R.id.tvEncajeProt)).setText(getString(R.string.ficha_encaja_prot_valor, antesP, despuesP, objP));
        findViewById(R.id.encajeKcal).setContentDescription(getString(R.string.ficha_encaja_kcal_a11y, antesK, despuesK, objK));
        findViewById(R.id.encajeProt).setContentDescription(getString(R.string.ficha_encaja_prot_a11y, antesP, despuesP, objP));
        tramos(R.id.tramoKcalAntes, R.id.tramoKcalSuma, e.antesKcalPct, e.sumaKcalPct);
        tramos(R.id.tramoProtAntes, R.id.tramoProtSuma, e.antesProteinaPct, e.sumaProteinaPct);

        String quedanP = nf.format(Math.round(e.quedanProteina));
        String texto;
        if (!e.sePasaKcal) {
            texto = e.sePasaProteina || e.quedanProteina < 0.5
                    ? getString(R.string.ficha_quedan_prot_cumplida, nf.format(e.quedanKcal))
                    : getString(R.string.ficha_quedan, nf.format(e.quedanKcal), quedanP);
        } else {
            texto = e.sePasaProteina || e.quedanProteina < 0.5
                    ? getString(R.string.ficha_pasas_prot_cumplida, nf.format(e.pasaKcalPor))
                    : getString(R.string.ficha_pasas_quedan_prot, nf.format(e.pasaKcalPor), quedanP);
        }
        ((TextView) findViewById(R.id.tvQuedan)).setText(texto);
    }

    // Dos tramos de una barra: lo que ya llevas y lo que suma esto, en % del ancho.
    private void tramos(int antes, int suma, double pctAntes, double pctSuma) {
        View barra = (View) findViewById(antes).getParent();
        barra.post(() -> {
            int ancho = barra.getWidth();
            ViewGroup.LayoutParams a = findViewById(antes).getLayoutParams();
            a.width = (int) Math.round(ancho * pctAntes / 100.0);
            findViewById(antes).setLayoutParams(a);
            ViewGroup.LayoutParams s = findViewById(suma).getLayoutParams();
            s.width = (int) Math.round(ancho * pctSuma / 100.0);
            findViewById(suma).setLayoutParams(s);
        });
    }

    private void pintarInfo100() {
        if (alimento == null) return;
        LinearLayout info = findViewById(R.id.info100);
        info.removeAllViews();
        fila(info, R.string.ficha_info_energia, getString(R.string.ficha_info_kcal_valor, nf.format(alimento.getCalorias())));
        fila(info, R.string.ficha_info_proteinas, getString(R.string.ficha_gramos_valor, nf.format(alimento.getProteinas())));
        fila(info, R.string.ficha_info_carbohidratos, getString(R.string.ficha_gramos_valor, nf.format(alimento.getCarbohidratos())));
        fila(info, R.string.ficha_info_grasas, getString(R.string.ficha_gramos_valor, nf.format(alimento.getGrasas())));
        if (alimento.getFibra() != null) {
            fila(info, R.string.ficha_info_fibra, getString(R.string.ficha_gramos_valor, nf.format(alimento.getFibra())));
        }
    }

    private void fila(LinearLayout dentro, int etiqueta, String valor) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_fila_info, dentro, false);
        ((TextView) v.findViewById(R.id.tvEtiquetaInfo)).setText(etiqueta);
        ((TextView) v.findViewById(R.id.tvValorInfo)).setText(valor);
        dentro.addView(v);
    }

    private void alternarInfo() {
        infoAbierta = !infoAbierta;
        findViewById(R.id.info100).setVisibility(infoAbierta ? View.VISIBLE : View.GONE);
        findViewById(R.id.ivInfo100).setRotation(infoAbierta ? 180f : 0f);
        androidx.core.view.ViewCompat.setStateDescription(findViewById(R.id.btnInfo100),
                getString(infoAbierta ? R.string.ficha_info_abierta : R.string.ficha_info_cerrada));
    }

    // ── Escribir la cantidad ────────────────────────────────────────────────

    private void escribirCantidad() {
        if (cantidad == null) return;
        View vista = getLayoutInflater().inflate(R.layout.dialog_cantidad, null);
        EditText et = vista.findViewById(R.id.etCantidad);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setText(nf.format(cantidad.valor()));
        et.selectAll();
        TextView unidad = vista.findViewById(R.id.tvUnidadCantidad);
        unidad.setText(getString(R.string.ficha_escribir_cantidad_raciones, nombre(cantidad.unidad())));
        androidx.appcompat.app.AlertDialog d = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ficha_escribir_cantidad)
                .setView(vista)
                .setPositiveButton(R.string.dialog_confirmar, null)
                .setNegativeButton(R.string.dialog_cancelar, null)
                .create();
        // Salir con otra cifra escrita pregunta antes de tirarla (GP-108).
        String inicial = et.getText().toString();
        es.pmdm.gymprofit.utils.AvisoDescartar.instalarEnDialogo(this, d,
                () -> es.pmdm.gymprofit.utils.AvisoDescartar.distinto(inicial, et.getText()), et);
        d.setOnShowListener(x -> d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            double valor;
            try {
                valor = Numeros.leerDecimal(et.getText().toString());
            } catch (NumberFormatException e) {
                valor = Double.NaN;
            }
            if (!cantidad.escribir(valor)) {
                double max = cantidad.unidad().esGramos() ? CantidadFicha.MAX_GRAMOS : CantidadFicha.MAX_RACIONES;
                UIHelper.marcarError(et, getString(R.string.ficha_cantidad_invalida, nf.format(max)));
                return;
            }
            pintarCantidad();
            d.dismiss();
        }));
        d.show();
    }

    // ── Reportar ────────────────────────────────────────────────────────────

    private static final String[] MOTIVOS = {"VALORES", "NOMBRE", "RACION", "REPETIDO", "OTRO"};

    private void reportar() {
        if (alimento == null) return;
        String[] textos = {getString(R.string.aviso_motivo_valores), getString(R.string.aviso_motivo_nombre),
                getString(R.string.aviso_motivo_racion), getString(R.string.aviso_motivo_repetido),
                getString(R.string.aviso_motivo_otro)};
        final int[] elegido = {-1};
        androidx.appcompat.app.AlertDialog d = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ficha_reportar_titulo)
                .setSingleChoiceItems(textos, -1, (dlg, i) -> elegido[0] = i)
                .setPositiveButton(R.string.ficha_reportar_enviar, (dlg, i) -> enviarAviso(MOTIVOS[elegido[0]]))
                .setNegativeButton(R.string.dialog_cancelar, null)
                .create();
        d.setOnShowListener(x -> {
            // Sin motivo elegido no se envía nada.
            d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            d.getListView().setOnItemClickListener((lista, v, i, id) -> {
                elegido[0] = i;
                d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            });
        });
        d.show();
    }

    private void enviarAviso(String motivo) {
        if (alimento == null) return;
        Map<String, Object> body = new HashMap<>();
        if (alimento.getId() > 0) body.put("alimentoId", alimento.getId());
        else body.put("barcode", alimento.getBarcode());
        body.put("motivo", motivo);
        alimentoApi.reportar(body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                if (!isDestroyed()) UIHelper.mostrarToastExito(FichaAlimentoActivity.this, getString(R.string.ficha_reportar_gracias));
            }

            @Override
            public void onFail(int code, String message) {
                if (!isDestroyed()) UiFeedback.toastError(FichaAlimentoActivity.this, code, message);
            }
        });
    }

    // ── Añadir o actualizar ─────────────────────────────────────────────────

    private void guardar() {
        if (alimento == null || cantidad == null) return;
        View boton = findViewById(R.id.btnAnadirFicha);
        boton.setEnabled(false);
        LoadingDialog.show(this);
        if (lineaId > 0) {
            lineaApi.patch(lineaId, PedidoCantidad.actualizar(cantidad)).enqueue(new ApiCallback<Void>() {
                @Override
                public void onOk(Void ignored) {
                    LoadingDialog.hide(FichaAlimentoActivity.this);
                    if (isDestroyed()) return;
                    UIHelper.mostrarToastExito(FichaAlimentoActivity.this, getString(R.string.ficha_actualizado));
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onFail(int code, String message) {
                    LoadingDialog.hide(FichaAlimentoActivity.this);
                    boton.setEnabled(true);
                    UiFeedback.toastError(FichaAlimentoActivity.this, code, message);
                }
            });
            return;
        }
        String dia = fecha != null ? fecha
                : new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(new java.util.Date());
        comidaApi.anadir(PedidoCantidad.anadir(alimento, cantidad, dia, tipoComida)).enqueue(new ApiCallback<AnadirRespuesta>() {
            @Override
            public void onOk(AnadirRespuesta r) {
                LoadingDialog.hide(FichaAlimentoActivity.this);
                if (isDestroyed()) return;
                Movimiento.vibrar(boton, Movimiento.Vibracion.LIGERA);
                UIHelper.mostrarToastExito(FichaAlimentoActivity.this, getString(R.string.ficha_anadido, alimento.getNombre()));
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(FichaAlimentoActivity.this);
                boton.setEnabled(true);
                UiFeedback.toastError(FichaAlimentoActivity.this, code, message);
            }
        });
    }

    // Con letra grande «proteína» no cabe en la cuarta parte del ancho sin bajar de 13 sp:
    // ahí va «prot.», como «carb.». TalkBack lee la frase entera de la fila.
    private int etiquetaProteina() {
        return getResources().getConfiguration().fontScale >= 1.3f
                ? R.string.ficha_proteina_corta : R.string.ficha_proteina;
    }
}
