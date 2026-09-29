package es.pmdm.gymprofit.ui.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.comida.Comida;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.model.record.Records;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.model.sesion.VolumenMuscular;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.network.ProgramaApi;
import es.pmdm.gymprofit.network.EjercicioApi;
import es.pmdm.gymprofit.network.RecordApi;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.activities.AnadirAlimentoActivity;
import es.pmdm.gymprofit.ui.activities.CrearRutinaActivity;
import es.pmdm.gymprofit.ui.activities.ProgramasActivity;
import es.pmdm.gymprofit.ui.activities.RegistrarSesionActivity;
import es.pmdm.gymprofit.ui.activities.ResumenSesionActivity;
import es.pmdm.gymprofit.ui.widget.ElegirRutinaHoja;
import es.pmdm.gymprofit.utils.AvatarUtils;
import es.pmdm.gymprofit.utils.ComidaQueToca;
import es.pmdm.gymprofit.utils.DiaNutricion;
import es.pmdm.gymprofit.utils.EjercicioNavHelper;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.HoyToca;
import es.pmdm.gymprofit.utils.Marcas;
import es.pmdm.gymprofit.utils.NombreVisible;
import es.pmdm.gymprofit.utils.NavTabs;
import es.pmdm.gymprofit.utils.TiempoRelativo;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.Zonas;

// ============================================================
// InicioFragment — pestaña Inicio (GP-105), según 01-inicio.png.
//
// Cabecera (fecha, «Hola, <nombre>» y avatar que lleva a Progreso) y cuatro tarjetas:
//   · Hoy toca: siguiendo un programa, la que toca en él (lote 1.2.1); sin programa, la
//     rutina propia que más tiempo lleva sin hacerse (HoyToca), con Empezar y Cambiar;
//     «Hecho hoy» si ya hay una sesión hoy; y, sin programa ni rutinas, «Elige tu
//     programa» (a Programas) y «Crea tu primera rutina».
//   · Esta semana (GP-090): sesiones y tiempo desde el lunes, y las seis zonas con
//     sus series de la semana natural (/volumen-muscular?desde=, A.4).
//   · Último récord: la tarjeta dorada de siempre, con lo de antes y cuándo.
//   · Nutrición de hoy: lo mismo que la pestaña Nutrición, calculado por DiaNutricion.
// La silueta muscular y la racha salen de aquí; SiluetaMuscularView sigue en el código.
// ============================================================
public class InicioFragment extends BaseFragment {

    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);
    private final RecordApi recordApi = ApiClient.service(RecordApi.class);
    private final ComidaApi comidaApi = ApiClient.service(ComidaApi.class);
    private final EjercicioApi ejercicioApi = ApiClient.service(EjercicioApi.class);
    private final ProgramaApi programaApi = ApiClient.service(ProgramaApi.class);

    // Lo cargado para Hoy toca; las tres llamadas se esperan unas a otras.
    @Nullable private List<Rutina> propias;
    @Nullable private List<SesionEntrenamiento> sesiones;
    @Nullable private ProgramaQueSigue seguido;
    private boolean falloHoyToca;
    private int pendientesHoyToca;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View avatar = findViewById(R.id.avatarInicio);
        avatar.setContentDescription(getString(R.string.inicio_avatar_a11y));
        avatar.setOnClickListener(v -> irATab(NavTabs.PROGRESO));
        findViewById(R.id.btnRegistrarComida).setOnClickListener(v -> registrarComida());
        findViewById(R.id.cardNutricionHoy).setOnClickListener(v -> irATab(NavTabs.NUTRICION));
    }

    @Override
    public void onResume() {
        super.onResume();
        pintarCabecera();
        cargarHoyTocaYSemana();
        cargarVolumenSemana();
        cargarRecord();
        cargarNutricion();
    }

    // ── Cabecera ─────────────────────────────────────────────────────────────

    private void pintarCabecera() {
        Locale idioma = FechaUtils.localeDeLaApp(requireContext());
        String fecha = new SimpleDateFormat(getString(R.string.home_fecha_patron), idioma).format(new Date());
        // El patrón da «domingo, 27 de septiembre»; el lienzo lo quiere con mayúscula.
        ((TextView) findViewById(R.id.tvFecha)).setText(fecha.isEmpty() ? fecha
                : fecha.substring(0, 1).toUpperCase(idioma) + fecha.substring(1));

        // El nombre para mostrar y, sin él, el de usuario (GP-116).
        String nombre = NombreVisible.de(prefsManager.getNombre(), prefsManager.getUsername());
        if (nombre.isEmpty()) nombre = getString(R.string.home_usuario_defecto);
        ((TextView) findViewById(R.id.tvHola)).setText(getString(R.string.inicio_hola, nombre));
        AvatarUtils.pintar(findViewById(R.id.avatarInicio), nombre, prefsManager.getUsuarioId(),
                color(com.google.android.material.R.attr.colorOnSurface));
    }

    // ── Hoy toca y Esta semana ────────────────────────────────────────────────

    // Rutinas activas, sesiones y el programa que sigue: con las tres se decide Hoy toca,
    // y con las sesiones se cuenta también la semana.
    private void cargarHoyTocaYSemana() {
        int uid = prefsManager.getUsuarioId();
        falloHoyToca = false;
        propias = null;
        sesiones = null;
        seguido = null;

        if (uid == -1) {
            // Invitado: no tiene rutinas, sesiones ni programa propios.
            propias = new ArrayList<>();
            sesiones = new ArrayList<>();
            pendientesHoyToca = 1;
            hoyTocaListo();
        } else {
            pendientesHoyToca = 3;
            rutinaApi.getDeUsuarioActivas(uid).enqueue(new ApiCallback<List<Rutina>>() {
                @Override public void onOk(List<Rutina> l) { propias = l != null ? l : new ArrayList<>(); hoyTocaListo(); }
                @Override public void onFail(int code, String m) { falloHoyToca(code, m); }
            });
            sesionApi.getDeUsuario(uid).enqueue(new ApiCallback<List<SesionEntrenamiento>>() {
                @Override public void onOk(List<SesionEntrenamiento> l) { sesiones = l != null ? l : new ArrayList<>(); hoyTocaListo(); }
                @Override public void onFail(int code, String m) { falloHoyToca(code, m); }
            });
            programaApi.seguido().enqueue(new ApiCallback<ProgramaQueSigue>() {
                @Override public void onOk(ProgramaQueSigue s) { seguido = s; hoyTocaListo(); }
                @Override public void onFail(int code, String m) {
                    // Sin saber el programa, Hoy toca se decide con «Mis rutinas», como antes
                    // de la 1.2.1: se ve algo razonable y Entrenar enseña el fallo con reintentar.
                    seguido = null;
                    hoyTocaListo();
                }
            });
        }
    }

    private void falloHoyToca(int code, String message) {
        if (!isAdded()) return;
        if (!falloHoyToca) {
            falloHoyToca = true;
            UiFeedback.toastError(requireActivity(), code, message);
        }
        pintarHoyTocaError();
        ((TextView) findViewById(R.id.tvSemanaResumen)).setText(R.string.semana_error);
    }

    private void hoyTocaListo() {
        if (!isAdded() || falloHoyToca) return;
        if (--pendientesHoyToca > 0) return;
        pintarHoyToca();
        pintarSemanaSesiones();
    }

    private void pintarHoyToca() {
        MaterialButton principal = findViewById(R.id.btnHoyPrincipal);
        MaterialButton secundario = findViewById(R.id.btnHoySecundario);
        TextView eyebrow = findViewById(R.id.tvHoyEyebrow);
        ImageView icono = findViewById(R.id.ivHoyIcono);
        TextView titulo = findViewById(R.id.tvHoyTitulo);
        TextView sub = findViewById(R.id.tvHoySub);
        MaterialCardView card = findViewById(R.id.cardHoyToca);
        card.setOnClickListener(null);
        card.setClickable(false);
        apilarBotones(false);

        HoyToca.Eleccion eleccion = HoyToca.elegir(seguido, propias, sesiones);
        if (main() != null) main().publicarHoyToca(eleccion == null ? null : eleccion.rutina);

        SesionEntrenamiento deHoy = HoyToca.sesionDeHoy(sesiones, TiempoRelativo.hoy());
        if (deHoy != null) {
            eyebrow.setText(R.string.hecho_hoy);
            icono.setImageResource(R.drawable.ic_ms_check_circle_fill);
            String nombre = deHoy.getRutinaNombre();
            titulo.setText(nombre != null ? nombre : getString(R.string.hecho_hoy_libre));
            sub.setText(getString(R.string.duracion_min, deHoy.getDuracionMinutos()));
            // Tocar la tarjeta abre el resumen de la sesión de hoy.
            card.setOnClickListener(v -> abrirResumen(deHoy, nombre));
            principal.setText(R.string.btn_empezar_otra);
            principal.setIconResource(R.drawable.ic_ms_play_arrow_fill);
            principal.setOnClickListener(v -> elegirRutina());
            secundario.setVisibility(View.GONE);
            return;
        }

        eyebrow.setText(R.string.hoy_toca);
        icono.setImageResource(R.drawable.ic_ms_event_available);

        if (eleccion == null) {
            // Sin programa ni rutinas: lo principal es elegir un programa; crear una rutina
            // propia se queda. Entrenar a su aire sigue en el «+» y en Entrenar.
            titulo.setText(R.string.hoy_toca_sin_rutinas);
            sub.setText(R.string.hoy_toca_sin_rutinas_sub_programa);
            principal.setIcon(null);
            principal.setText(R.string.tu_programa_elige_titulo);
            principal.setOnClickListener(v -> startActivity(new Intent(requireContext(), ProgramasActivity.class)));
            secundario.setVisibility(View.VISIBLE);
            secundario.setText(R.string.btn_crea_primera_rutina);
            secundario.setOnClickListener(v -> {
                if (verificarAccesoRegistrado()) {
                    startActivity(new Intent(requireContext(), CrearRutinaActivity.class));
                }
            });
            // Dos textos largos no caben lado a lado sin cortarse: uno debajo del otro.
            apilarBotones(true);
            return;
        }

        Rutina r = eleccion.rutina;
        titulo.setText(r.getNombre());
        sub.setText(subtituloHoyToca(r, eleccion.ultimoDia));
        principal.setText(R.string.btn_empezar);
        principal.setIconResource(R.drawable.ic_ms_play_arrow_fill);
        principal.setOnClickListener(v -> empezar(r));
        secundario.setVisibility(View.VISIBLE);
        secundario.setText(R.string.btn_cambiar);
        secundario.setOnClickListener(v -> elegirRutina());
    }

    // «6 ejercicios · unos 55 min · la última, hace 4 días»
    private String subtituloHoyToca(Rutina r, @Nullable String ultimoDia) {
        List<String> partes = new ArrayList<>();
        partes.add(getResources().getQuantityString(R.plurals.rutina_num_ejercicios,
                r.getNumEjercicios(), r.getNumEjercicios()));
        if (r.getDuracionMinutos() > 0) partes.add(getString(R.string.hoy_toca_unos_min, r.getDuracionMinutos()));
        String cuando = TiempoRelativo.texto(requireContext(), ultimoDia);
        partes.add(ultimoDia == null || cuando == null
                ? getString(R.string.hoy_toca_nunca)
                : getString(R.string.hoy_toca_ultima, cuando));
        return unir(partes);
    }

    // Los dos botones de Hoy toca, en fila (Empezar · Cambiar) o uno sobre otro.
    private void apilarBotones(boolean apilar) {
        android.widget.LinearLayout fila = findViewById(R.id.filaBotonesHoy);
        fila.setOrientation(apilar ? android.widget.LinearLayout.VERTICAL : android.widget.LinearLayout.HORIZONTAL);
        int margen = Math.round(8 * getResources().getDisplayMetrics().density);
        android.widget.LinearLayout.LayoutParams p1 = (android.widget.LinearLayout.LayoutParams)
                findViewById(R.id.btnHoyPrincipal).getLayoutParams();
        p1.width = apilar ? ViewGroup.LayoutParams.MATCH_PARENT : 0;
        p1.weight = apilar ? 0 : 1;
        android.widget.LinearLayout.LayoutParams p2 = (android.widget.LinearLayout.LayoutParams)
                findViewById(R.id.btnHoySecundario).getLayoutParams();
        p2.width = apilar ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        p2.setMarginStart(apilar ? 0 : margen);
        p2.leftMargin = apilar ? 0 : margen;
        p2.topMargin = apilar ? margen : 0;
        fila.requestLayout();
    }

    private void pintarHoyTocaError() {
        ((TextView) findViewById(R.id.tvHoyTitulo)).setText(R.string.hoy_toca);
        ((TextView) findViewById(R.id.tvHoySub)).setText(R.string.hoy_toca_error);
        MaterialButton principal = findViewById(R.id.btnHoyPrincipal);
        principal.setIcon(null);
        principal.setText(R.string.btn_reintentar);
        principal.setOnClickListener(v -> { cargarHoyTocaYSemana(); cargarVolumenSemana(); });
        findViewById(R.id.btnHoySecundario).setVisibility(View.GONE);
    }

    private void elegirRutina() {
        if (!verificarAccesoRegistrado()) return;
        ElegirRutinaHoja.mostrar(requireActivity(), propias != null ? propias : new ArrayList<>(), this::empezar);
    }

    // Registrar sesión con esa rutina, o en entrenamiento libre si es null.
    private void empezar(@Nullable Rutina r) {
        if (!verificarAccesoRegistrado()) return;
        Intent i = new Intent(requireContext(), RegistrarSesionActivity.class);
        if (r != null) i.putExtra(RegistrarSesionActivity.EXTRA_RUTINA_ID, r.getId());
        startActivity(i);
    }

    private void abrirResumen(SesionEntrenamiento s, @Nullable String nombre) {
        Intent intent = new Intent(requireContext(), ResumenSesionActivity.class);
        intent.putExtra("sesionId", s.getId());
        intent.putExtra(ResumenSesionActivity.EXTRA_ENTRENAMIENTO_LIBRE, s.esEntrenamientoLibre());
        intent.putExtra("rutinaNombre", nombre != null ? nombre : "");
        startActivity(intent);
    }

    // «2 sesiones · 1 h 50 min», desde el lunes a las 00:00.
    private void pintarSemanaSesiones() {
        String lunes = lunesIso();
        int n = 0, minutos = 0;
        if (sesiones != null) {
            for (SesionEntrenamiento s : sesiones) {
                if (s.getFechaInicio() != null && s.getFechaInicio().compareTo(lunes) >= 0) {
                    n++;
                    minutos += Math.max(0, s.getDuracionMinutos());
                }
            }
        }
        String tiempo = minutos >= 60
                ? getString(R.string.duracion_h_min, minutos / 60, minutos % 60)
                : getString(R.string.duracion_min, minutos);
        ((TextView) findViewById(R.id.tvSemanaResumen)).setText(getString(R.string.hoy_toca_separador,
                getResources().getQuantityString(R.plurals.semana_sesiones, n, n), tiempo));
    }

    // Las seis zonas con sus series de la semana natural.
    private void cargarVolumenSemana() {
        int uid = prefsManager.getUsuarioId();
        if (uid == -1) { pintarZonas(null); return; }
        sesionApi.getVolumenMuscularDesde(uid, lunesIso()).enqueue(new ApiCallback<List<VolumenMuscular>>() {
            @Override
            public void onOk(List<VolumenMuscular> volumen) {
                if (!isAdded()) return;
                Map<String, Integer> porMusculo = new LinkedHashMap<>();
                if (volumen != null) {
                    for (VolumenMuscular v : volumen) {
                        if (v.getMusculo() != null) porMusculo.put(v.getMusculo(), v.getSeries());
                    }
                }
                pintarZonas(porMusculo);
            }
            @Override
            public void onFail(int code, String message) {
                if (!isAdded()) return;
                // El aviso ya lo da Hoy toca, que pide a la vez las sesiones; aquí se dice
                // en la tarjeta en vez de enseñar seis zonas pendientes que no lo son.
                ((TextView) findViewById(R.id.tvSemanaZonas)).setText(R.string.semana_error);
                ((ChipGroup) findViewById(R.id.grupoZonas)).removeAllViews();
                findViewById(R.id.tvSemanaNota).setVisibility(View.GONE);
            }
        });
    }

    private void pintarZonas(@Nullable Map<String, Integer> porMusculo) {
        LinkedHashMap<Zonas.Zona, Integer> porZona = Zonas.seriesPorZona(porMusculo);
        ChipGroup grupo = findViewById(R.id.grupoZonas);
        grupo.removeAllViews();
        findViewById(R.id.tvSemanaNota).setVisibility(View.VISIBLE);

        int hechas = 0;
        List<String> hechasA11y = new ArrayList<>();
        List<String> pendientesA11y = new ArrayList<>();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Map.Entry<Zonas.Zona, Integer> e : porZona.entrySet()) {
            String nombre = getString(e.getKey().titulo);
            int series = e.getValue();
            TextView chip = (TextView) inflater.inflate(R.layout.item_chip_zona, grupo, false);
            if (series > 0) {
                hechas++;
                chip.setText(getString(R.string.semana_chip, nombre, series));
                chip.setBackgroundResource(R.drawable.bg_chip_zona_hecha);
                chip.setTextColor(color(com.google.android.material.R.attr.colorOnPrimaryContainer));
                chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
                chip.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_ms_check_circle_fill, 0, 0, 0);
                hechasA11y.add(getResources().getQuantityString(R.plurals.semana_zona_hecha_a11y, series, nombre, series));
            } else {
                chip.setText(nombre);
                chip.setBackgroundResource(R.drawable.bg_chip_zona_pendiente);
                chip.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
                chip.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_ms_radio_button_unchecked, 0, 0, 0);
                pendientesA11y.add(nombre);
            }
            // El icono, a 18 dp y del color del texto.
            android.graphics.drawable.Drawable d = chip.getCompoundDrawablesRelative()[0];
            if (d != null) {
                int px = Math.round(18 * getResources().getDisplayMetrics().density);
                d.setBounds(0, 0, px, px);
                chip.setCompoundDrawablesRelative(d, null, null, null);
                chip.setCompoundDrawableTintList(ColorStateList.valueOf(chip.getCurrentTextColor()));
            }
            grupo.addView(chip);
        }

        TextView tvZonas = findViewById(R.id.tvSemanaZonas);
        tvZonas.setText(getString(R.string.semana_zonas, hechas, porZona.size()));

        // TalkBack lee la tarjeta de una vez: cuántas, cuáles hechas y cuáles no.
        StringBuilder a11y = new StringBuilder(tvZonas.getText()).append(". ");
        a11y.append(hechasA11y.isEmpty() ? getString(R.string.semana_ninguna_hecha_a11y)
                : getString(R.string.semana_hechas_a11y, unirComas(hechasA11y)));
        if (!pendientesA11y.isEmpty()) {
            a11y.append(' ').append(getString(R.string.semana_pendientes_a11y, unirComas(pendientesA11y)));
        }
        grupo.setContentDescription(a11y.toString());
        grupo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        tvZonas.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    // Lunes de esta semana a las 00:00, en la hora del teléfono.
    static String lunesIso() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        cal.add(Calendar.DAY_OF_MONTH, -((dow + 5) % 7));
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(cal.getTime());
    }

    // ── Último récord ────────────────────────────────────────────────────────

    private void cargarRecord() {
        MaterialCardView card = findViewById(R.id.cardRecordHome);
        if (prefsManager.getUsuarioId() == -1) { card.setVisibility(View.GONE); return; }

        recordApi.getRecords(null).enqueue(new ApiCallback<Records>() {
            @Override
            public void onOk(Records r) {
                if (!isAdded()) return;
                if (r == null || r.getRecords().isEmpty()) { card.setVisibility(View.GONE); return; }
                pintarRecord(r.getRecords().get(0));
            }
            @Override
            public void onFail(int code, String message) {
                if (!isAdded()) return;
                // La tarjeta es un extra de la pantalla: si falla se queda oculta y el
                // resto de Inicio se lee igual. Queda rastro en el log.
                Log.w("GymProFit", "cargarRecord falló (" + code + "): " + message);
                card.setVisibility(View.GONE);
            }
        });
    }

    // «Press de banca · 90 kg × 10» y «Antes: 82,5 kg × 10 · hace 2 días».
    private void pintarRecord(Record record) {
        MaterialCardView card = findViewById(R.id.cardRecordHome);
        card.setVisibility(View.VISIBLE);
        String nombre = record.nombre(FechaUtils.localeDeLaApp(requireContext()));
        String marca = Marcas.texto(requireContext(), record);
        ((TextView) findViewById(R.id.tvRecordEjercicio)).setText(getString(R.string.hoy_toca_separador, nombre, marca));

        TextView antes = findViewById(R.id.tvRecordAntes);
        String cuando = TiempoRelativo.texto(requireContext(), record.getFecha());
        String anterior = Marcas.anteriorSolo(requireContext(), record);
        if (anterior != null && cuando != null) {
            antes.setText(getString(R.string.ultimo_record_sub, anterior, cuando));
        } else {
            antes.setText(cuando);
        }
        antes.setVisibility(antes.getText().length() == 0 ? View.GONE : View.VISIBLE);
        card.setContentDescription(getString(R.string.ultimo_record) + ". "
                + ((TextView) findViewById(R.id.tvRecordEjercicio)).getText() + ". " + antes.getText());

        // La ficha espera el ejercicio entero en extras: se pide al pulsar.
        card.setOnClickListener(v ->
                ejercicioApi.getPorId(record.getEjercicioId()).enqueue(new ApiCallback<Ejercicio>() {
                    @Override
                    public void onOk(Ejercicio ejercicio) {
                        if (!isAdded() || ejercicio == null) return;
                        EjercicioNavHelper.abrir(requireContext(), ejercicio);
                    }
                    @Override
                    public void onFail(int code, String message) {
                        if (!isAdded()) return;
                        UiFeedback.toastError(requireActivity(), code, message);
                    }
                }));
    }

    // ── Nutrición de hoy ─────────────────────────────────────────────────────

    private void cargarNutricion() {
        int uid = prefsManager.getUsuarioId();
        if (uid == -1) { pintarNutricion(null); return; }
        comidaApi.getDeUsuarioFecha(uid, TiempoRelativo.hoy()).enqueue(new ApiCallback<List<Comida>>() {
            @Override public void onOk(List<Comida> lista) { if (isAdded()) pintarNutricion(lista); }
            @Override public void onFail(int code, String message) {
                if (!isAdded()) return;
                // Sin las comidas no se sabe cuánto se ha comido: mejor no enseñar un 0
                // que parezca un dato. Se avisa y la tarjeta se queda con lo último.
                UiFeedback.toastError(requireActivity(), code, message);
            }
        });
    }

    private void pintarNutricion(@Nullable List<Comida> comidas) {
        DiaNutricion d = DiaNutricion.de(comidas, DiaNutricion.objetivo(prefsManager));
        NumberFormat nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(requireContext()));
        TextView kcal = findViewById(R.id.tvKcalHoy);
        kcal.setText(nf.format(d.kcal));
        ((TextView) findViewById(R.id.tvKcalObjetivoHoy)).setText(getString(R.string.nutricion_de_kcal, nf.format(d.objetivoKcal)));
        ((LinearProgressIndicator) findViewById(R.id.barraKcalHoy)).setProgressCompat(
                DiaNutricion.porcentaje(d.kcal, d.objetivoKcal), false);

        macro(R.id.tvMacroProtHoy, R.string.macro_prot_corto, d.proteinas, d.objetivoProteinas, d.estadoProteinas());
        macro(R.id.tvMacroCarbosHoy, R.string.macro_carbos_corto, d.carbohidratos, d.objetivoCarbohidratos, d.estadoCarbohidratos());
        macro(R.id.tvMacroGrasasHoy, R.string.macro_grasas_corto, d.grasas, d.objetivoGrasas, d.estadoGrasas());

        findViewById(R.id.filaKcalHoy).setContentDescription(
                getString(R.string.nutricion_hoy_a11y, nf.format(d.kcal), nf.format(d.objetivoKcal)));
    }

    private void macro(int id, int formato, double valor, int objetivo, DiaNutricion.Estado estado) {
        TextView tv = findViewById(id);
        tv.setText(getString(formato, (int) Math.round(valor), objetivo));
        tv.setTextColor(colorEstado(estado));
    }

    private int colorEstado(DiaNutricion.Estado e) {
        switch (e) {
            case LOGRADO: return ContextCompat.getColor(requireContext(), R.color.gp_success);
            case PASADO:  return color(androidx.appcompat.R.attr.colorError);
            default:      return color(com.google.android.material.R.attr.colorOnSurface);
        }
    }

    // Registrar comida → añadir alimento, hoy, en la comida que toca por la hora.
    private void registrarComida() {
        if (!verificarAccesoRegistrado()) return;
        Intent i = new Intent(requireContext(), AnadirAlimentoActivity.class);
        i.putExtra("tipoComida", ComidaQueToca.ahora());
        i.putExtra("comidaId", -1);
        i.putExtra("fecha", TiempoRelativo.hoy());
        startActivity(i);
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private String unir(List<String> partes) {
        String s = partes.get(0);
        for (int i = 1; i < partes.size(); i++) s = getString(R.string.hoy_toca_separador, s, partes.get(i));
        return s;
    }

    private static String unirComas(List<String> l) {
        return android.text.TextUtils.join(", ", l);
    }

    private int color(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
