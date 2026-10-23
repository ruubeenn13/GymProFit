package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.programa.DiaPrograma;
import es.pmdm.gymprofit.model.programa.Programa;
import es.pmdm.gymprofit.model.programa.ProgramaDetalle;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.programa.RutinaConEjercicios;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ProgramaApi;
import es.pmdm.gymprofit.ui.widget.SeguirProgramaHoja;
import es.pmdm.gymprofit.ui.widget.TuProgramaVista;
import es.pmdm.gymprofit.utils.NavTabs;
import es.pmdm.gymprofit.utils.Siglas;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// ProgramaDetalleActivity — el detalle de un programa (GP-074, rehecho en GP-216, tablero 31).
//
// Cabecera con el color de su material (gimnasio, mancuernas, peso corporal) y sus datos; «El
// ciclo» con cada rutina en su día —los programas son cíclicos, así que los días son el orden en
// que tocan, no los de la semana—; «De qué va» con «Leer más»; las rutinas plegables, una abierta
// cada vez; y «Seguir este programa» fijo abajo. Si ya es el que sigue, el botón es «Cambiar el
// tiempo». Al seguirlo, vuelve a Entrenar.
// ============================================================
public class ProgramaDetalleActivity extends BaseActivity {

    public static final String EXTRA_CODIGO = "programa_codigo";

    // Líneas de la descripción antes de «Leer más».
    private static final int LINEAS_RESUMEN = 3;

    private final ProgramaApi api = ApiClient.service(ProgramaApi.class);

    private String codigo;
    private VistaEstado estado;
    @Nullable private ProgramaDetalle detalle;
    @Nullable private ProgramaQueSigue seguido;
    private boolean seguidoListo, fallo;

    // La rutina abierta ahora (cabecera + su texto), o null.
    @Nullable private View rutinaAbierta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_programa_detalle);
        codigo = getIntent().getStringExtra(EXTRA_CODIGO);
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        es.pmdm.gymprofit.ui.widget.BarraSuperior.enlazar(findViewById(R.id.scrollPantalla), findViewById(R.id.barraSuperior));
        estado = new VistaEstado(findViewById(R.id.estadoDetalle));
        cargar();
    }

    private void cargar() {
        detalle = null;
        seguido = null;
        seguidoListo = false;
        fallo = false;
        estado.cargando();
        findViewById(R.id.contenidoDetalle).setVisibility(View.GONE);
        findViewById(R.id.barraSeguir).setVisibility(View.GONE);

        api.detalle(codigo).enqueue(new ApiCallback<ProgramaDetalle>() {
            @Override public void onOk(ProgramaDetalle d) {
                detalle = d;
                listo();
            }
            @Override public void onFail(int code, String message) {
                if ((isFinishing() || isDestroyed())) return;
                fallo = true;
                estado.error(VistaEstado.mensaje(ProgramaDetalleActivity.this, R.string.programa_detalle_error,
                        code, message), ProgramaDetalleActivity.this::cargar);
            }
        });
        api.seguido().enqueue(new ApiCallback<ProgramaQueSigue>() {
            @Override public void onOk(ProgramaQueSigue s) {
                seguido = s;
                seguidoListo = true;
                listo();
            }
            @Override public void onFail(int code, String message) {
                // Sin saber cuál sigue, el botón es «Seguir este programa» y la hoja no avisa
                // de que deja otro; la API lo deja igual al seguir. Mejor que no dejar seguir.
                seguidoListo = true;
                listo();
            }
        });
    }

    private void listo() {
        if (fallo || detalle == null || !seguidoListo || (isFinishing() || isDestroyed())) return;
        estado.oculto();
        findViewById(R.id.contenidoDetalle).setVisibility(View.VISIBLE);
        findViewById(R.id.barraSeguir).setVisibility(View.VISIBLE);
        ProgramaDetalle p = detalle;

        pintarCabecera(p);
        pintarCiclo(p);
        pintarDescripcion(p.getDescripcion());
        pintarRutinas(p);

        boolean esElSuyo = seguido != null && seguido.getPrograma() != null
                && codigo.equals(seguido.getPrograma().getCodigo());
        com.google.android.material.button.MaterialButton boton = findViewById(R.id.btnSeguirPrograma);
        boton.setText(esElSuyo ? R.string.tu_programa_cambiar_tiempo : R.string.programa_seguir);
        boton.setOnClickListener(v -> SeguirProgramaHoja.mostrar(this, codigo, p.getNombre(), seguido,
                this::volverAEntrenar));
    }

    // ── Cabecera ────────────────────────────────────────────────────────────

    // El color de su material, el nombre y tres datos.
    private void pintarCabecera(ProgramaDetalle p) {
        ((TextView) findViewById(R.id.tvDetalleMaterial)).setText(getString(R.string.programa_eyebrow,
                TuProgramaVista.equipamiento(this, p.getEquipamiento()).toLowerCase(
                        es.pmdm.gymprofit.utils.FechaUtils.localeDeLaApp(this))));
        ((TextView) findViewById(R.id.tvDetalleNombre)).setText(p.getNombre());

        int color = ContextCompat.getColor(this, TuProgramaVista.colorDe(p.getEquipamiento()));

        // Sólido y con el borde de abajo redondeado: un degradado hasta el fondo dejaba el título claro sobre un fondo claro en el tema claro.
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        float r = getResources().getDimension(R.dimen.radio_protagonista);
        g.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        findViewById(R.id.cabeceraPrograma).setBackground(g);

        ChipGroup chips = findViewById(R.id.chipsPrograma);
        chips.removeAllViews();
        String dias = p.getDiasMin() == p.getDiasMax()
                ? getResources().getQuantityString(R.plurals.programa_dias, p.getDiasMax(), p.getDiasMax())
                : getString(R.string.programa_dias_rango, p.getDiasMin(), p.getDiasMax());
        chips.addView(dato(dias));
        int[] minutos = p.minutos();
        if (minutos != null) {
            chips.addView(dato(minutos[0] == minutos[1] ? getString(R.string.duracion_min, minutos[0])
                    : getString(R.string.programa_minutos_rango, minutos[0], minutos[1])));
        }
        if (p.getNivel() != null) chips.addView(dato(UIHelper.traducirNivel(this, p.getNivel())));
    }

    // Una píldora de dato sobre la cabecera (solo texto).
    private TextView dato(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(13);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setTextColor(ContextCompat.getColor(this, R.color.gp_on_hero));
        float dp = getResources().getDisplayMetrics().density;
        tv.setPadding(Math.round(10 * dp), Math.round(6 * dp), Math.round(10 * dp), Math.round(6 * dp));
        tv.setBackgroundResource(R.drawable.bg_chip_cabecera);
        return tv;
    }

    // ── El ciclo ────────────────────────────────────────────────────────────

    // Un círculo por día del ciclo con las siglas de su rutina.
    private void pintarCiclo(ProgramaDetalle p) {
        LinearLayout ciclo = findViewById(R.id.listaCiclo);
        ciclo.removeAllViews();
        List<DiaPrograma> semana = p.getSemana() != null ? p.getSemana() : new ArrayList<>();
        LayoutInflater inflater = LayoutInflater.from(this);
        StringBuilder voz = new StringBuilder();
        for (DiaPrograma d : semana) {
            View v = inflater.inflate(R.layout.item_dia_programa, ciclo, false);
            ((TextView) v.findViewById(R.id.tvDiaNumero)).setText(String.valueOf(d.getPosicion()));
            ((TextView) v.findViewById(R.id.tvDiaSiglas)).setText(Siglas.de(d.getRutinaNombre()));
            ciclo.addView(v);
            if (voz.length() > 0) voz.append(", ");
            voz.append(getString(R.string.programa_ciclo_dia_a11y, d.getPosicion(), d.getRutinaNombre()));
        }
        // La tira entera se lee de una vez.
        ciclo.setContentDescription(getString(R.string.programa_ciclo_a11y, semana.size(), voz));
        ciclo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    // ── De qué va ───────────────────────────────────────────────────────────

    // La descripción entera; si pasa de tres líneas, se recorta con «Leer más».
    private void pintarDescripcion(@Nullable String texto) {
        TextView tv = findViewById(R.id.tvDetalleDescripcion);
        TextView boton = findViewById(R.id.btnLeerMas);
        tv.setText(texto != null ? texto : "");
        tv.setMaxLines(Integer.MAX_VALUE);
        boton.setVisibility(View.GONE);
        tv.post(() -> {
            if (tv.getLineCount() <= LINEAS_RESUMEN) return;
            tv.setMaxLines(LINEAS_RESUMEN);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            boton.setText(R.string.programa_leer_mas);
            boton.setVisibility(View.VISIBLE);
            boton.setOnClickListener(v -> {
                boolean abrir = tv.getMaxLines() != Integer.MAX_VALUE;
                tv.setMaxLines(abrir ? Integer.MAX_VALUE : LINEAS_RESUMEN);
                boton.setText(abrir ? R.string.programa_leer_menos : R.string.programa_leer_mas);
            });
        });
    }

    // ── Las rutinas ─────────────────────────────────────────────────────────

    private void pintarRutinas(ProgramaDetalle p) {
        List<RutinaConEjercicios> rutinas = p.getRutinas() != null ? p.getRutinas() : new ArrayList<>();
        ((TextView) findViewById(R.id.tvRutinasTitulo)).setText(getResources().getQuantityString(
                R.plurals.programa_rutinas_n, rutinas.size(), rutinas.size()));
        LinearLayout lista = findViewById(R.id.listaRutinasPrograma);
        lista.removeAllViews();
        rutinaAbierta = null;
        for (int i = 0; i < rutinas.size(); i++) {
            View fila = rutina(lista, rutinas.get(i), i + 1);
            if (i == rutinas.size() - 1) fila.findViewById(R.id.rayaRutina).setVisibility(View.GONE);
            lista.addView(fila);
        }
    }

    // Una rutina plegable: la cabecera abre y cierra la lista de sus ejercicios; solo una abierta.
    private View rutina(LinearLayout padre, RutinaConEjercicios r, int numero) {
        View fila = LayoutInflater.from(this).inflate(R.layout.item_programa_rutina, padre, false);
        String resumen = TuProgramaVista.resumen(this, r);
        ((TextView) fila.findViewById(R.id.tvRutinaNumero)).setText(String.valueOf(numero));
        ((TextView) fila.findViewById(R.id.tvRutinaNombre)).setText(r.getNombre());
        ((TextView) fila.findViewById(R.id.tvRutinaResumen)).setText(resumen);

        List<String> nombres = new ArrayList<>();
        List<RutinaEjercicio> lista = r.getEjercicios() != null ? r.getEjercicios() : new ArrayList<>();
        for (RutinaEjercicio re : lista) {
            if (re.getNombreEjercicio() != null && !re.getNombreEjercicio().isEmpty()) nombres.add(re.getNombreEjercicio());
        }
        TextView ejercicios = fila.findViewById(R.id.tvRutinaEjercicios);
        ejercicios.setText(android.text.TextUtils.join(", ", nombres));

        View cabecera = fila.findViewById(R.id.cabeceraRutina);
        ImageView flecha = fila.findViewById(R.id.ivDesplegar);
        cabecera.setContentDescription(r.getNombre() + ". " + resumen);
        pintarDesplegado(cabecera, flecha, false);
        cabecera.setOnClickListener(v -> {
            boolean abrir = ejercicios.getVisibility() != View.VISIBLE;
            // Solo una abierta: la anterior se pliega.
            if (abrir && rutinaAbierta != null && rutinaAbierta != fila) plegar(rutinaAbierta);
            ejercicios.setVisibility(abrir ? View.VISIBLE : View.GONE);
            pintarDesplegado(cabecera, flecha, abrir);
            rutinaAbierta = abrir ? fila : null;
        });
        return fila;
    }

    private void plegar(View fila) {
        fila.findViewById(R.id.tvRutinaEjercicios).setVisibility(View.GONE);
        pintarDesplegado(fila.findViewById(R.id.cabeceraRutina), fila.findViewById(R.id.ivDesplegar), false);
    }

    // La flecha y lo que dice TalkBack del toque: «Mostrar» u «Ocultar ejercicios».
    private void pintarDesplegado(View cabecera, ImageView flecha, boolean abierto) {
        flecha.setImageResource(abierto ? R.drawable.ic_ms_keyboard_arrow_up : R.drawable.ic_ms_keyboard_arrow_down);
        String accion = getString(abierto ? R.string.programa_rutina_ocultar : R.string.programa_rutina_mostrar);
        ViewCompat.setAccessibilityDelegate(cabecera, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK, accion));
            }
        });
        ViewCompat.setStateDescription(cabecera, abierto
                ? getString(R.string.programa_rutina_abierta) : getString(R.string.programa_rutina_cerrada));
    }

    // Tras seguirlo: a Entrenar, que pide el programa que sigue.
    private void volverAEntrenar() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(NavTabs.EXTRA_TAB, NavTabs.ENTRENAR);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }
}
