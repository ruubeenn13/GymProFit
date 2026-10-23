package es.pmdm.gymprofit.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.AvatarUtils;
import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.model.comida.Comida;
import es.pmdm.gymprofit.model.comida.ComidaReciente;
import es.pmdm.gymprofit.model.comida.CopiaRespuesta;
import es.pmdm.gymprofit.model.comida.DiaDiario;
import es.pmdm.gymprofit.network.AlimentoComidaApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.network.DiarioApi;
import es.pmdm.gymprofit.ui.activities.AnadirAlimentoActivity;
import es.pmdm.gymprofit.ui.activities.ComidaActivity;
import es.pmdm.gymprofit.ui.activities.SemanaNutricionActivity;
import es.pmdm.gymprofit.ui.nutricion.AnilloDia;
import es.pmdm.gymprofit.ui.nutricion.CopiarComida;
import es.pmdm.gymprofit.ui.nutricion.ElegirComida;
import es.pmdm.gymprofit.utils.Agua;
import es.pmdm.gymprofit.utils.ComidaQueToca;
import es.pmdm.gymprofit.utils.ComidasRecientes;
import es.pmdm.gymprofit.model.comida.ResumenDiarioNutricion;
import es.pmdm.gymprofit.utils.DiaCompleto;
import es.pmdm.gymprofit.utils.DiaNutricion;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.ProteinaTexto;
import es.pmdm.gymprofit.utils.ResultadoNutricional;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.SemanaNutricion;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// NutricionFragment — pestaña Nutrición (GP-105), el diario.
//
// La tira de la semana (de lunes a domingo, con los días futuros desactivados)
// sustituye a las flechas ‹ ›; el calendario abre un selector de fecha hasta hoy; el
// botón de gráficas abre «Historial y estadísticas». Debajo, las cinco comidas: tocar
// abre la comida; su «+» abre directamente añadir alimento, y el de la comida que toca
// por la hora va en naranja si está sin registrar.
// Desde la 1.6.4 (tablero 1), la comida que toca ahora, si el día es hoy, está vacía y la
// misma comida de ayer tuvo algo, lleva debajo «¿Copiar la de ayer?»: la ✓ la copia y el
// día se recarga; la ✗ la pliega y no vuelve ese día (se guarda en el móvil).
//
// Desde la 1.6.8, el diario nuevo (tableros 1 y 3, GP-162 con GP-166):
//   · El anillo de las kcal del día, lo que queda y los macros, de DiaNutricion. Sin rojo
//     en lo que queda: pasarse se dice, no se riñe.
//   · El agua, a vasos hacia el objetivo del plan (Agua): «Vaso» suma uno, tocar el último
//     lleno lo quita. Se guarda el total del día en la API, uno detrás de otro.
//   · La tira, con el punto de cada día (DiaCompleto) y la marca del entreno; «Día de
//     entreno · …» si ese día hubo una sesión terminada. Todo sale de GET /diario, una
//     petición por semana.
//   · El día completo: las cinco comidas apuntadas o marcadas «Hoy no» (en la pantalla de
//     la comida). El anillo se cierra en verde, «Día completo» y «Tu semana». Al completarse
//     mientras se mira, el momento 21, una vez al día.
// ============================================================
public class NutricionFragment extends BaseFragment {

    /** Vasos por fila, como el tablero 1. */
    private static final int VASOS_POR_FILA = 9;

    private final ComidaApi comidaApi = ApiClient.service(ComidaApi.class);
    private final AlimentoComidaApi alimentoComidaApi = ApiClient.service(AlimentoComidaApi.class);
    private final DiarioApi diarioApi = ApiClient.service(DiarioApi.class);

    // Día que se está viendo (por defecto hoy; nunca uno que no ha llegado).
    private final Calendar fechaSel = Calendar.getInstance();
    private final Map<String, Comida> comidasDia = new HashMap<>();
    // Qué hay dentro de cada comida, para su resumen («Tostadas, café · 420 kcal»).
    private final Map<Integer, List<AlimentoComida>> alimentosPorComida = new HashMap<>();

    // La semana de la tira, de GET /diario (fecha → día); vacía hasta que llega.
    private final Map<String, DiaDiario> semana = new HashMap<>();
    // «Tu semana» (tablero 15): lo comido cada día de esa semana, de GET /comidas/usuario/{id}/resumen.
    private final Map<String, ResumenDiarioNutricion> resumenSemana = new HashMap<>();
    @Nullable private String semanaResumida;
    private int turnoResumen;
    // La línea del día completo («2 días completos esta semana…»), o null.
    @Nullable private String semanaExtra;
    private int turnoDiario;
    // Si cada día estaba completo la última vez que se pintó: para celebrar solo el paso.
    private final Map<String, Boolean> completoVisto = new HashMap<>();
    // El día cuyas cifras ya han contado al entrar (momento 7): las siguientes, pasan.
    @Nullable private String fechaContada;

    // El agua del día elegido: lo que se ve y lo último que confirmó la API. Se manda de
    // una en una; lo que se toque mientras tanto sale después, con el total de ese momento.
    private int aguaMostrada, aguaConfirmada;
    private boolean enviandoAgua, aguaPendiente;
    // La acción de TalkBack «Quitar un vaso» del resumen del agua, o NO_ID.
    private int accionQuitarVaso = View.NO_ID;
    // El día de las comidas que hay en comidasDia: hasta que llegan las del día elegido,
    // la tarjeta del anillo no se repinta con las de otro.
    @Nullable private String comidasDe;

    private ActivityResultLauncher<Intent> recargar;
    // El fallo de cargar el día, con su «Reintentar» (como Progreso e Historial).
    private VistaEstado estado;

    // «¿Copiar la de ayer?» (1.6.4): la comida de ayer y a qué comida de hoy va; null si no
    // toca. Mientras se copia no se vuelve a pintar, y si falla vuelve.
    @Nullable private ComidaReciente copiaAyer;
    @Nullable private String copiaAyerTipo;
    private boolean copiandoAyer;
    private int turnoCopiaAyer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recargar = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) return;
                    cargarComidas();
                    cargarDiario();
                    // Se añadió a otra comida que la de la tarjeta: se dice a cuál (decisión 15).
                    String a = result.getData() != null
                            ? result.getData().getStringExtra(AnadirAlimentoActivity.EXTRA_ANADIDO_A) : null;
                    if (a != null) {
                        UIHelper.mostrarToastExito(requireActivity(), getString(ElegirComida.anadidoA(a)));
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nutricion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fechaContada = null;
        // «Tu semana de nutrición» (tablero 12); desde ahí se llega a las semanas y los meses.
        estado = new VistaEstado(findViewById(R.id.estadoNutricion));
        findViewById(R.id.cardSemana).setOnClickListener(v -> startActivity(
                new Intent(requireContext(), SemanaNutricionActivity.class)
                        .putExtra(SemanaNutricionActivity.EXTRA_LUNES, fechaStr(lunesDe(fechaSel)))));
        findViewById(R.id.btnHistorial).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SemanaNutricionActivity.class)));
        findViewById(R.id.btnCalendario).setOnClickListener(v -> elegirFecha());
        prepararMacro(R.id.macroProteinas, R.string.nutricion_proteinas, R.color.gp_macro_proteinas, 10);
        prepararMacro(R.id.macroCarbos, R.string.nutricion_carbohidratos, R.color.gp_macro_carbos, 8);
        prepararMacro(R.id.macroGrasas, R.string.nutricion_grasas, R.color.gp_macro_grasas, 8);
        findViewById(R.id.btnVaso).setOnClickListener(v -> anadirVaso(v));
        findViewById(R.id.cardAgua).setVisibility(View.GONE);
        // Con letra grande (1,3 o más) el centro del anillo no cabía en 136 al lado de lo que
        // queda: se apilan, el anillo arriba y a 200, y lo que queda (o «Día completo») debajo.
        if (getResources().getConfiguration().fontScale >= 1.3f) {
            float d = getResources().getDisplayMetrics().density;
            LinearLayout fila = findViewById(R.id.filaKcal);
            fila.setOrientation(LinearLayout.VERTICAL);
            fila.setGravity(Gravity.CENTER_HORIZONTAL);
            View marco = findViewById(R.id.marcoAnilloDia);
            ViewGroup.LayoutParams lp = marco.getLayoutParams();
            lp.width = lp.height = Math.round(200 * d);
            marco.setLayoutParams(lp);
            for (int id : new int[]{R.id.grupoQuedan, R.id.grupoCompleto}) {
                View g = findViewById(id);
                LinearLayout.LayoutParams lg = (LinearLayout.LayoutParams) g.getLayoutParams();
                lg.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lg.weight = 0;
                lg.setMarginStart(0);
                lg.topMargin = Math.round(16 * d);
                g.setLayoutParams(lg);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AvatarUtils.cabecera(requireContext(), findViewById(R.id.avatarCabecera), prefsManager);
        // Si la app se quedó abierta de un día para otro, «hoy» ya es otro.
        Calendar hoy = Calendar.getInstance();
        if (fechaSel.after(hoy)) fechaSel.setTime(hoy.getTime());
        pintarSemana(false);
        cargarComidas();
        cargarDiario();
        pintarAvisoSemana();
    }

    // ── «Tu semana de nutrición», los lunes ─────────────────────────────────

    /**
     * Los lunes, una tarjeta que lleva al resumen de la semana pasada (decisión 13): sin
     * notificación, y solo si no se ha cerrado ya ese lunes. El resto de la semana no sale; el
     * resumen sigue en el botón de la cabecera.
     */
    private void pintarAvisoSemana() {
        View tarjeta = findViewById(R.id.cardAvisoSemana);
        Calendar hoy = Calendar.getInstance();
        java.text.SimpleDateFormat iso = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        String lunes = iso.format(hoy.getTime());
        if (hoy.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY || lunes.equals(prefsManager.getAvisoSemanaCerrado())) {
            tarjeta.setVisibility(View.GONE);
            return;
        }
        tarjeta.setVisibility(View.VISIBLE);
        Calendar pasada = (Calendar) hoy.clone();
        pasada.add(Calendar.DAY_OF_YEAR, -7);
        findViewById(R.id.filaAvisoSemana).setOnClickListener(v -> {
            // Se ha visto: no vuelve a salir hasta el lunes que viene.
            prefsManager.setAvisoSemanaCerrado(lunes);
            startActivity(new Intent(requireContext(), SemanaNutricionActivity.class)
                    .putExtra(SemanaNutricionActivity.EXTRA_LUNES, iso.format(pasada.getTime())));
        });
        findViewById(R.id.btnCerrarAvisoSemana).setOnClickListener(v -> {
            prefsManager.setAvisoSemanaCerrado(lunes);
            tarjeta.setVisibility(View.GONE);
        });
    }

    // ── Tira de la semana ───────────────────────────────────────────────────

    // Con el día completándose mientras se mira, su punto salta (momento 21).
    private void pintarSemana(boolean saltaPuntoElegido) {
        LinearLayout tira = findViewById(R.id.tiraSemana);
        tira.removeAllViews();
        String[] iniciales = getResources().getStringArray(R.array.dias_semana_iniciales);
        Locale idioma = FechaUtils.localeDeLaApp(requireContext());
        SimpleDateFormat largo = new SimpleDateFormat(getString(R.string.home_fecha_patron), idioma);

        Calendar hoy = Calendar.getInstance();
        Calendar dia = lunesDe(fechaSel);

        int texto = color(com.google.android.material.R.attr.colorOnSurface);
        int secundario = color(com.google.android.material.R.attr.colorOnSurfaceVariant);
        int apagado = color(com.google.android.material.R.attr.colorOutline);
        int sobreContenedor = ContextCompat.getColor(requireContext(), R.color.gp_on_primary_container);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < 7; i++) {
            final Calendar esteDia = (Calendar) dia.clone();
            boolean elegido = mismoDia(esteDia, fechaSel);
            boolean esHoy = mismoDia(esteDia, hoy);
            boolean futuro = esteDia.after(hoy) && !esHoy;
            DiaDiario dd = semana.get(fechaStr(esteDia));

            View celda = inflater.inflate(R.layout.item_dia_semana, tira, false);
            TextView tvInicial = celda.findViewById(R.id.tvInicial);
            TextView tvNumero = celda.findViewById(R.id.tvNumero);
            tvInicial.setText(iniciales[i]);
            tvNumero.setText(String.valueOf(esteDia.get(Calendar.DAY_OF_MONTH)));

            if (elegido) {
                celda.setBackgroundResource(R.drawable.bg_dia_elegido);
                tvInicial.setTextColor(sobreContenedor);
                tvInicial.setTypeface(tvInicial.getTypeface(), android.graphics.Typeface.BOLD);
                tvNumero.setTextColor(texto);
                tvNumero.setTypeface(tvNumero.getTypeface(), android.graphics.Typeface.BOLD);
            } else {
                celda.setBackgroundResource(fondoPulsable());
                tvInicial.setTextColor(futuro ? apagado : secundario);
                tvNumero.setTextColor(futuro ? apagado : texto);
            }

            DiaCompleto.Punto punto = DiaCompleto.punto(dd, esHoy, futuro);
            View vPunto = celda.findViewById(R.id.vPuntoDia);
            pintarPunto(vPunto, punto);
            boolean entreno = dd != null && dd.getEntrenos() > 0 && !futuro;
            celda.findViewById(R.id.vMarcaEntreno).setBackgroundResource(entreno ? R.drawable.bg_marca_entreno : 0);
            if (elegido && saltaPuntoElegido && punto == DiaCompleto.Punto.COMPLETO && !Movimiento.quieto(requireContext())) {
                Movimiento.saltar(vPunto, Movimiento.PUNTO);
            }

            String nombre = largo.format(esteDia.getTime());
            celda.setContentDescription(futuro ? getString(R.string.dia_futuro_a11y, nombre)
                    : descripcionDia(nombre, punto, entreno));
            celda.setSelected(elegido);
            celda.setEnabled(!futuro);
            celda.setOnClickListener(futuro ? null : v -> {
                fechaSel.setTime(esteDia.getTime());
                pintarSemana(false);
                cargarComidas();
                if (semana.containsKey(fechaSelStr())) pintarDiario();
                else cargarDiario();
            });
            tira.addView(celda);
            dia.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void pintarPunto(View v, DiaCompleto.Punto punto) {
        switch (punto) {
            case COMPLETO:
                v.setBackgroundResource(R.drawable.bg_punto_dia);
                v.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.gp_success)));
                break;
            case A_MEDIAS:
                v.setBackgroundResource(R.drawable.bg_punto_dia);
                v.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.gp_macro_carbos)));
                break;
            case EN_CURSO:
                v.setBackgroundTintList(null);
                v.setBackgroundResource(R.drawable.bg_punto_dia_en_curso);
                break;
            default:
                v.setBackgroundTintList(null);
                v.setBackground(null);
        }
    }

    // «Lunes 21 de septiembre: día completo, día de entreno».
    private String descripcionDia(String nombre, DiaCompleto.Punto punto, boolean entreno) {
        List<String> partes = new ArrayList<>();
        if (punto == DiaCompleto.Punto.COMPLETO) partes.add(getString(R.string.dia_a11y_completo));
        else if (punto == DiaCompleto.Punto.A_MEDIAS) partes.add(getString(R.string.dia_a11y_a_medias));
        else if (punto == DiaCompleto.Punto.EN_CURSO) partes.add(getString(R.string.dia_a11y_en_curso));
        if (entreno) partes.add(getString(R.string.dia_a11y_entreno));
        if (partes.isEmpty()) return nombre;
        return getString(R.string.dia_a11y, nombre, android.text.TextUtils.join(", ", partes));
    }

    // Selector de fecha hasta hoy; la tira salta a la semana del día elegido.
    private void elegirFecha() {
        long hoyUtc = MaterialDatePicker.todayInUtcMilliseconds();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(utcDe(fechaSel))
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setEnd(hoyUtc)
                        .setValidator(DateValidatorPointBackward.before(hoyUtc + 1))
                        .build())
                .build();
        picker.addOnPositiveButtonClickListener(utc -> {
            Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            u.setTimeInMillis(utc);
            fechaSel.set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH));
            pintarSemana(false);
            cargarComidas();
            cargarDiario();
        });
        picker.show(getParentFragmentManager(), "fecha_nutricion");
    }

    private static long utcDe(Calendar local) {
        Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        u.clear();
        u.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        return u.getTimeInMillis();
    }

    private static Calendar lunesDe(Calendar c) {
        Calendar lunes = (Calendar) c.clone();
        lunes.add(Calendar.DAY_OF_MONTH, -((lunes.get(Calendar.DAY_OF_WEEK) + 5) % 7));
        return lunes;
    }

    private static boolean mismoDia(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private boolean esHoy() {
        return mismoDia(fechaSel, Calendar.getInstance());
    }

    private String fechaSelStr() {
        return fechaStr(fechaSel);
    }

    private static String fechaStr(Calendar c) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    // ── Carga ───────────────────────────────────────────────────────────────

    private void cargarComidas() {
        int usuarioId = prefsManager.getUsuarioId();
        final String fecha = fechaSelStr();
        if (usuarioId == -1) {
            comidasDia.clear();
            alimentosPorComida.clear();
            comidasDe = fecha;
            pintar();
            return;
        }
        comidaApi.getDeUsuarioFecha(usuarioId, fecha).enqueue(new ApiCallback<List<Comida>>() {
            @Override
            public void onOk(List<Comida> lista) {
                if (!isAdded() || !fecha.equals(fechaSelStr())) return;
                comidasDia.clear();
                alimentosPorComida.clear();
                if (lista != null) for (Comida c : lista) comidasDia.put(c.getTipoComida(), c);
                comidasDe = fecha;
                contenidoDia(true);
                pintar();
                cargarAlimentos(fecha);
                buscarCopiaAyer(fecha);
            }
            @Override
            public void onFail(int code, String message) {
                if (!isAdded()) return;
                // Un día sin comidas no llega aquí: es 200 con []. Esto es un fallo de
                // verdad: se dice dónde irían las cifras, con su botón; no se pinta un día vacío que no lo es.
                contenidoDia(false);
                estado.error(VistaEstado.mensaje(requireContext(), R.string.nutricion_error_dia, code, message),
                        () -> {
                            estado.cargando();
                            cargarComidas();
                            cargarDiario();
                        });
            }
        });
    }

    // Con el día cargado se ve el diario; con el fallo, solo el aviso y su botón.
    private void contenidoDia(boolean hay) {
        if (hay) estado.oculto();
        int v = hay ? View.VISIBLE : View.GONE;
        findViewById(R.id.cardDia).setVisibility(v);
        findViewById(R.id.cardComidas).setVisibility(v);
        findViewById(R.id.cabeceraComidas).setVisibility(v);
        if (!hay) {
            findViewById(R.id.pildoraEntreno).setVisibility(View.GONE);
            findViewById(R.id.cardAgua).setVisibility(View.GONE);
            findViewById(R.id.cardSemana).setVisibility(View.GONE);
        }
    }

    // La semana de la tira: puntos, entrenos, agua y «Hoy no» de cada día. Si falla se
    // dice, y el diario se queda sin lo que sale de aquí (puntos, agua, día completo):
    // lo de las comidas sigue siendo cierto.
    private void cargarDiario() {
        if (prefsManager.getUsuarioId() == -1) return;
        int miTurno = ++turnoDiario;
        Calendar lunes = lunesDe(fechaSel);
        Calendar domingo = (Calendar) lunes.clone();
        domingo.add(Calendar.DAY_OF_MONTH, 6);
        diarioApi.dias(fechaStr(lunes), fechaStr(domingo)).enqueue(new ApiCallback<List<DiaDiario>>() {
            @Override
            public void onOk(List<DiaDiario> dias) {
                if (!isAdded() || miTurno != turnoDiario) return;
                semana.clear();
                if (dias != null) for (DiaDiario d : dias) if (d.getFecha() != null) semana.put(d.getFecha(), d);
                pintarDiario();
                cargarResumenSemana(lunes, domingo, miTurno);
            }

            @Override
            public void onFail(int code, String message) {
                if (!isAdded() || miTurno != turnoDiario) return;
                UIHelper.mostrarToastError(requireActivity(), getString(R.string.diario_fallo,
                        UiFeedback.mensaje(requireContext(), code, message)));
            }
        });
    }

    // Lo que depende del día del diario: celebra si se acaba de completar.
    private void pintarDiario() {
        String fecha = fechaSelStr();
        DiaDiario dd = semana.get(fecha);
        boolean completo = DiaCompleto.completo(dd);
        boolean celebrar = false;
        if (dd != null) {
            Boolean antes = completoVisto.put(fecha, completo);
            celebrar = DiaCompleto.celebrar(antes, completo, fecha, prefsManager.getDiaCompletoCelebrado());
            if (celebrar) prefsManager.setDiaCompletoCelebrado(fecha);
            if (!enviandoAgua) {
                aguaMostrada = dd.getAguaMl();
                aguaConfirmada = dd.getAguaMl();
            }
        }
        pintarSemana(celebrar);
        pintarKcal(celebrar);
        pintarEntreno(dd);
        pintarAgua(-1, -1);
        pintarSemanaCompleta(dd, celebrar);
        pintarComidas();
    }

    // Lo que hay dentro de cada comida, para su resumen. Si alguna falla, esa comida
    // se queda con el resumen corto (solo kcal), que sigue siendo cierto.
    private void cargarAlimentos(String fecha) {
        for (Comida c : comidasDia.values()) {
            alimentoComidaApi.getDeComida(c.getId()).enqueue(new ApiCallback<List<AlimentoComida>>() {
                @Override
                public void onOk(List<AlimentoComida> lista) {
                    if (!isAdded() || !fecha.equals(fechaSelStr())) return;
                    alimentosPorComida.put(c.getId(), lista != null ? lista : new ArrayList<>());
                    pintarComidas();
                }
                @Override
                public void onFail(int code, String message) {
                    // Se ignora a propósito (GP-017): el resumen de la tarjeta se queda en
                    // las kcal, que ya están; el detalle está al abrir la comida.
                }
            });
        }
    }

    // ── «¿Copiar la de ayer?» (1.6.4, B2) ───────────────────────────────────

    // Solo hoy, solo la comida que toca ahora, solo si está vacía y no se ha dicho que no.
    // Un fallo no se enseña: sin la tarjeta, el diario es el de siempre.
    private void buscarCopiaAyer(String fecha) {
        int miTurno = ++turnoCopiaAyer;
        String tipo = ComidaQueToca.ahora();
        if (copiandoAyer) return;
        if (!esHoy() || registrada(comidasDia.get(tipo)) || prefsManager.copiaAyerDescartada(fecha, tipo)) {
            if (copiaAyer != null) {
                copiaAyer = null;
                pintarComidas();
            }
            return;
        }
        comidaApi.recientes(fecha, tipo).enqueue(new ApiCallback<List<ComidaReciente>>() {
            @Override
            public void onOk(List<ComidaReciente> lista) {
                if (!isAdded() || miTurno != turnoCopiaAyer || !fecha.equals(fechaSelStr())) return;
                copiaAyer = ComidasRecientes.deAyer(lista, tipo, fecha);
                copiaAyerTipo = tipo;
                pintarComidas();
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: la tarjeta es un atajo; sin ella se apunta igual.
            }
        });
    }

    // «Copiar ayer»: el enlace se va al momento y la comida se copia; al volver, el día se
    // recarga con ella. Si falla, el enlace vuelve y se dice.
    private void copiarAyer(@NonNull View tarjeta, @NonNull ComidaReciente ayer, @NonNull String tipo) {
        if (copiandoAyer) return;
        copiandoAyer = true;
        String fecha = fechaSelStr();
        Movimiento.vibrar(tarjeta, Movimiento.Vibracion.LIGERA);
        findViewById(R.id.btnCopiarAyerDiario).setVisibility(View.GONE);
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("comidaId", ayer.getId());
        cuerpo.put("fecha", fecha);
        cuerpo.put("tipoComida", tipo);
        comidaApi.copiar(cuerpo).enqueue(new ApiCallback<CopiaRespuesta>() {
            @Override
            public void onOk(CopiaRespuesta r) {
                copiandoAyer = false;
                if (!isAdded()) return;
                copiaAyer = null;
                cargarComidas();
                cargarDiario();
            }

            @Override
            public void onFail(int code, String message) {
                copiandoAyer = false;
                if (!isAdded()) return;
                pintarComidas();
                UIHelper.mostrarToastError(requireActivity(), getString(R.string.copiar_ayer_fallo,
                        UiFeedback.mensaje(requireContext(), code, message)));
            }
        });
    }

    private static boolean registrada(@Nullable Comida c) {
        return c != null && c.getTotalCalorias() > 0;
    }

    // Apuntada: con kcal, o con algo dentro según el diario (un café solo no suma kcal).
    private boolean apuntada(String tipo) {
        if (registrada(comidasDia.get(tipo))) return true;
        DiaDiario dd = semana.get(fechaSelStr());
        return dd != null && dd.getComidasApuntadas().contains(tipo);
    }

    private boolean hoyNo(String tipo) {
        DiaDiario dd = semana.get(fechaSelStr());
        return dd != null && dd.getHoyNo().contains(tipo) && !apuntada(tipo);
    }

    // ── Pintado ─────────────────────────────────────────────────────────────

    private void pintar() {
        pintarKcal(false);
        pintarComidas();
    }

    // El anillo, lo que queda y los macros (tablero 1); con el día completo, el tablero 3.
    private void pintarKcal(boolean celebrar) {
        DiaNutricion d = DiaNutricion.de(new ArrayList<>(comidasDia.values()), DiaNutricion.objetivo(prefsManager));
        NumberFormat nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(requireContext()));
        String fecha = fechaSelStr();
        if (!fecha.equals(comidasDe)) return;
        boolean completo = DiaCompleto.completo(semana.get(fecha));

        AnilloDia anillo = findViewById(R.id.anilloDia);
        AnilloDia.Modo modo;
        if (!fecha.equals(fechaContada)) {
            modo = AnilloDia.Modo.CONTAR;
            fechaContada = fecha;
        } else {
            modo = celebrar ? AnilloDia.Modo.CERRAR : AnilloDia.Modo.PASAR;
        }
        float fraccion = d.sinObjetivo ? 0f : d.objetivoKcal > 0 ? (float) d.kcal / d.objetivoKcal : 0f;
        anillo.poner(fraccion, completo, modo);

        TextView tvKcal = findViewById(R.id.tvKcal);
        TextView tvObjetivo = findViewById(R.id.tvKcalObjetivo);
        TextView rotulo = findViewById(R.id.tvQuedanRotulo);
        TextView quedan = findViewById(R.id.tvQuedan);
        TextView unidad = findViewById(R.id.tvQuedanUnidad);
        findViewById(R.id.grupoQuedan).setVisibility(completo ? View.GONE : View.VISIBLE);
        findViewById(R.id.grupoCompleto).setVisibility(completo ? View.VISIBLE : View.GONE);

        String a11y;
        if (d.sinObjetivo) {
            // Sin peso ni altura (GP-103): lo comido, sin objetivo que restar.
            tvObjetivo.setText(R.string.nutricion_kcal_sola);
            rotulo.setText(R.string.nutricion_sin_objetivo_rotulo);
            quedan.setVisibility(View.GONE);
            unidad.setText(R.string.nutricion_sin_objetivo_accion);
            contarKcal(tvKcal, null, d.kcal, 0, modo, nf);
            a11y = getString(R.string.nutricion_sin_objetivo_a11y, nf.format(d.kcal));
        } else {
            tvObjetivo.setText(getString(R.string.nutricion_kcal_de, nf.format(d.objetivoKcal)));
            int restantes = d.kcalRestantes();
            rotulo.setText(restantes >= 0 ? R.string.nutricion_te_quedan : R.string.nutricion_te_pasas);
            quedan.setVisibility(View.VISIBLE);
            unidad.setText(restantes >= 0 ? R.string.nutricion_kcal_para_hoy : R.string.nutricion_kcal_hoy);
            contarKcal(tvKcal, quedan, d.kcal, d.objetivoKcal, modo, nf);
            a11y = getString(R.string.nutricion_kcal_a11y, nf.format(d.kcal), nf.format(d.objetivoKcal),
                    rotulo.getText(), getString(R.string.nutricion_kcal_valor, nf.format(Math.abs(restantes))));
        }
        findViewById(R.id.filaKcal).setContentDescription(completo
                ? getString(R.string.dia_completo_a11y, a11y) : a11y);

        boolean proteinaHecha = !d.sinObjetivo && d.estadoProteinas() == DiaNutricion.Estado.LOGRADO;
        macroProteina(d, completo, proteinaHecha, celebrar);
        macro(R.id.macroCarbos, d.carbohidratos, d.objetivoCarbohidratos, d.sinObjetivo, DiaNutricion.Estado.NORMAL);
        macro(R.id.macroGrasas, d.grasas, d.objetivoGrasas, d.sinObjetivo, DiaNutricion.Estado.NORMAL);

        if (celebrar) {
            // Momento 21: el ✓ salta, el anillo suelta su halo y vibra una vez.
            View check = findViewById(R.id.checkCompleto);
            if (!Movimiento.quieto(requireContext())) Movimiento.saltar(check, Movimiento.CHECK);
            anillo.soltarHalo(Movimiento.CIERRA / 2);
            Movimiento.vibrar(anillo, Movimiento.Vibracion.EXITO);
        }
    }

    // Las cifras del centro y de lo que queda: cuentan al entrar (momento 7), si no, saltan.
    private void contarKcal(TextView tvKcal, @Nullable TextView quedan, int kcal, int objetivo,
                            AnilloDia.Modo modo, NumberFormat nf) {
        if (modo != AnilloDia.Modo.CONTAR || Movimiento.quieto(requireContext())) {
            tvKcal.setText(nf.format(kcal));
            if (quedan != null) quedan.setText(nf.format(Math.abs(objetivo - kcal)));
            return;
        }
        Movimiento.contar(0, Movimiento.CIFRAS, f -> {
            if (!isAdded()) return;
            int ahora = Math.round(kcal * f);
            tvKcal.setText(nf.format(ahora));
            if (quedan != null) quedan.setText(nf.format(Math.abs(objetivo - ahora)));
        });
    }

    private void prepararMacro(int id, int nombre, int colorRes, int grosorDp) {
        View fila = findViewById(id);
        ((TextView) fila.findViewById(R.id.tvMacroNombre)).setText(nombre);
        int c = ContextCompat.getColor(requireContext(), colorRes);
        fila.findViewById(R.id.vMacroPunto).setBackgroundTintList(ColorStateList.valueOf(c));
        LinearProgressIndicator barra = fila.findViewById(R.id.barraMacro);
        barra.setIndicatorColor(c);
        int px = Math.round(grosorDp * getResources().getDisplayMetrics().density);
        barra.setTrackThickness(px);
        barra.setTrackCornerRadius(px / 2);
    }

    // La proteína: «Te faltan 54 g» debajo; con el día completo y llegada, «Proteína
    // conseguida» en dorado con su trofeo (tablero 3), y al celebrarlo su barra brilla.
    private void macroProteina(DiaNutricion d, boolean completo, boolean hecha, boolean celebrar) {
        View fila = findViewById(R.id.macroProteinas);
        TextView nombre = fila.findViewById(R.id.tvMacroNombre);
        TextView valor = fila.findViewById(R.id.tvMacroValor);
        TextView falta = fila.findViewById(R.id.tvMacroFalta);
        ImageView trofeo = fila.findViewById(R.id.ivMacroIcono);
        View punto = fila.findViewById(R.id.vMacroPunto);
        LinearProgressIndicator barra = fila.findViewById(R.id.barraMacro);
        int azul = ContextCompat.getColor(requireContext(), R.color.gp_macro_proteinas);
        int oro = ContextCompat.getColor(requireContext(), R.color.gp_gold);
        int oroTexto = ContextCompat.getColor(requireContext(), R.color.gp_gold_text);
        int gramos = (int) Math.round(d.proteinas);

        boolean dorada = completo && hecha;
        trofeo.setVisibility(dorada ? View.VISIBLE : View.GONE);
        trofeo.setImageTintList(ColorStateList.valueOf(oro));
        punto.setVisibility(dorada ? View.GONE : View.VISIBLE);
        nombre.setText(dorada ? R.string.proteina_conseguida : R.string.nutricion_proteinas);
        nombre.setTextColor(dorada ? oroTexto : color(com.google.android.material.R.attr.colorOnSurface));
        nombre.setTypeface(null, android.graphics.Typeface.BOLD);
        barra.setIndicatorColor(dorada ? oro : azul);

        if (d.sinObjetivo) {
            valor.setText(getString(R.string.nutricion_macro_solo, gramos));
            valor.setTextColor(color(com.google.android.material.R.attr.colorOnSurface));
            barra.setProgressCompat(0, false);
            falta.setVisibility(View.GONE);
            fila.setContentDescription(nombre.getText() + ", " + valor.getText());
            return;
        }
        valor.setText(getString(R.string.nutricion_macro_valor, gramos, d.objetivoProteinas));
        // El rango y sobre qué se calculó (GP-206); un objetivo anterior no los trae y no se inventan.
        TextView base = fila.findViewById(R.id.tvMacroBase);
        StringBuilder detalle = new StringBuilder();
        if (d.proteinaMinima > 0 && d.proteinaMinima < d.objetivoProteinas) {
            detalle.append(getString(R.string.nutricion_proteina_rango, d.objetivoProteinas, d.proteinaMinima));
        }
        int textoBase = ProteinaTexto.base(d.baseProteina);
        if (textoBase != 0) {
            if (detalle.length() > 0) detalle.append('\n');
            detalle.append(getString(textoBase));
        }
        base.setVisibility(detalle.length() > 0 ? View.VISIBLE : View.GONE);
        base.setText(detalle);
        valor.setTextColor(hecha && !dorada ? ContextCompat.getColor(requireContext(), R.color.gp_success)
                : color(com.google.android.material.R.attr.colorOnSurface));
        barra.setProgressCompat(DiaNutricion.porcentaje(d.proteinas, d.objetivoProteinas), animar());
        int faltan = Math.max(0, d.objetivoProteinas - gramos);
        falta.setVisibility(faltan > 0 ? View.VISIBLE : View.GONE);
        falta.setText(getString(R.string.nutricion_te_faltan, faltan));
        String a11y = getString(R.string.nutricion_macro_a11y, nombre.getText(), gramos, d.objetivoProteinas);
        if (detalle.length() > 0) a11y = a11y + ". " + detalle.toString().replace('\n', ' ');
        fila.setContentDescription(faltan > 0 ? getString(R.string.nutricion_macro_falta_a11y, a11y, faltan) : a11y);
        if (celebrar && dorada) {
            Movimiento.brillar(barra, ContextCompat.getColor(requireContext(), R.color.gp_brillo_oro),
                    Movimiento.CIERRA, Movimiento.BRILLO);
        }
    }

    private void macro(int id, double valor, int objetivo, boolean sinObjetivo, DiaNutricion.Estado estado) {
        View fila = findViewById(id);
        TextView nombre = fila.findViewById(R.id.tvMacroNombre);
        TextView tvValor = fila.findViewById(R.id.tvMacroValor);
        LinearProgressIndicator barra = fila.findViewById(R.id.barraMacro);
        int gramos = (int) Math.round(valor);
        if (sinObjetivo) {
            tvValor.setText(getString(R.string.nutricion_macro_solo, gramos));
            tvValor.setTextColor(color(com.google.android.material.R.attr.colorOnSurface));
            barra.setProgressCompat(0, false);
            fila.setContentDescription(nombre.getText() + ", " + tvValor.getText());
            return;
        }
        tvValor.setText(getString(R.string.nutricion_macro_valor, gramos, objetivo));
        tvValor.setTextColor(colorEstado(estado));
        barra.setProgressCompat(DiaNutricion.porcentaje(valor, objetivo), animar());
        fila.setContentDescription(getString(R.string.nutricion_macro_a11y, nombre.getText(), gramos, objetivo));
    }

    private boolean animar() {
        return !Movimiento.quieto(requireContext());
    }

    // «Día de entreno · Cuerpo completo B»: de las sesiones terminadas ese día. Sin tocar:
    // lo de después del entreno (tablero 11) llega en la 1.6.9.
    private void pintarEntreno(@Nullable DiaDiario dd) {
        View pildora = findViewById(R.id.pildoraEntreno);
        if (dd == null || dd.getEntrenos() <= 0) {
            pildora.setVisibility(View.GONE);
            return;
        }
        String rutina = dd.getRutinaEntreno();
        String texto = rutina != null && !rutina.isEmpty()
                ? getString(R.string.entreno_dia_rutina, rutina) : getString(R.string.entreno_dia);
        ((TextView) findViewById(R.id.tvEntreno)).setText(texto);
        pildora.setContentDescription(texto);
        pildora.setVisibility(View.VISIBLE);
    }

    // «2 días completos esta semana» y lo que hubo ese día; solo con el día completo. Va como
    // una línea más de «Tu semana».
    private void pintarSemanaCompleta(@Nullable DiaDiario dd, boolean celebrar) {
        if (!DiaCompleto.completo(dd)) {
            semanaExtra = null;
            pintarTuSemana();
            return;
        }
        List<DiaDiario> dias = new ArrayList<>(semana.values());
        int n = DiaCompleto.completosEnSemana(dias);
        DiaNutricion d = DiaNutricion.de(new ArrayList<>(comidasDia.values()), DiaNutricion.objetivo(prefsManager));
        boolean proteina = !d.sinObjetivo && d.estadoProteinas() == DiaNutricion.Estado.LOGRADO;
        String titulo = getResources().getQuantityString(R.plurals.dias_completos_semana, n, n);
        String texto = getString(frase(DiaCompleto.frase(esHoy(), dd.getEntrenos() > 0, proteina)));
        semanaExtra = titulo + ". " + texto;
        pintarTuSemana();
        if (celebrar) Movimiento.entrarUna(findViewById(R.id.cardSemana), 1400, Movimiento.ENTRA, 16);
    }

    // Lo comido cada día de la semana elegida, para el resumen. Un fallo no se enseña: «Tu semana»
    // es un atajo a la semana completa, y sin las cifras la tarjeta no sale.
    private void cargarResumenSemana(Calendar lunes, Calendar domingo, int turnoDiarioDeEsta) {
        int uid = prefsManager.getUsuarioId();
        if (uid == -1) return;
        String desde = fechaStr(lunes);
        int miTurno = ++turnoResumen;
        comidaApi.getResumen(uid, desde, fechaStr(domingo)).enqueue(new ApiCallback<List<ResumenDiarioNutricion>>() {
            @Override
            public void onOk(List<ResumenDiarioNutricion> lista) {
                if (!isAdded() || miTurno != turnoResumen || turnoDiarioDeEsta != turnoDiario) return;
                resumenSemana.clear();
                if (lista != null) for (ResumenDiarioNutricion r : lista) if (r.getFecha() != null) resumenSemana.put(r.getFecha(), r);
                semanaResumida = desde;
                pintarTuSemana();
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: sin las cifras, «Tu semana» no sale; la semana completa sigue en el historial.
                if (!isAdded() || miTurno != turnoResumen) return;
                semanaResumida = null;
                pintarTuSemana();
            }
        });
    }

    // «Tu semana»: «3 de 4 días con tu proteína» y «2.240 kcal de media», con enlace a la semana.
    private void pintarTuSemana() {
        View card = findViewById(R.id.cardSemana);
        Calendar lunes = lunesDe(fechaSel);
        if (semanaResumida == null || !semanaResumida.equals(fechaStr(lunes)) || comidasDia == null) {
            card.setVisibility(View.GONE);
            return;
        }
        List<SemanaNutricion.Entrada> siete = new ArrayList<>();
        Calendar dia = (Calendar) lunes.clone();
        for (int i = 0; i < 7; i++) {
            String f = fechaStr(dia);
            ResumenDiarioNutricion r = resumenSemana.get(f);
            DiaDiario d = semana.get(f);
            boolean hayComida = r != null && (r.getCalorias() > 0 || r.getProteinas() > 0);
            siete.add(new SemanaNutricion.Entrada(f, hayComida, r != null ? r.getProteinas() : 0,
                    r != null ? r.getCalorias() : 0, DiaCompleto.completo(d), d != null && d.getEntrenos() > 0,
                    d != null ? d.getAguaMl() : 0));
            dia.add(Calendar.DAY_OF_MONTH, 1);
        }
        ResultadoNutricional objetivo = DiaNutricion.objetivo(prefsManager);
        SemanaNutricion sn = SemanaNutricion.de(siete, fechaStr(Calendar.getInstance()),
                objetivo != null ? objetivo.proteinas : 0, objetivo != null ? objetivo.calorias : 0, 0);
        if (sn.vacia()) {
            card.setVisibility(View.GONE);
            return;
        }
        NumberFormat nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(requireContext()));
        int dias = sn.diasConDatos();
        String titulo = sn.hayObjetivoProteina()
                ? getResources().getQuantityString(R.plurals.nutricion_semana_proteina, dias, sn.proteinaConseguida(), dias)
                : getResources().getQuantityString(R.plurals.nutricion_semana_apuntados, dias, dias);
        Integer media = sn.mediaKcal();
        String texto = media != null ? getString(R.string.nutricion_semana_kcal, nf.format(media)) : "";
        ((TextView) findViewById(R.id.tvSemanaTitulo)).setText(titulo);
        TextView tvTexto = findViewById(R.id.tvSemanaTexto);
        tvTexto.setText(texto);
        tvTexto.setVisibility(texto.isEmpty() ? View.GONE : View.VISIBLE);
        TextView extra = findViewById(R.id.tvSemanaExtra);
        extra.setText(semanaExtra);
        extra.setVisibility(semanaExtra == null ? View.GONE : View.VISIBLE);
        card.setContentDescription(getString(R.string.nutricion_tu_semana) + ". " + titulo
                + (texto.isEmpty() ? "" : ". " + texto) + (semanaExtra == null ? "" : ". " + semanaExtra));
        boolean entra = card.getVisibility() != View.VISIBLE;
        card.setVisibility(View.VISIBLE);
        if (entra) Movimiento.entrarUna(card, 0, Movimiento.ENTRA, 16);
    }

    private static int frase(DiaCompleto.Frase f) {
        switch (f) {
            case HOY_ENTRENO_Y_PROTEINA: return R.string.semana_frase_hoy_entreno_proteina;
            case HOY_ENTRENO: return R.string.semana_frase_hoy_entreno;
            case HOY_PROTEINA: return R.string.semana_frase_hoy_proteina;
            case HOY: return R.string.semana_frase_hoy;
            case ESE_DIA_ENTRENO_Y_PROTEINA: return R.string.semana_frase_dia_entreno_proteina;
            case ESE_DIA_ENTRENO: return R.string.semana_frase_dia_entreno;
            case ESE_DIA_PROTEINA: return R.string.semana_frase_dia_proteina;
            default: return R.string.semana_frase_dia;
        }
    }

    // ── El agua (GP-166) ────────────────────────────────────────────────────

    /**
     * Pinta el agua del día elegido.
     *
     * @param nuevo    índice del vaso que se acaba de llenar (momento 22), o -1.
     * @param mlAntes  los ml antes de ese vaso, para que los litros cuenten; -1 sin cuenta.
     */
    private void pintarAgua(int nuevo, int mlAntes) {
        View card = findViewById(R.id.cardAgua);
        if (!semana.containsKey(fechaSelStr())) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        Locale idioma = FechaUtils.localeDeLaApp(requireContext());
        int vaso = prefsManager.getVasoMl();
        ResultadoNutricional objetivo = DiaNutricion.objetivo(prefsManager);
        double litrosObjetivo = objetivo != null ? objetivo.agua : 0;
        int llenos = Agua.vasosLlenos(aguaMostrada, vaso);
        int total = Agua.vasosAPintar(llenos, Agua.vasosObjetivo(litrosObjetivo, vaso));

        TextView tvLitros = findViewById(R.id.tvAguaLitros);
        if (mlAntes >= 0 && !Movimiento.quieto(requireContext())) {
            int desde = mlAntes;
            int hasta = aguaMostrada;
            Movimiento.contar(0, Movimiento.VASO, f -> {
                if (isAdded()) tvLitros.setText(textoLitros(Math.round(desde + (hasta - desde) * f), litrosObjetivo, idioma));
            });
        } else {
            tvLitros.setText(textoLitros(aguaMostrada, litrosObjetivo, idioma));
        }

        String vasos = getResources().getQuantityString(R.plurals.agua_vasos, llenos, llenos);
        String litros = litrosObjetivo > 0
                ? getString(R.string.agua_a11y, Agua.litros(aguaMostrada, idioma), Agua.litrosObjetivo(litrosObjetivo, idioma), vasos)
                : getString(R.string.agua_a11y_sin_objetivo, Agua.litros(aguaMostrada, idioma), vasos);
        View resumen = findViewById(R.id.resumenAgua);
        resumen.setContentDescription(litros);
        // Quitar un vaso con TalkBack: los vasos son dibujo, la acción va en el resumen.
        if (accionQuitarVaso != View.NO_ID) ViewCompat.removeAccessibilityAction(resumen, accionQuitarVaso);
        accionQuitarVaso = llenos > 0
                ? ViewCompat.addAccessibilityAction(resumen, getString(R.string.agua_quitar_a11y), (v, args) -> {
                    quitarVaso(v);
                    return true;
                })
                : View.NO_ID;
        findViewById(R.id.btnVaso).setContentDescription(getString(R.string.agua_vaso_a11y, vaso));

        LinearLayout filas = findViewById(R.id.filasVasos);
        filas.removeAllViews();
        LinearLayout fila = null;
        int azul = ContextCompat.getColor(requireContext(), R.color.gp_agua);
        int vacio = ContextCompat.getColor(requireContext(), R.color.gp_stroke);
        int alto = Math.round(48 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < total; i++) {
            if (i % VASOS_POR_FILA == 0) {
                fila = new LinearLayout(requireContext());
                fila.setOrientation(LinearLayout.HORIZONTAL);
                fila.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                filas.addView(fila, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, alto));
            }
            fila.addView(vaso(i, llenos, i == nuevo, azul, vacio),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        }
        // La última fila se completa con huecos para que los vasos no se estiren.
        if (fila != null) {
            for (int i = total % VASOS_POR_FILA; i != 0 && i < VASOS_POR_FILA; i++) {
                fila.addView(new View(requireContext()),
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            }
        }
    }

    // Un vaso: el contorno y, encima, el relleno recortado desde abajo. El nuevo se llena
    // de abajo arriba con rebote y crece un poco (momento 22).
    private View vaso(int i, int llenos, boolean nuevo, @ColorInt int azul, @ColorInt int vacio) {
        FrameLayout celda = new FrameLayout(requireContext());
        int lado = Math.round(28 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(lado, lado, Gravity.CENTER);
        ImageView contorno = new ImageView(requireContext());
        contorno.setImageResource(R.drawable.ic_ms_local_drink);
        contorno.setImageTintList(ColorStateList.valueOf(i < llenos ? azul : vacio));
        celda.addView(contorno, lp);
        if (i < llenos) {
            Drawable relleno = ContextCompat.getDrawable(requireContext(), R.drawable.ic_ms_local_drink_fill);
            if (relleno != null) {
                relleno = relleno.mutate();
                relleno.setTint(azul);
                ClipDrawable recorte = new ClipDrawable(relleno, Gravity.BOTTOM, ClipDrawable.VERTICAL);
                recorte.setLevel(10000);
                ImageView lleno = new ImageView(requireContext());
                lleno.setImageDrawable(recorte);
                celda.addView(lleno, new FrameLayout.LayoutParams(lado, lado, Gravity.CENTER));
                if (nuevo && !Movimiento.quieto(requireContext())) {
                    android.animation.ValueAnimator a = android.animation.ValueAnimator.ofInt(0, 10000);
                    a.setDuration(Movimiento.VASO);
                    a.setInterpolator(Movimiento.REBOTE);
                    a.addUpdateListener(an -> recorte.setLevel(Math.min(10000, (int) an.getAnimatedValue())));
                    a.start();
                    celda.setScaleX(1f);
                    celda.animate().scaleX(1.12f).scaleY(1.12f).setDuration(Movimiento.VASO / 3)
                            .setInterpolator(Movimiento.ESTANDAR)
                            .withEndAction(() -> celda.animate().scaleX(1f).scaleY(1f)
                                    .setDuration(Movimiento.VASO * 2 / 3).setInterpolator(Movimiento.ESTANDAR).start())
                            .start();
                }
            }
        }
        Agua.Toque toque = Agua.toque(i, llenos);
        if (toque != Agua.Toque.NADA) {
            celda.setBackgroundResource(fondoPulsableSinBorde());
            celda.setOnClickListener(v -> {
                if (toque == Agua.Toque.QUITAR) quitarVaso(v);
                else anadirVaso(v);
            });
        }
        celda.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return celda;
    }

    private String textoLitros(int ml, double litrosObjetivo, Locale idioma) {
        return litrosObjetivo > 0
                ? getString(R.string.agua_litros, Agua.litros(ml, idioma), Agua.litrosObjetivo(litrosObjetivo, idioma))
                : getString(R.string.agua_litros_sin_objetivo, Agua.litros(ml, idioma));
    }

    private void anadirVaso(View origen) {
        if (!semana.containsKey(fechaSelStr())) return;
        int vaso = prefsManager.getVasoMl();
        int antes = aguaMostrada;
        aguaMostrada = Agua.mas(aguaMostrada, vaso);
        if (aguaMostrada == antes) return;
        Movimiento.vibrar(origen, Movimiento.Vibracion.LIGERA);
        pintarAgua(Agua.vasosLlenos(aguaMostrada, vaso) - 1, antes);
        enviarAgua();
    }

    private void quitarVaso(View origen) {
        if (!semana.containsKey(fechaSelStr())) return;
        int antes = aguaMostrada;
        aguaMostrada = Agua.menos(aguaMostrada, prefsManager.getVasoMl());
        if (aguaMostrada == antes) return;
        Movimiento.vibrar(origen, Movimiento.Vibracion.LIGERA);
        pintarAgua(-1, -1);
        origen.announceForAccessibility(getString(R.string.agua_quitado_a11y));
        enviarAgua();
    }

    // Manda el total que se ve. Si ya hay uno en camino, sale después con el total de ese
    // momento: así nunca llegan dos a la vez y gana el último toque. Si falla, vuelve a lo
    // último guardado y se dice.
    private void enviarAgua() {
        if (enviandoAgua) {
            aguaPendiente = true;
            return;
        }
        enviandoAgua = true;
        final String fecha = fechaSelStr();
        final int valor = aguaMostrada;
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("mililitros", valor);
        diarioApi.guardarAgua(fecha, cuerpo).enqueue(new ApiCallback<Map<String, Object>>() {
            @Override
            public void onOk(Map<String, Object> r) {
                enviandoAgua = false;
                DiaDiario dd = semana.get(fecha);
                if (dd != null) dd.setAguaMl(valor);
                if (!isAdded() || !fecha.equals(fechaSelStr())) {
                    aguaPendiente = false;
                    return;
                }
                aguaConfirmada = valor;
                if (aguaPendiente) {
                    aguaPendiente = false;
                    if (aguaMostrada != valor) enviarAgua();
                }
            }

            @Override
            public void onFail(int code, String message) {
                enviandoAgua = false;
                aguaPendiente = false;
                if (!isAdded() || !fecha.equals(fechaSelStr())) return;
                aguaMostrada = aguaConfirmada;
                pintarAgua(-1, -1);
                UIHelper.mostrarToastError(requireActivity(), getString(R.string.agua_fallo,
                        UiFeedback.mensaje(requireContext(), code, message)));
            }
        });
    }

    // ── Las comidas ─────────────────────────────────────────────────────────

    private void pintarComidas() {
        LinearLayout lista = findViewById(R.id.listaComidas);
        lista.removeAllViews();
        String queToca = esHoy() ? ComidaQueToca.ahora() : null;
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        NumberFormat nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(requireContext()));
        DiaNutricion dia = DiaNutricion.de(new ArrayList<>(comidasDia.values()), DiaNutricion.objetivo(prefsManager));
        int faltaProteina = dia.sinObjetivo ? 0 : (int) Math.round(dia.objetivoProteinas - dia.proteinas);

        for (String tipo : ComidaQueToca.TIPOS) {
            Comida c = comidasDia.get(tipo);
            boolean registrada = apuntada(tipo);
            boolean hoyNo = hoyNo(tipo);
            boolean toca = tipo.equals(queToca) && !registrada && !hoyNo;

            View card = inflater.inflate(R.layout.item_comida_dia, lista, false);
            ((ImageView) card.findViewById(R.id.ivComidaIcono)).setImageResource(ElegirComida.icono(tipo));
            String nombre = getString(ComidaQueToca.titulo(tipo));
            ((TextView) card.findViewById(R.id.tvComidaNombre)).setText(nombre);
            String resumen = hoyNo ? getString(R.string.comida_hoy_no)
                    : toca && faltaProteina > 0 ? getString(R.string.nutricion_toca_proteina, faltaProteina)
                    : resumenComida(c, toca);
            TextView tvResumen = card.findViewById(R.id.tvComidaResumen);
            tvResumen.setText(resumen);
            tvResumen.setVisibility(resumen.isEmpty() ? View.GONE : View.VISIBLE);
            // Apuntada, sus kcal a la derecha; pendiente, el «+» (tablero 15).
            boolean conKcal = c != null && c.getTotalCalorias() > 0 && !hoyNo;
            TextView tvKcal = card.findViewById(R.id.tvComidaKcal);
            String kcalTexto = conKcal ? getString(R.string.unidad_kcal, c.getTotalCalorias()) : "";
            tvKcal.setText(kcalTexto);
            tvKcal.setVisibility(conKcal ? View.VISIBLE : View.GONE);
            card.setContentDescription(nombre + ". " + (resumen.isEmpty() ? kcalTexto : resumen + ". " + kcalTexto));
            card.setOnClickListener(v -> abrirComida(tipo));

            ImageButton mas = card.findViewById(R.id.btnAnadirComida);
            mas.setContentDescription(getString(anadirA11y(tipo)));
            mas.setVisibility(conKcal ? View.GONE : View.VISIBLE);
            if (toca) {
                mas.setBackgroundResource(R.drawable.bg_circulo_primario_pulsable);
                mas.setImageTintList(ColorStateList.valueOf(color(com.google.android.material.R.attr.colorOnPrimary)));
            }
            mas.setOnClickListener(v -> anadirAlimento(tipo));
            // El círculo del icono: naranja tintado si está apuntada, gris si falta (tablero 15).
            View circulo = card.findViewById(R.id.circuloComida);
            ImageView icono = card.findViewById(R.id.ivComidaIcono);
            circulo.setBackgroundTintList(conKcal ? ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.gp_primary_container)) : null);
            icono.setImageTintList(ColorStateList.valueOf(conKcal
                    ? ContextCompat.getColor(requireContext(), R.color.gp_on_primary_container)
                    : color(com.google.android.material.R.attr.colorOnSurfaceVariant)));
            if (lista.getChildCount() > 0) {
                View raya = new View(requireContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        Math.max(1, Math.round(getResources().getDisplayMetrics().density)));
                lp.setMarginStart(Math.round(70 * getResources().getDisplayMetrics().density));
                raya.setLayoutParams(lp);
                raya.setBackgroundColor(color(com.google.android.material.R.attr.colorOutlineVariant));
                raya.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                lista.addView(raya);
            }
            lista.addView(card);
        }

        // «Copiar ayer»: solo hoy y con la de ayer a mano para la comida que toca.
        TextView copiar = findViewById(R.id.btnCopiarAyerDiario);
        ComidaReciente ayer = copiaAyer;
        String tipoAyer = copiaAyerTipo;
        boolean puede = ayer != null && tipoAyer != null && !copiandoAyer && esHoy();
        copiar.setVisibility(puede ? View.VISIBLE : View.GONE);
        if (puede) {
            copiar.setContentDescription(getString(CopiarComida.siCopiar(tipoAyer)));
            copiar.setOnClickListener(v -> copiarAyer(v, ayer, tipoAyer));
        }
    }

    // «Tostadas con aguacate, café con leche» con uno o dos alimentos; «3 alimentos» con más;
    // «Sin registrar», con «· ahora toca» si toca. Las kcal van aparte, a la derecha.
    private String resumenComida(@Nullable Comida c, boolean toca) {
        if (c == null || c.getTotalCalorias() <= 0) {
            return toca ? getString(R.string.comida_resumen, getString(R.string.sin_registrar),
                    getString(R.string.comida_ahora_toca)) : getString(R.string.sin_registrar);
        }
        List<AlimentoComida> alimentos = alimentosPorComida.get(c.getId());
        if (alimentos == null || alimentos.isEmpty()) return "";
        if (alimentos.size() <= 2) {
            List<String> nombres = new ArrayList<>();
            for (AlimentoComida a : alimentos) nombres.add(a.getNombreAlimento());
            return android.text.TextUtils.join(", ", nombres);
        }
        return getResources().getQuantityString(R.plurals.comida_n_alimentos, alimentos.size(), alimentos.size());
    }

    private static int anadirA11y(String tipo) {
        switch (tipo) {
            case "DESAYUNO": return R.string.comida_anadir_desayuno;
            case "ALMUERZO": return R.string.comida_anadir_almuerzo;
            case "COMIDA":   return R.string.comida_anadir_comida;
            case "MERIENDA": return R.string.comida_anadir_merienda;
            default:         return R.string.comida_anadir_cena;
        }
    }

    // Tocar la tarjeta abre la comida del día elegido, como antes; desde la 1.6.8, con su
    // «Hoy no» para que la pantalla de la comida vacía lo enseñe y lo deje quitar.
    private void abrirComida(String tipo) {
        Intent intent = new Intent(requireContext(), ComidaActivity.class);
        intent.putExtra("tipoComida", tipo);
        Comida c = comidasDia.get(tipo);
        intent.putExtra("comidaId", c != null ? c.getId() : -1);
        intent.putExtra("fecha", fechaSelStr());
        DiaDiario dd = semana.get(fechaSelStr());
        intent.putExtra(ComidaActivity.EXTRA_HOY_NO, dd != null && dd.getHoyNo().contains(tipo));
        intent.putExtra(ComidaActivity.EXTRA_DIARIO_LISTO, dd != null);
        recargar.launch(intent);
    }

    // El «+» abre directamente añadir alimento en esa comida y ese día.
    private void anadirAlimento(String tipo) {
        Intent intent = new Intent(requireContext(), AnadirAlimentoActivity.class);
        intent.putExtra("tipoComida", tipo);
        Comida c = comidasDia.get(tipo);
        intent.putExtra("comidaId", c != null ? c.getId() : -1);
        intent.putExtra("fecha", fechaSelStr());
        recargar.launch(intent);
    }

    // ── Utilidades ──────────────────────────────────────────────────────────

    private int colorEstado(DiaNutricion.Estado e) {
        switch (e) {
            case LOGRADO: return ContextCompat.getColor(requireContext(), R.color.gp_success);
            default:      return color(com.google.android.material.R.attr.colorOnSurface);
        }
    }

    private int fondoPulsable() {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        return tv.resourceId;
    }

    private int fondoPulsableSinBorde() {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true);
        return tv.resourceId;
    }

    private int color(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
