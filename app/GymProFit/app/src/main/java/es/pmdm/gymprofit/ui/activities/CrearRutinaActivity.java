package es.pmdm.gymprofit.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.rutina.BorradorRutina;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ProgramaApi;
import es.pmdm.gymprofit.ui.rutina.TextosCrearRutina;
import es.pmdm.gymprofit.ui.widget.SelectorPildora;
import es.pmdm.gymprofit.utils.AvisoDescartar;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.TiempoRutinas;
import es.pmdm.gymprofit.utils.Transiciones;

// ============================================================
// CrearRutinaActivity — crear una rutina, paso 1: qué y cuánto (GP-244, tablero Mezcla-1).
//
// Las seis zonas (los GrupoMuscular de la API, sin cardio ni cuerpo completo: esos se
// encuentran buscando entre todos), una o varias, y el tiempo: 30, 45, 60 o 75+, de entrada el
// del programa que sigue y, si no sigue ninguno, 45. «Elegir ejercicios» pide al menos una
// zona; «Prefiero buscar entre todos» va al paso 2 sin zonas. Lo elegido viaja en un
// BorradorRutina. Si el paso 3 guarda o se cierra, esta también se cierra.
// ============================================================
public class CrearRutinaActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    /** Los tiempos del selector. */
    static final int[] MINUTOS = {30, 45, 60, 75};
    private static final int MINUTOS_SIN_PROGRAMA = 45;

    private final List<String> zonas = new ArrayList<>();
    private final List<MaterialCardView> tarjetas = new ArrayList<>();
    private SelectorPildora selector;
    private TextView tvPie;
    // Si la persona ha tocado el tiempo: entonces no se cambia por el del programa al llegar.
    private boolean tiempoTocado;

    private ActivityResultLauncher<Intent> paso2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_rutina);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Salir con algo elegido pregunta antes de tirarlo (GP-098).
        AvisoDescartar.instalar(this, toolbar, () -> !zonas.isEmpty() || tiempoTocado);

        tvPie = findViewById(R.id.tvPieCrear);
        montarZonas();
        montarTiempo();

        paso2 = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            // Guardada (OK) o cerrada desde el paso 3 (RESULT_FIRST_USER): se acaba el asistente.
            if (r.getResultCode() == RESULT_OK) {
                setResult(RESULT_OK);
                finish();
            } else if (r.getResultCode() == RESULT_FIRST_USER) {
                finish();
            }
        });
        findViewById(R.id.btnElegirEjercicios).setOnClickListener(v -> {
            if (zonas.isEmpty()) {
                Movimiento.temblar(findViewById(R.id.rejillaZonas));
                Movimiento.vibrar(v, Movimiento.Vibracion.ERROR);
                tvPie.announceForAccessibility(getString(R.string.crear_pie_sin_zona));
                return;
            }
            abrirPaso2(false);
        });
        findViewById(R.id.btnBuscarTodos).setOnClickListener(v -> abrirPaso2(true));
        pintarPie();
    }

    // Las seis tarjetas, de dos en dos.
    private void montarZonas() {
        LinearLayout rejilla = findViewById(R.id.rejillaZonas);
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout fila = null;
        for (int i = 0; i < BorradorRutina.ZONAS.size(); i++) {
            String grupo = BorradorRutina.ZONAS.get(i);
            if (i % 2 == 0) {
                fila = new LinearLayout(this);
                fila.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                if (i > 0) lp.topMargin = getResources().getDimensionPixelSize(R.dimen.espacio_12);
                rejilla.addView(fila, lp);
            }
            MaterialCardView t = (MaterialCardView) inflater.inflate(R.layout.item_zona_crear, fila, false);
            if (i % 2 == 1) {
                ((LinearLayout.LayoutParams) t.getLayoutParams()).setMarginStart(
                        getResources().getDimensionPixelSize(R.dimen.espacio_12));
            }
            ((TextView) t.findViewById(R.id.tvZonaNombre)).setText(TextosCrearRutina.zona(grupo));
            ((TextView) t.findViewById(R.id.tvZonaEjemplos)).setText(TextosCrearRutina.ejemplos(grupo));
            t.setContentDescription(getString(R.string.crear_zona_a11y, getString(TextosCrearRutina.zona(grupo)),
                    getString(TextosCrearRutina.ejemplos(grupo))));
            t.setOnClickListener(v -> {
                if (zonas.contains(grupo)) zonas.remove(grupo);
                else zonas.add(grupo);
                Movimiento.vibrar(v, Movimiento.Vibracion.LIGERA);
                pintarZona(t, zonas.contains(grupo));
                pintarPie();
            });
            pintarZona(t, false);
            fila.addView(t);
            tarjetas.add(t);
        }
    }

    // Elegida, en naranja con el texto sobre la marca y el check; sin elegir, la tarjeta de siempre.
    private void pintarZona(MaterialCardView t, boolean elegida) {
        t.setChecked(elegida);
        int fondo = elegida ? R.color.gp_primary : R.color.gp_surface;
        int texto = elegida ? R.color.gp_on_primary : R.color.gp_on_surface;
        int apoyo = elegida ? R.color.gp_on_primary : R.color.gp_text_secondary;
        t.setCardBackgroundColor(ContextCompat.getColor(this, fondo));
        ((TextView) t.findViewById(R.id.tvZonaNombre)).setTextColor(ContextCompat.getColor(this, texto));
        ((TextView) t.findViewById(R.id.tvZonaEjemplos)).setTextColor(ContextCompat.getColor(this, apoyo));
        ImageView check = t.findViewById(R.id.ivZonaCheck);
        check.setImageTintList(ContextCompat.getColorStateList(this, R.color.gp_on_primary));
        boolean antes = check.getVisibility() == View.VISIBLE;
        check.setVisibility(elegida ? View.VISIBLE : View.INVISIBLE);
        // Momento 1: el check salta al elegir; quieto con «Quitar animaciones».
        if (elegida && !antes && !Movimiento.quieto(this)) Movimiento.saltar(check, Movimiento.CHECK);
    }

    // El selector de tiempo: 45 de entrada y, si sigue un programa, el suyo en cuanto llega.
    private void montarTiempo() {
        selector = findViewById(R.id.selectorMinutos);
        TiempoRutinas.Textos t = TiempoRutinas.Textos.de(this);
        CharSequence[] textos = new CharSequence[MINUTOS.length];
        CharSequence[] nombres = new CharSequence[MINUTOS.length];
        for (int i = 0; i < MINUTOS.length; i++) {
            textos[i] = MINUTOS[i] == 75 ? getString(R.string.seguir_chip_75)
                    : getString(R.string.seguir_chip_min, MINUTOS[i]);
            nombres[i] = t.minutos(MINUTOS[i]);
        }
        selector.setOpciones(textos, nombres);
        elegirMinutos(MINUTOS_SIN_PROGRAMA);
        selector.setAlElegir(i -> {
            tiempoTocado = true;
            pintarPie();
        });
        ApiClient.service(ProgramaApi.class).seguido().enqueue(new ApiCallback<ProgramaQueSigue>() {
            @Override public void onOk(ProgramaQueSigue s) {
                if (isFinishing() || tiempoTocado || s == null) return;
                elegirMinutos(s.getMinutos());
                pintarPie();
            }
            @Override public void onFail(int code, String message) {
                // Seguro: sin el programa se queda en 45, que es lo que dice el diseño sin programa,
                // y el selector deja elegir otro. No hay nada que reintentar ni que enseñar.
            }
        });
    }

    private void elegirMinutos(int minutos) {
        for (int i = 0; i < MINUTOS.length; i++) {
            if (MINUTOS[i] == minutos) selector.setElegida(i, false);
        }
    }

    private int minutos() {
        return MINUTOS[Math.max(0, selector.getElegida())];
    }

    // «Pecho y hombros, 45 minutos.» o, sin zonas, «Elige al menos una zona.»
    private void pintarPie() {
        if (zonas.isEmpty()) {
            tvPie.setText(R.string.crear_pie_sin_zona);
            return;
        }
        BorradorRutina b = new BorradorRutina();
        b.zonas = new ArrayList<>(zonas);
        String nombre = b.nombrePropuesto(BorradorRutina.Textos.de(this));
        tvPie.setText(getString(R.string.crear_pie_paso1, nombre, TiempoRutinas.Textos.de(this).minutos(minutos())));
    }

    private void abrirPaso2(boolean todos) {
        BorradorRutina b = new BorradorRutina();
        if (!todos) b.zonas = new ArrayList<>(zonas);
        b.minutos = minutos();
        Transiciones.avanzar(this, AnadirEjerciciosActivity.paraCrear(this, b), paso2);
    }
}
