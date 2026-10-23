package es.pmdm.gymprofit.ui.fragments;

import es.pmdm.gymprofit.envivo.LogicaSesion;
import es.pmdm.gymprofit.envivo.SesionEnCursoRepositorio;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

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
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.record.Records;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.model.sesion.VolumenMuscular;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ProgramaApi;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.activities.AnadirAlimentoActivity;
import es.pmdm.gymprofit.ui.activities.CrearRutinaActivity;
import es.pmdm.gymprofit.ui.activities.PerfilActivity;
import es.pmdm.gymprofit.ui.activities.ProgramasActivity;
import es.pmdm.gymprofit.envivo.EmpezarSesion;
import es.pmdm.gymprofit.ui.activities.ResumenSesionActivity;
import es.pmdm.gymprofit.ui.inicio.InicioEntrenos;
import es.pmdm.gymprofit.ui.inicio.InicioHoyDetalle;
import es.pmdm.gymprofit.ui.inicio.InicioProgreso;
import es.pmdm.gymprofit.ui.inicio.InicioTuDia;
import es.pmdm.gymprofit.ui.widget.ElegirRutinaHoja;
import es.pmdm.gymprofit.ui.widget.MenuRutina;
import es.pmdm.gymprofit.ui.widget.SiluetaMuscularView;
import es.pmdm.gymprofit.utils.AvatarUtils;
import es.pmdm.gymprofit.utils.ComidaQueToca;
import es.pmdm.gymprofit.utils.DiaNutricion;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.FraseProfi;
import es.pmdm.gymprofit.utils.HistorialSesiones;
import es.pmdm.gymprofit.utils.HoyToca;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.PrimerosPasos;
import es.pmdm.gymprofit.utils.NombreVisible;
import es.pmdm.gymprofit.utils.NavTabs;
import es.pmdm.gymprofit.utils.ResultadoNutricional;
import es.pmdm.gymprofit.utils.TiempoRelativo;
import es.pmdm.gymprofit.utils.TuPrograma;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;
import es.pmdm.gymprofit.utils.Zonas;

// ============================================================
// InicioFragment — pestaña Inicio, según el tablero 13 de GP-216 (22-10-2026).
//
// Cabecera (fecha, «Hola, <nombre>» y avatar, que abre Perfil y ajustes), el bocadillo de Profi y
// seis bloques:
//   · Hoy toca: siguiendo un programa, la que toca en él (lote 1.2.1); sin programa, la rutina
//     propia que más tiempo lleva sin hacerse (HoyToca), con Empezar y Cambiar; «Hecho hoy» si ya
//     hay una sesión hoy; y, sin programa ni rutinas, «Elige tu programa» (a Programas) y «Crea tu
//     primera rutina». Con rutina de hoy, InicioHoyDetalle suma los ejercicios en carrusel y las
//     zonas en el cuerpo, y la tarjeta crece hasta la rutina al pulsarla.
//   · Tu día (InicioTuDia): calorías, proteína y agua, y la comida que toca.
//   · Tus músculos esta semana: las series por zona desde el lunes (/volumen-muscular?desde=).
//   · Tu progreso (InicioProgreso): kilos por semana y el último récord.
//   · Tus últimos entrenos (InicioEntrenos).
//   · Primeros pasos y el fundador, solo el primer día (GP-103).
// Cada bloque con datos de red dice su carga, su vacío y su fallo dentro (GP-211), sin toast.
// ============================================================
public class InicioFragment extends BaseFragment {

    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);
    private final ProgramaApi programaApi = ApiClient.service(ProgramaApi.class);

    // Lo cargado para Hoy toca; las tres llamadas se esperan unas a otras.
    @Nullable private List<Rutina> propias;
    @Nullable private List<SesionEntrenamiento> sesiones;
    @Nullable private ProgramaQueSigue seguido;
    private boolean falloHoyToca;
    private int pendientesHoyToca;

    private InicioTuDia tuDia;
    private InicioProgreso progreso;
    private InicioEntrenos entrenos;
    private InicioHoyDetalle hoyDetalle;
    private VistaEstado estadoMusculos;

    // Lo que Profi necesita saber, según va llegando.
    @Nullable private Records records;
    private boolean recordsListos;
    @Nullable private String primerEjercicioHoy;
    @Nullable private String zonasHoy;
    private int proteinaFalta;

    // Al volver de una rutina abierta desde Hoy toca (editarla o borrarla): se recarga todo.
    private final ActivityResultLauncher<Intent> lanzadorRutina = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (isAdded()) cargarTodo();
            });

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
        avatar.setContentDescription(getString(R.string.avatar_perfil_a11y));
        avatar.setOnClickListener(v -> startActivity(new Intent(requireContext(), PerfilActivity.class)));

        hoyDetalle = new InicioHoyDetalle(this, view);
        tuDia = new InicioTuDia(this, view, prefsManager, () -> irATab(NavTabs.NUTRICION), this::registrarComida,
                this::alTenerComidas);
        progreso = new InicioProgreso(this, view, prefsManager, () -> irATab(NavTabs.PROGRESO), this::alTenerRecords);
        entrenos = new InicioEntrenos(this, view, () -> irAProgreso(ProgresoFragment.HISTORIAL));
        estadoMusculos = new VistaEstado(view.findViewById(R.id.estadoMusculos));
        findViewById(R.id.enlaceMusculos).setOnClickListener(v -> irAProgreso(ProgresoFragment.RECORDS));
        prepararPrimerDia();

        // Hoy toca dice «En curso» con la sesión de su rutina en marcha (GP-012). Al
        // empezar, guardar o descartar se repinta con lo ya cargado.
        // Si la sesión desaparece (se guardó, quizá sola al abrir la app), Hoy toca y la
        // semana ya no son los de antes: se vuelven a pedir.
        SesionEnCursoRepositorio.get(requireContext()).getSesion().observe(getViewLifecycleOwner(), s -> {
            if (habiaSesion && s == null) {
                cargarTodo();
            } else if (hoyTocaPintado) {
                pintarHoyToca();
            }
            habiaSesion = s != null;
        });
    }

    // Si Hoy toca ya se pintó con datos: antes no hay nada que repintar.
    private boolean hoyTocaPintado;
    // Si en el último aviso había sesión en curso.
    private boolean habiaSesion;

    @Override
    public void onResume() {
        super.onResume();
        pintarCabecera();
        cargarTodo();
    }

    private void cargarTodo() {
        cargarHoyTocaYSemana();
        cargarVolumenSemana();
        progreso.cargar();
        tuDia.cargar();
    }

    private void irAProgreso(int seccion) {
        if (main() != null) main().irAProgreso(seccion);
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
        AvatarUtils.pintar(findViewById(R.id.avatarInicio), nombre, prefsManager.getUsuarioId());
    }

    // ── Profi ────────────────────────────────────────────────────────────────

    // Una frase con un dato tuyo que sea verdad; sin dato, no hay bocadillo (FraseProfi).
    private void pintarProfi() {
        View fila = findViewById(R.id.filaProfi);
        if (fila == null) return;
        if (sesiones == null) {
            fila.setVisibility(View.GONE);
            return;
        }
        HoyToca.Eleccion eleccion = HoyToca.elegir(seguido, propias, sesiones);
        FraseProfi.Datos d = new FraseProfi.Datos();
        List<SesionEntrenamiento> hechas = new ArrayList<>();
        for (SesionEntrenamiento s : HistorialSesiones.masRecientePrimero(sesiones)) {
            if (s.isCompletada()) hechas.add(s);
        }
        d.hayEntrenos = !hechas.isEmpty();
        d.entrenadoHoy = HoyToca.sesionDeHoy(sesiones, TiempoRelativo.hoy()) != null;
        if (d.hayEntrenos) {
            d.ultimaRutina = hechas.get(0).getRutinaNombre();
            d.recordsUltima = InicioEntrenos.recordsDe(records, hechas.get(0).getId());
        }
        d.rutinaHoy = eleccion == null ? null : eleccion.rutina.getNombre();
        d.primerEjercicioHoy = primerEjercicioHoy;
        d.sesionesSemana = contarSemana();
        d.proteinaFalta = proteinaFalta;
        // Los récords de la última sesión se saben cuando llegan: antes, Profi no los da por ceros.
        if (!recordsListos) d.recordsUltima = 0;

        FraseProfi f = FraseProfi.elegir(d);
        CharSequence texto = textoProfi(f);
        if (texto == null) {
            fila.setVisibility(View.GONE);
            return;
        }
        ((TextView) findViewById(R.id.tvProfi)).setText(texto);
        fila.setVisibility(View.VISIBLE);
    }

    @Nullable
    private CharSequence textoProfi(FraseProfi f) {
        switch (f.tipo) {
            case SIN_ENTRENOS:
                return negrita(getString(R.string.profi_sin_entrenos), getString(R.string.profi_sin_entrenos_b));
            case ENTRENADO_HOY:
                String hecho = f.rutina == null ? getString(R.string.profi_hoy_libre)
                        : getString(R.string.profi_hoy_rutina, f.rutina);
                return negrita(hecho, f.n > 0
                        ? getResources().getQuantityString(R.plurals.profi_hoy_records, f.n, f.n)
                        : getString(R.string.profi_hoy_bien));
            case RECORDS_ULTIMA:
                return negrita(getResources().getQuantityString(R.plurals.profi_records, f.n, f.rutina, f.n),
                        deHoy(f));
            case SEMANA:
                return negrita(getResources().getQuantityString(R.plurals.profi_semana, f.n, f.n), deHoy(f));
            case PROTEINA:
                return negrita(getString(R.string.profi_proteina, f.n), getString(R.string.profi_proteina_b));
            case HOY_TOCA:
                return negrita("", deHoy(f));
            default:
                return null;
        }
    }

    // «Hoy empezamos por Press banca.» o «Hoy toca Torso B.»; null si no hay rutina de hoy.
    @Nullable
    private String deHoy(FraseProfi f) {
        if (f.ejercicioHoy != null) return getString(R.string.profi_empezamos_por, f.ejercicioHoy);
        if (f.rutinaHoy != null) return getString(R.string.profi_hoy_toca, f.rutinaHoy);
        return null;
    }

    // «base» normal y, detrás, «destacada» en negrita.
    private static CharSequence negrita(String base, @Nullable String destacada) {
        SpannableStringBuilder sb = new SpannableStringBuilder(base);
        if (destacada != null && !destacada.isEmpty()) {
            if (sb.length() > 0) sb.append(' ');
            int desde = sb.length();
            sb.append(destacada);
            sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), desde, sb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    private void alTenerRecords(@Nullable Records r) {
        records = r;
        recordsListos = true;
        entrenos.setRecords(r);
        pintarProfi();
    }

    // Las comidas de hoy: la primera marca «Primeros pasos» y la proteína que falta, a Profi.
    private void alTenerComidas(@Nullable List<Comida> comidas) {
        if (comidas != null && !comidas.isEmpty() && !prefsManager.getUsername().isEmpty()) {
            prefsManager.marcarPrimeraComida(prefsManager.getUsername());
            pintarPrimerDia();
        }
        ResultadoNutricional objetivo = DiaNutricion.objetivo(prefsManager);
        DiaNutricion d = DiaNutricion.de(comidas, objetivo);
        proteinaFalta = d.sinObjetivo ? 0 : Math.max(0, d.objetivoProteinas - (int) Math.round(d.proteinas));
        pintarProfi();
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
            // Sin id de cuenta: ni rutinas, ni sesiones, ni programa propios.
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

    // El fallo se dice dentro de cada bloque (Hoy toca y Tus últimos entrenos), sin toast: antes salía
    // el mismo mensaje en dos sitios y además como aviso que se va (GP-211).
    private void falloHoyToca(int code, String message) {
        if (!isAdded() || falloHoyToca) return;
        falloHoyToca = true;
        pintarHoyTocaError();
        entrenos.fallo(code, message, this::cargarHoyTocaYSemana);
        ((TextView) findViewById(R.id.tvSemanaResumen)).setText(R.string.semana_error);
        pintarProfi();
    }

    private void hoyTocaListo() {
        if (!isAdded() || falloHoyToca) return;
        if (--pendientesHoyToca > 0) return;
        pintarHoyToca();
        pintarSemanaSesiones();
        entrenos.setSesiones(sesiones);
        pintarPrimerDia();
        pintarProfi();
    }

    private void pintarHoyToca() {
        MaterialButton principal = findViewById(R.id.btnHoyPrincipal);
        MaterialButton secundario = findViewById(R.id.btnHoySecundario);
        TextView eyebrow = findViewById(R.id.tvHoyEyebrow);
        ImageView icono = findViewById(R.id.ivHoyIcono);
        TextView titulo = findViewById(R.id.tvHoyTitulo);
        TextView sub = findViewById(R.id.tvHoySub);
        TextView ultima = findViewById(R.id.tvHoyUltima);
        MaterialCardView card = findViewById(R.id.cardHoyToca);
        card.setOnClickListener(null);
        card.setClickable(false);
        ultima.setVisibility(View.GONE);
        apilarBotones(false);

        HoyToca.Eleccion eleccion = HoyToca.elegir(seguido, propias, sesiones);
        if (main() != null) main().publicarHoyToca(eleccion == null ? null : eleccion.rutina);
        hoyTocaPintado = true;
        principal.setContentDescription(null);

        // La de hoy está en curso: la tarjeta lo dice y vuelve a ella.
        SesionEnCursoRepositorio repo = SesionEnCursoRepositorio.get(requireContext());
        SesionEnCurso enCurso = repo.actual();
        if (eleccion != null && enCurso != null && enCurso.rutinaId != null
                && enCurso.rutinaId == eleccion.rutina.getId()) {
            hoyDetalle.ocultar();
            eyebrow.setText(R.string.envivo_en_curso);
            icono.setImageResource(R.drawable.ic_ms_timer);
            titulo.setText(eleccion.rutina.getNombre());
            sub.setText(getString(R.string.duracion_min, LogicaSesion.minutosReloj(enCurso.inicioMs, repo.ahora())));
            principal.setText(R.string.envivo_volver);
            principal.setIconResource(R.drawable.ic_ms_play_arrow_fill);
            principal.setContentDescription(getString(R.string.envivo_volver_rutina_a11y, eleccion.rutina.getNombre()));
            principal.setOnClickListener(v -> EmpezarSesion.abrir(requireActivity()));
            secundario.setVisibility(View.GONE);
            return;
        }

        SesionEntrenamiento deHoy = HoyToca.sesionDeHoy(sesiones, TiempoRelativo.hoy());
        if (deHoy != null) {
            hoyDetalle.ocultar();
            eyebrow.setText(R.string.hecho_hoy);
            icono.setImageResource(R.drawable.ic_ms_check_circle_fill);
            String nombre = deHoy.getRutinaNombre();
            titulo.setText(nombre != null ? nombre : getString(R.string.hecho_hoy_libre));
            sub.setText(getString(R.string.duracion_min, deHoy.getDuracionMinutos()));
            // Tocar la tarjeta abre el resumen de la sesión de hoy, y la tarjeta crece hasta él.
            card.setOnClickListener(v -> abrirResumen(deHoy, card));
            principal.setText(R.string.btn_empezar_otra);
            principal.setIconResource(R.drawable.ic_ms_play_arrow_fill);
            principal.setOnClickListener(v -> elegirRutina());
            secundario.setVisibility(View.GONE);
            return;
        }

        eyebrow.setText(R.string.hoy_toca);
        icono.setImageResource(R.drawable.ic_ms_event_available);

        if (eleccion == null && !prefsManager.getProgramaPendienteCodigo().isEmpty()) {
            hoyDetalle.ocultar();
            pintarProgramaPendiente(titulo, sub, principal, secundario);
            return;
        }

        if (eleccion == null) {
            hoyDetalle.ocultar();
            // Sin programa ni rutinas: lo principal es elegir un programa; crear una rutina
            // propia se queda. Entrenar a su aire sigue en el «+» y en Entrenar.
            titulo.setText(R.string.hoy_toca_sin_rutinas);
            sub.setText(R.string.hoy_toca_sin_rutinas_sub_programa);
            principal.setIcon(null);
            principal.setText(R.string.tu_programa_elige_titulo);
            principal.setOnClickListener(v -> startActivity(new Intent(requireContext(), ProgramasActivity.class)));
            secundario.setVisibility(View.VISIBLE);
            secundario.setText(R.string.btn_crea_primera_rutina);
            secundario.setOnClickListener(v -> startActivity(new Intent(requireContext(), CrearRutinaActivity.class)));
            // Dos textos largos no caben lado a lado sin cortarse: uno debajo del otro.
            apilarBotones(true);
            return;
        }

        Rutina r = eleccion.rutina;
        // Las zonas de otra rutina no se enseñan ni un instante con la de ahora.
        if (rutinaDelDetalle != r.getId()) zonasHoy = null;
        eyebrow.setText(eyebrowDelPrograma(eleccion));
        titulo.setText(r.getNombre());
        String base = subtituloHoyToca(r);
        sub.setText(zonasHoy == null ? base : getString(R.string.hoy_toca_separador, base, zonasHoy));
        String cuando = TiempoRelativo.texto(requireContext(), eleccion.ultimoDia);
        ultima.setText(eleccion.ultimoDia == null || cuando == null
                ? getString(R.string.inicio_hoy_nunca) : getString(R.string.inicio_hoy_ultima, cuando));
        ultima.setVisibility(View.VISIBLE);
        principal.setText(R.string.btn_empezar);
        principal.setIconResource(R.drawable.ic_ms_play_arrow_fill);
        principal.setOnClickListener(v -> empezar(r));
        secundario.setVisibility(View.VISIBLE);
        secundario.setText(R.string.btn_cambiar);
        secundario.setOnClickListener(v -> elegirRutina());
        // Tocar la tarjeta abre la rutina, y la tarjeta crece hasta ella.
        card.setOnClickListener(v -> MenuRutina.abrirDetalle(requireActivity(), lanzadorRutina, r, card));
        // El primer día, «Empezar» llama la atención dos veces, una sola vez (GP-104).
        if (esPrimerDia() && !empezarAnimado) {
            empezarAnimado = true;
            Movimiento.respirar(principal, 1300);
            Movimiento.respirar(principal, 2700);
        }
        mostrarDetalleDeHoy(r);
    }

    // Los ejercicios en carrusel y las zonas en el cuerpo; solo se piden cuando cambia la rutina.
    private int rutinaDelDetalle = -1;

    private void mostrarDetalleDeHoy(Rutina r) {
        if (rutinaDelDetalle == r.getId() && findViewById(R.id.scrollHoyEjercicios).getVisibility() == View.VISIBLE) {
            return;
        }
        rutinaDelDetalle = r.getId();
        zonasHoy = null;
        primerEjercicioHoy = null;
        hoyDetalle.mostrar(r, new InicioHoyDetalle.AlInformar() {
            @Override
            public void ejercicios(@Nullable String primerEjercicio) {
                primerEjercicioHoy = primerEjercicio;
                pintarProfi();
            }

            @Override
            public void zonas(@Nullable String texto) {
                zonasHoy = texto;
                if (hoyTocaPintado) pintarHoyToca();
            }
        });
    }

    // Si «Empezar» ya llamó la atención en esta vista.
    private boolean empezarAnimado;

    // «Hoy toca · día 2 de 4» siguiendo un programa; «Hoy toca» a secas si no.
    private String eyebrowDelPrograma(HoyToca.Eleccion eleccion) {
        if (!eleccion.delPrograma || seguido == null) return getString(R.string.hoy_toca);
        int dias = 0;
        if (seguido.getCiclo() != null) {
            for (ProgramaQueSigue.DiaCiclo d : seguido.getCiclo()) if (d.isActiva()) dias++;
        }
        if (dias <= 0) return getString(R.string.hoy_toca);
        int hecha = Math.min(dias, TuPrograma.hechas(seguido) + 1);
        return getString(R.string.inicio_hoy_dia_programa, hecha, dias);
    }

    // El programa que el alta no pudo seguir (GP-103): Inicio lo ofrece con sus minutos.
    private void pintarProgramaPendiente(TextView titulo, TextView sub, MaterialButton principal,
                                         MaterialButton secundario) {
        String nombre = prefsManager.getProgramaPendienteNombre();
        titulo.setText(nombre);
        sub.setText(R.string.inicio_programa_pendiente_sub);
        principal.setIcon(null);
        principal.setText(R.string.inicio_programa_pendiente_seguir);
        principal.setContentDescription(getString(R.string.inicio_programa_pendiente_a11y, nombre));
        principal.setOnClickListener(v -> seguirPendiente(principal));
        secundario.setVisibility(View.GONE);
    }

    private void seguirPendiente(MaterialButton boton) {
        boton.setEnabled(false);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("minutos", prefsManager.getProgramaPendienteMinutos());
        programaApi.seguir(prefsManager.getProgramaPendienteCodigo(), body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignorado) {
                if (!isAdded()) return;
                boton.setEnabled(true);
                prefsManager.borrarProgramaPendiente();
                cargarHoyTocaYSemana();
            }

            @Override
            public void onFail(int code, String message) {
                if (!isAdded()) return;
                boton.setEnabled(true);
                UiFeedback.toastError(requireActivity(), code, message);
            }
        });
    }

    // «6 ejercicios · unos 55 min»; las zonas, si ya se saben, las suma quien la llama.
    private String subtituloHoyToca(Rutina r) {
        List<String> partes = new ArrayList<>();
        partes.add(getResources().getQuantityString(R.plurals.rutina_num_ejercicios,
                r.getNumEjercicios(), r.getNumEjercicios()));
        if (r.getDuracionMinutos() > 0) partes.add(getString(R.string.hoy_toca_unos_min, r.getDuracionMinutos()));
        return unir(partes);
    }

    // Los dos botones de Hoy toca, en fila (Empezar · Cambiar) o uno sobre otro.
    private void apilarBotones(boolean apilar) {
        LinearLayout fila = findViewById(R.id.filaBotonesHoy);
        fila.setOrientation(apilar ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        int margen = getResources().getDimensionPixelSize(R.dimen.espacio_8);
        LinearLayout.LayoutParams p1 = (LinearLayout.LayoutParams)
                findViewById(R.id.btnHoyPrincipal).getLayoutParams();
        p1.width = apilar ? ViewGroup.LayoutParams.MATCH_PARENT : 0;
        p1.weight = apilar ? 0 : 1;
        LinearLayout.LayoutParams p2 = (LinearLayout.LayoutParams)
                findViewById(R.id.btnHoySecundario).getLayoutParams();
        p2.width = apilar ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        p2.setMarginStart(apilar ? 0 : margen);
        p2.leftMargin = apilar ? 0 : margen;
        p2.topMargin = apilar ? margen : 0;
        fila.requestLayout();
    }

    private void pintarHoyTocaError() {
        hoyDetalle.ocultar();
        rutinaDelDetalle = -1;
        ((TextView) findViewById(R.id.tvHoyTitulo)).setText(R.string.hoy_toca);
        ((TextView) findViewById(R.id.tvHoySub)).setText(R.string.hoy_toca_error);
        findViewById(R.id.tvHoyUltima).setVisibility(View.GONE);
        MaterialButton principal = findViewById(R.id.btnHoyPrincipal);
        principal.setIcon(null);
        principal.setText(R.string.btn_reintentar);
        principal.setOnClickListener(v -> { cargarHoyTocaYSemana(); cargarVolumenSemana(); });
        findViewById(R.id.btnHoySecundario).setVisibility(View.GONE);
    }

    private void elegirRutina() {
        ElegirRutinaHoja.mostrar(requireActivity(), propias != null ? propias : new ArrayList<>(), this::empezar);
    }

    // La sesión en vivo con esa rutina, o vacía si es null (GP-012). Con otra en curso,
    // EmpezarSesion pregunta.
    private void empezar(@Nullable Rutina r) {
        EmpezarSesion.empezar(requireActivity(), r);
    }

    private void abrirResumen(SesionEntrenamiento s, View tarjeta) {
        Intent intent = new Intent(requireContext(), ResumenSesionActivity.class);
        intent.putExtra("sesionId", s.getId());
        es.pmdm.gymprofit.utils.Transiciones.abrirTarjeta(requireActivity(), tarjeta, intent);
    }

    // «2 sesiones · 1 h 50 min», desde el lunes a las 00:00.
    private void pintarSemanaSesiones() {
        int n = contarSemana();
        int minutos = 0;
        String lunes = lunesIso();
        if (sesiones != null) {
            for (SesionEntrenamiento s : sesiones) {
                if (s.getFechaInicio() != null && s.getFechaInicio().compareTo(lunes) >= 0) {
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

    // Sesiones desde el lunes (las que empiezan desde esa hora).
    private int contarSemana() {
        if (sesiones == null) return 0;
        String lunes = lunesIso();
        int n = 0;
        for (SesionEntrenamiento s : sesiones) {
            if (s.getFechaInicio() != null && s.getFechaInicio().compareTo(lunes) >= 0) n++;
        }
        return n;
    }

    // Las series por zona de la semana natural, y el cuerpo encendido.
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
                // Se dice en el bloque, con su «Reintentar», en vez de enseñar seis zonas a cero que
                // no lo son.
                findViewById(R.id.cardMusculos).setVisibility(View.GONE);
                estadoMusculos.error(VistaEstado.mensaje(requireContext(), R.string.semana_error, code, message),
                        InicioFragment.this::cargarVolumenSemana);
            }
        });
    }

    private void pintarZonas(@Nullable Map<String, Integer> porMusculo) {
        estadoMusculos.oculto();
        findViewById(R.id.cardMusculos).setVisibility(View.VISIBLE);
        SiluetaMuscularView silueta = findViewById(R.id.siluetaSemana);
        silueta.setVolumen(porMusculo);

        LinkedHashMap<Zonas.Zona, Integer> porZona = Zonas.seriesPorZona(porMusculo);
        int maximo = 1;
        for (int series : porZona.values()) maximo = Math.max(maximo, series);

        LinearLayout lista = findViewById(R.id.listaZonasSemana);
        lista.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        List<String> a11y = new ArrayList<>();
        for (Map.Entry<Zonas.Zona, Integer> e : porZona.entrySet()) {
            String nombre = getString(e.getKey().titulo);
            int series = e.getValue();
            View fila = inflater.inflate(R.layout.item_zona_semana, lista, false);
            ((TextView) fila.findViewById(R.id.tvZonaNombre)).setText(nombre);
            String texto = getResources().getQuantityString(R.plurals.inicio_zona_series, series, series);
            ((TextView) fila.findViewById(R.id.tvZonaSeries)).setText(texto);
            LinearProgressIndicator barra = fila.findViewById(R.id.barraZona);
            barra.setProgressCompat(series * 100 / maximo, false);
            // Cuanto más trabajada, más intensa; sin series, solo la pista.
            barra.setIndicatorColor(colorBarra(series, maximo));
            fila.setContentDescription(getString(R.string.hoy_toca_separador, nombre, texto));
            fila.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            lista.addView(fila);
            a11y.add(nombre + ": " + texto);
        }
        silueta.setContentDescription(getString(R.string.inicio_musculos_a11y, android.text.TextUtils.join(", ", a11y)));
        silueta.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    // El naranja entero para lo más trabajado y dos escalones mezclados con la pista (sin transparencia, que
    // dejaba ver el borde redondeado de la barra): el valor va escrito en la fila.
    private int colorBarra(int series, int maximo) {
        int primario = color(androidx.appcompat.R.attr.colorPrimary);
        float proporcion = series / (float) maximo;
        float mezcla = proporcion >= 0.66f ? 1f : proporcion >= 0.33f ? 0.7f : 0.4f;
        return androidx.core.graphics.ColorUtils.blendARGB(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.gp_meter_track), primario, mezcla);
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

    // Registrar comida → añadir alimento, hoy, en la comida que toca por la hora.
    private void registrarComida() {
        Intent i = new Intent(requireContext(), AnadirAlimentoActivity.class);
        i.putExtra("tipoComida", ComidaQueToca.ahora());
        i.putExtra("comidaId", -1);
        i.putExtra("fecha", TiempoRelativo.hoy());
        startActivity(i);
    }

    // ── El primer día (GP-103) ───────────────────────────────────────────────

    private boolean esPrimerDia() {
        String u = prefsManager.getUsername();
        return !u.isEmpty() && prefsManager.isPrimerDia(u);
    }

    private void prepararPrimerDia() {
        String u = prefsManager.getUsername();
        findViewById(R.id.btnOcultarPasos).setOnClickListener(v -> {
            prefsManager.ocultarPrimerosPasos(u);
            findViewById(R.id.cardPrimerosPasos).setVisibility(View.GONE);
        });
        findViewById(R.id.btnOcultarFundador).setOnClickListener(v -> {
            prefsManager.ocultarFundador(u);
            findViewById(R.id.cardFundador).setVisibility(View.GONE);
        });
        findViewById(R.id.btnEscribirFundador).setOnClickListener(v -> escribirAlFundador());
        pintarFundador();
    }

    // Un correo a soporte, que lee el fundador (el texto de la tarjeta lo promete).
    private void escribirAlFundador() {
        Intent i = new Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:"));
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.email_soporte)});
        try {
            startActivity(Intent.createChooser(i, getString(R.string.email_elegir_app)));
        } catch (android.content.ActivityNotFoundException e) {
            es.pmdm.gymprofit.utils.UIHelper.mostrarToastError(requireContext(), getString(R.string.email_sin_app));
        }
    }

    private void pintarFundador() {
        String u = prefsManager.getUsername();
        findViewById(R.id.cardFundador).setVisibility(
                esPrimerDia() && !prefsManager.isFundadorOculto(u) ? View.VISIBLE : View.GONE);
    }

    // «Primeros pasos»: tu plan (hecho al llegar aquí), tu primer entrenamiento y tu primera
    // comida. Se marcan solos y, con los tres, la tarjeta se va para siempre.
    private void pintarPrimerDia() {
        if (!isAdded()) return;
        pintarFundador();
        String u = prefsManager.getUsername();
        PrimerosPasos p = new PrimerosPasos(true, sesiones != null && !sesiones.isEmpty(),
                prefsManager.isPrimeraComida(u));
        View card = findViewById(R.id.cardPrimerosPasos);
        if (p.completos() && esPrimerDia()) prefsManager.ocultarPrimerosPasos(u);
        if (!p.visible(esPrimerDia(), prefsManager.isPrimerosPasosOcultos(u))) {
            card.setVisibility(View.GONE);
            return;
        }
        boolean nueva = card.getVisibility() != View.VISIBLE;
        card.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.tvPrimerosPasos)).setText(
                getString(R.string.inicio_pasos_titulo, p.hechos(), PrimerosPasos.TOTAL));
        LinearProgressIndicator barra = findViewById(R.id.barraPasos);
        barra.setProgressCompat(p.hechos() * 100 / PrimerosPasos.TOTAL, nueva && !Movimiento.quieto(requireContext()));

        HoyToca.Eleccion hoy = HoyToca.elegir(seguido, propias, sesiones);
        paso(R.id.pasoPlan, R.string.inicio_paso_plan, getString(R.string.inicio_paso_hecho), true, null);
        paso(R.id.pasoEntreno, R.string.inicio_paso_entreno, p.entreno ? getString(R.string.inicio_paso_hecho)
                        : hoy != null ? getString(R.string.inicio_paso_entreno_sub, hoy.rutina.getNombre())
                        : getString(R.string.inicio_paso_entreno_sub_libre),
                p.entreno, () -> irATab(NavTabs.ENTRENAR));
        paso(R.id.pasoComida, R.string.inicio_paso_comida, p.comida ? getString(R.string.inicio_paso_hecho)
                : getString(R.string.inicio_paso_comida_sub), p.comida, this::registrarComida);
    }

    private void paso(int id, int titulo, String sub, boolean hecho, @Nullable Runnable accion) {
        View fila = findViewById(id);
        TextView t = fila.findViewById(R.id.tvTituloPaso);
        t.setText(titulo);
        t.setTextColor(color(hecho ? com.google.android.material.R.attr.colorOnSurfaceVariant
                : com.google.android.material.R.attr.colorOnSurface));
        t.setTypeface(null, hecho ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
        ((TextView) fila.findViewById(R.id.tvSubPaso)).setText(sub);
        ImageView icono = fila.findViewById(R.id.ivPaso);
        icono.setImageResource(hecho ? R.drawable.ic_ms_check_circle_fill : R.drawable.ic_ms_radio_button_unchecked);
        icono.setImageTintList(ColorStateList.valueOf(color(hecho ? androidx.appcompat.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOutline)));
        fila.findViewById(R.id.ivFlechaPaso).setVisibility(hecho ? View.INVISIBLE : View.VISIBLE);
        fila.setClickable(!hecho && accion != null);
        fila.setOnClickListener(hecho || accion == null ? null : v -> accion.run());
        fila.setContentDescription(getString(hecho ? R.string.inicio_paso_hecho_a11y
                : R.string.inicio_paso_pendiente_a11y, getString(titulo), sub));
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private String unir(List<String> partes) {
        String s = partes.get(0);
        for (int i = 1; i < partes.size(); i++) s = getString(R.string.hoy_toca_separador, s, partes.get(i));
        return s;
    }

    private int color(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
