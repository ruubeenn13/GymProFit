package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.programa.DiaPrograma;
import es.pmdm.gymprofit.model.programa.ProgramaDetalle;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.programa.RutinaConEjercicios;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ProgramaApi;
import es.pmdm.gymprofit.ui.widget.FilaPauta;
import es.pmdm.gymprofit.ui.widget.SeguirProgramaHoja;
import es.pmdm.gymprofit.ui.widget.TuProgramaVista;
import es.pmdm.gymprofit.utils.NavTabs;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// ProgramaDetalleActivity — el detalle de un programa (GP-074, lote 1.2.1).
//
// Nombre, material y nivel, días y minutos, la descripción entera (la API ya trae los
// días sugeridos), el ciclo en orden y cada rutina desplegable con sus ejercicios y su
// pauta. «Seguir este programa» abre la hoja del tiempo; si ya es el que sigue, es
// «Cambiar el tiempo». Al seguirlo, vuelve a Entrenar.
// ============================================================
public class ProgramaDetalleActivity extends BaseActivity {

    public static final String EXTRA_CODIGO = "programa_codigo";

    private final ProgramaApi api = ApiClient.service(ProgramaApi.class);

    private String codigo;
    private VistaEstado estado;
    @Nullable private ProgramaDetalle detalle;
    @Nullable private ProgramaQueSigue seguido;
    private boolean seguidoListo, fallo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_programa_detalle);
        codigo = getIntent().getStringExtra(EXTRA_CODIGO);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
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
                if (isFinishing()) return;
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
        if (fallo || detalle == null || !seguidoListo || isFinishing()) return;
        estado.oculto();
        findViewById(R.id.contenidoDetalle).setVisibility(View.VISIBLE);
        findViewById(R.id.barraSeguir).setVisibility(View.VISIBLE);
        ProgramaDetalle p = detalle;

        ((MaterialToolbar) findViewById(R.id.toolbar)).setTitle(p.getNombre());
        ((TextView) findViewById(R.id.tvDetalleNombre)).setText(p.getNombre());
        ((TextView) findViewById(R.id.tvDetalleMaterial)).setText(ProgramasActivity.materialYNivel(this, p));
        ((TextView) findViewById(R.id.tvDetalleDias)).setText(ProgramasActivity.diasYMinutos(this, p));
        ((TextView) findViewById(R.id.tvDetalleDescripcion)).setText(p.getDescripcion());

        LinearLayout ciclo = findViewById(R.id.listaCiclo);
        ciclo.removeAllViews();
        if (p.getSemana() != null) {
            for (DiaPrograma d : p.getSemana()) {
                TextView tv = (TextView) LayoutInflater.from(this).inflate(R.layout.item_dia_ciclo, ciclo, false);
                tv.setText(getString(R.string.programa_ciclo_dia, d.getPosicion(), d.getRutinaNombre()));
                ciclo.addView(tv);
            }
        }

        LinearLayout rutinas = findViewById(R.id.listaRutinasPrograma);
        rutinas.removeAllViews();
        if (p.getRutinas() != null) {
            for (RutinaConEjercicios r : p.getRutinas()) rutinas.addView(rutina(rutinas, r));
        }

        boolean esElSuyo = seguido != null && seguido.getPrograma() != null
                && codigo.equals(seguido.getPrograma().getCodigo());
        TextView boton = findViewById(R.id.btnSeguirPrograma);
        boton.setText(esElSuyo ? R.string.tu_programa_cambiar_tiempo : R.string.programa_seguir);
        boton.setOnClickListener(v -> SeguirProgramaHoja.mostrar(this, codigo, p.getNombre(), seguido,
                this::volverAEntrenar));
    }

    // Una rutina desplegable: la cabecera abre y cierra sus ejercicios.
    private View rutina(LinearLayout padre, RutinaConEjercicios r) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_programa_rutina, padre, false);
        String resumen = TuProgramaVista.resumen(this, r);
        ((TextView) card.findViewById(R.id.tvRutinaNombre)).setText(r.getNombre());
        ((TextView) card.findViewById(R.id.tvRutinaResumen)).setText(resumen);
        TextView desc = card.findViewById(R.id.tvRutinaDescripcion);
        if (r.getDescripcion() != null && !r.getDescripcion().isEmpty()) {
            desc.setText(r.getDescripcion());
            desc.setVisibility(View.VISIBLE);
        }

        LinearLayout ejercicios = card.findViewById(R.id.listaEjerciciosRutina);
        List<RutinaEjercicio> lista = r.getEjercicios() != null ? r.getEjercicios() : new ArrayList<>();
        for (RutinaEjercicio re : lista) ejercicios.addView(FilaPauta.crear(this, ejercicios, re, true));

        View cabecera = card.findViewById(R.id.cabeceraRutina);
        ImageView flecha = card.findViewById(R.id.ivDesplegar);
        String a11y = r.getNombre() + ". " + resumen;
        cabecera.setContentDescription(a11y);
        pintarDesplegado(cabecera, flecha, false);
        cabecera.setOnClickListener(v -> {
            boolean abrir = ejercicios.getVisibility() != View.VISIBLE;
            ejercicios.setVisibility(abrir ? View.VISIBLE : View.GONE);
            pintarDesplegado(cabecera, flecha, abrir);
        });
        return card;
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
        ViewCompat.setStateDescription(cabecera, accion.equals(getString(R.string.programa_rutina_ocultar))
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
