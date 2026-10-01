package es.pmdm.gymprofit.ui.fragments;

import es.pmdm.gymprofit.envivo.SesionEnCursoRepositorio;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.PageDTO;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.EjercicioApi;
import es.pmdm.gymprofit.network.ProgramaApi;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.activities.CrearRutinaActivity;
import es.pmdm.gymprofit.ui.activities.EjerciciosActivity;
import es.pmdm.gymprofit.ui.activities.ProgramaDetalleActivity;
import es.pmdm.gymprofit.ui.activities.ProgramasActivity;
import es.pmdm.gymprofit.envivo.EmpezarSesion;
import es.pmdm.gymprofit.ui.widget.MenuRutina;
import es.pmdm.gymprofit.ui.widget.SeguirProgramaHoja;
import es.pmdm.gymprofit.ui.widget.TuProgramaVista;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.HoyToca;
import es.pmdm.gymprofit.utils.TuPrograma;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// EntrenarFragment — pestaña Entrenar (GP-105), según 02-entrenar.png.
//
// Junta las antiguas pestañas Rutinas y Ejercicios:
//   · Tu programa (GP-074, lote 1.2.1): sin programa, «Elige tu programa»; con él, su
//     ciclo, «Hoy toca» y el resto de sus rutinas, y el menú para verlo, cambiar el
//     tiempo, cambiar de programa o dejarlo. Lo pinta TuProgramaVista.
//   · Mis rutinas: solo las propias (las de un programa van arriba); tarjetas con
//     Empezar, tocar abre el detalle y la pulsación larga el menú de siempre. Sin
//     programa, la de Hoy toca lleva «Toca hoy» y el Empezar relleno.
//   · Empezar sin rutina: registrar sesión en entrenamiento libre.
//   · Biblioteca: el buscador y las zonas abren la pantalla de ejercicios, ya
//     filtrada o con el teclado abierto. El número de la ayuda sale de la API.
// El carrusel de plantillas se fue con la 1.2.0: desde entonces no hay predefinidas.
// ============================================================
public class EntrenarFragment extends BaseFragment {

    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final EjercicioApi ejercicioApi = ApiClient.service(EjercicioApi.class);
    private final ProgramaApi programaApi = ApiClient.service(ProgramaApi.class);

    private ActivityResultLauncher<Intent> recargar;
    private TuProgramaVista tuPrograma;

    // Lo cargado: las rutinas activas (propias y del programa), las sesiones y el
    // programa que sigue. «Mis rutinas» se pinta con las tres; «Tu programa», con él.
    @Nullable private List<Rutina> propias;
    @Nullable private List<SesionEntrenamiento> sesiones;
    @Nullable private ProgramaQueSigue seguido;
    private boolean seguidoListo;
    private boolean falloPropias;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recargar = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                r -> { if (r.getResultCode() == Activity.RESULT_OK) cargarMisRutinas(); });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrenar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findViewById(R.id.btnCrearRutina).setOnClickListener(v -> crearRutina());
        findViewById(R.id.btnCrearPrimera).setOnClickListener(v -> crearRutina());
        findViewById(R.id.btnSinRutina).setOnClickListener(v -> empezar(null));
        tuPrograma = new TuProgramaVista(findViewById(R.id.tuPrograma), accionesPrograma());
        findViewById(R.id.campoBuscar).setOnClickListener(v -> abrirBiblioteca(null, true));

        zona(R.id.btnZonaPecho, "PECHO");
        zona(R.id.btnZonaEspalda, "ESPALDA");
        zona(R.id.btnZonaPierna, "PIERNAS");
        zona(R.id.btnZonaHombros, "HOMBROS");
        zona(R.id.btnZonaBrazos, "BRAZOS");
        zona(R.id.btnZonaCore, "ABDOMEN");

        pintarAyudaBuscador(null);
        cargarTotalEjercicios();

        // La tarjeta de la rutina en curso dice «En curso» (GP-012): al empezar, guardar o
        // descartar se repinta con lo que ya hay, sin volver a pedirlo.
        SesionEnCursoRepositorio.get(requireContext()).getSesion().observe(getViewLifecycleOwner(), s -> {
            // Guardada: el programa avanza y cambia qué toca; se vuelve a pedir.
            boolean guardadaODescartada = habiaSesion && s == null;
            habiaSesion = s != null;
            if (guardadaODescartada) {
                cargarMisRutinas();
                return;
            }
            if (seguido != null) {
                tuPrograma.setRutinaEnCurso(rutinaEnCurso());
                tuPrograma.pintar(seguido);
            }
            listo();
        });
    }

    // Si en el último aviso había sesión en curso.
    private boolean habiaSesion;

    // La rutina de la sesión en curso, o null.
    @Nullable
    private Integer rutinaEnCurso() {
        SesionEnCurso s = SesionEnCursoRepositorio.get(requireContext()).actual();
        return s != null ? s.rutinaId : null;
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarMisRutinas();
    }

    // ── Mis rutinas ─────────────────────────────────────────────────────────

    private void cargarMisRutinas() {
        int uid = prefsManager.getUsuarioId();
        propias = null;
        sesiones = null;
        seguido = null;
        seguidoListo = false;
        falloPropias = false;
        if (uid == -1) {
            // Sin id de cuenta: ni rutinas ni programa propios; la tarjeta lleva a Programas.
            propias = new ArrayList<>();
            sesiones = new ArrayList<>();
            seguidoListo = true;
            tuPrograma.sinPrograma();
            pintarMisRutinas();
            return;
        }
        cargarPrograma();
        rutinaApi.getDeUsuarioActivas(uid).enqueue(new ApiCallback<List<Rutina>>() {
            @Override public void onOk(List<Rutina> l) { propias = l != null ? l : new ArrayList<>(); listo(); }
            @Override public void onFail(int code, String message) {
                if (!isAdded()) return;
                falloPropias = true;
                UiFeedback.toastError(requireActivity(), code, message);
                pintarErrorMisRutinas();
            }
        });
        sesionApi.getDeUsuario(uid).enqueue(new ApiCallback<List<SesionEntrenamiento>>() {
            @Override public void onOk(List<SesionEntrenamiento> l) { sesiones = l != null ? l : new ArrayList<>(); listo(); }
            @Override public void onFail(int code, String message) {
                // Las sesiones solo deciden cuál lleva «Toca hoy»: sin ellas se marcan
                // igual las rutinas, con la regla aplicada a «nunca hechas».
                sesiones = new ArrayList<>();
                listo();
            }
        });
    }

    // El programa que sigue: su tarjeta, y de él depende qué lleva «Toca hoy» abajo.
    private void cargarPrograma() {
        tuPrograma.cargando();
        programaApi.seguido().enqueue(new ApiCallback<ProgramaQueSigue>() {
            @Override public void onOk(ProgramaQueSigue s) {
                if (!isAdded()) return;
                seguido = s;
                seguidoListo = true;
                if (s == null) tuPrograma.sinPrograma();
                else {
                    tuPrograma.setRutinaEnCurso(rutinaEnCurso());
                    tuPrograma.pintar(s);
                }
                listo();
            }
            @Override public void onFail(int code, String message) {
                if (!isAdded()) return;
                // El fallo se enseña en la tarjeta, con reintentar; «Mis rutinas» se pinta
                // igual, sin marcar ninguna como la de hoy.
                seguidoListo = true;
                tuPrograma.error(VistaEstado.mensaje(requireContext(), R.string.tu_programa_error, code, message),
                        () -> cargarMisRutinas());
                listo();
            }
        });
    }

    private void listo() {
        if (!isAdded() || falloPropias || propias == null || sesiones == null || !seguidoListo) return;
        pintarMisRutinas();
    }

    private void pintarMisRutinas() {
        LinearLayout lista = findViewById(R.id.listaMisRutinas);
        lista.removeAllViews();
        List<Rutina> mias = TuPrograma.misRutinas(propias);
        boolean vacia = mias.isEmpty();
        findViewById(R.id.layoutRutinasVacio).setVisibility(vacia ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.tvRutinasVacio)).setText(R.string.mis_rutinas_vacio);
        findViewById(R.id.btnCrearPrimera).setVisibility(View.VISIBLE);
        if (vacia) return;

        // Siguiendo un programa, lo que toca está arriba: aquí no se marca ninguna.
        HoyToca.Eleccion hoy = HoyToca.elegir(seguido, propias, sesiones);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Rutina r : mias) {
            boolean toca = hoy != null && !hoy.delPrograma && hoy.rutina.getId() == r.getId();
            View card = inflater.inflate(R.layout.item_rutina_entrenar, lista, false);
            ((TextView) card.findViewById(R.id.tvNombre)).setText(r.getNombre());
            String resumen = getString(R.string.rutina_resumen,
                    getResources().getQuantityString(R.plurals.rutina_num_ejercicios, r.getNumEjercicios(), r.getNumEjercicios()),
                    getString(R.string.duracion_min, r.getDuracionMinutos()));
            ((TextView) card.findViewById(R.id.tvResumen)).setText(resumen);
            // La de la sesión en curso dice «En curso» y vuelve a ella (GP-012).
            Integer enCursoId = rutinaEnCurso();
            boolean enCurso = enCursoId != null && enCursoId == r.getId();
            TextView etiqueta = card.findViewById(R.id.tvTocaHoy);
            etiqueta.setText(enCurso ? R.string.envivo_en_curso : R.string.toca_hoy);
            etiqueta.setVisibility(toca || enCurso ? View.VISIBLE : View.GONE);

            com.google.android.material.button.MaterialButton lleno = card.findViewById(R.id.btnEmpezarLleno);
            com.google.android.material.button.MaterialButton contorno = card.findViewById(R.id.btnEmpezarContorno);
            boolean destacado = toca || enCurso;
            lleno.setVisibility(destacado ? View.VISIBLE : View.GONE);
            contorno.setVisibility(destacado ? View.GONE : View.VISIBLE);
            com.google.android.material.button.MaterialButton boton = destacado ? lleno : contorno;
            boton.setText(enCurso ? R.string.envivo_volver : R.string.btn_empezar);
            boton.setContentDescription(enCurso ? getString(R.string.envivo_volver_rutina_a11y, r.getNombre())
                    : getString(R.string.empezar_rutina_a11y, r.getNombre()));
            boton.setOnClickListener(v -> empezar(r));

            card.setContentDescription(r.getNombre() + ". " + resumen
                    + (enCurso ? ". " + getString(R.string.envivo_en_curso) : toca ? ". " + getString(R.string.toca_hoy) : ""));
            card.setOnClickListener(v -> MenuRutina.abrirDetalle(requireActivity(), recargar, r));
            card.setOnLongClickListener(v -> {
                MenuRutina.mostrar(requireActivity(), v, r, recargar, this::cargarMisRutinas);
                return true;
            });
            lista.addView(card);
        }
    }

    private void pintarErrorMisRutinas() {
        ((LinearLayout) findViewById(R.id.listaMisRutinas)).removeAllViews();
        findViewById(R.id.layoutRutinasVacio).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.tvRutinasVacio)).setText(R.string.mis_rutinas_error);
        findViewById(R.id.btnCrearPrimera).setVisibility(View.GONE);
    }

    private void crearRutina() {
        recargar.launch(new Intent(requireContext(), CrearRutinaActivity.class));
    }

    // La sesión en vivo con esa rutina, o vacía si es null (GP-012). Con otra en curso,
    // EmpezarSesion pregunta.
    private void empezar(@Nullable Rutina r) {
        EmpezarSesion.empezar(requireActivity(), r);
    }

    // ── Tu programa ─────────────────────────────────────────────────────────

    private TuProgramaVista.Acciones accionesPrograma() {
        return new TuProgramaVista.Acciones() {
            @Override public void elegirPrograma() {
                startActivity(new Intent(requireContext(), ProgramasActivity.class));
            }
            @Override public void empezar(Rutina rutina) { EntrenarFragment.this.empezar(rutina); }
            @Override public void abrirRutina(Rutina rutina) {
                MenuRutina.abrirDetalle(requireActivity(), recargar, rutina);
            }
            @Override public void menuRutina(View ancla, Rutina rutina) {
                MenuRutina.mostrar(requireActivity(), ancla, rutina, recargar, EntrenarFragment.this::cargarMisRutinas);
            }
            @Override public void menuPrograma(View ancla, ProgramaQueSigue s) { mostrarMenuPrograma(ancla, s); }
            @Override public void volverASeguir(ProgramaQueSigue s) { cambiarTiempo(s); }
            @Override public void dejar() { confirmarDejar(); }
        };
    }

    // Ver el programa, cambiar el tiempo, cambiar de programa y dejarlo.
    private void mostrarMenuPrograma(View ancla, ProgramaQueSigue s) {
        List<UIHelper.MenuAction> acciones = new ArrayList<>();
        acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_visibility, getString(R.string.tu_programa_ver), () ->
                startActivity(new Intent(requireContext(), ProgramaDetalleActivity.class)
                        .putExtra(ProgramaDetalleActivity.EXTRA_CODIGO, s.getPrograma().getCodigo()))));
        acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_schedule, getString(R.string.tu_programa_cambiar_tiempo),
                () -> cambiarTiempo(s)));
        acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_calendar_month, getString(R.string.tu_programa_cambiar),
                () -> startActivity(new Intent(requireContext(), ProgramasActivity.class))));
        acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_close, getString(R.string.tu_programa_dejar), true,
                this::confirmarDejar));
        UIHelper.mostrarMenuAnclado(requireActivity(), ancla, s.getPrograma().getNombre(), acciones);
    }

    private void cambiarTiempo(ProgramaQueSigue s) {
        SeguirProgramaHoja.mostrar(requireActivity(), s.getPrograma().getCodigo(), s.getPrograma().getNombre(), s,
                this::cargarMisRutinas);
    }

    private void confirmarDejar() {
        UIHelper.mostrarDialogoConIcono(requireActivity(), getString(R.string.tu_programa_dejar),
                getString(R.string.tu_programa_dejar_confirmar), R.drawable.ic_ms_close, () ->
                        programaApi.dejar().enqueue(new ApiCallback<Void>() {
                            @Override public void onOk(Void body) {
                                if (!isAdded()) return;
                                UIHelper.mostrarToastExito(requireActivity(), getString(R.string.tu_programa_dejado));
                                cargarMisRutinas();
                            }
                            @Override public void onFail(int code, String message) {
                                if (isAdded()) UiFeedback.toastError(requireActivity(), code, message);
                            }
                        }));
    }

    // ── Biblioteca ──────────────────────────────────────────────────────────

    private void zona(int id, String grupo) {
        TextView b = findViewById(id);
        b.setContentDescription(getString(R.string.biblioteca_zona_a11y, b.getText()));
        b.setOnClickListener(v -> abrirBiblioteca(grupo, false));
    }

    private void abrirBiblioteca(@Nullable String grupo, boolean buscar) {
        Intent i = new Intent(requireContext(), EjerciciosActivity.class);
        if (grupo != null) i.putExtra(EjerciciosActivity.EXTRA_GRUPO, grupo);
        i.putExtra(EjerciciosActivity.EXTRA_BUSCAR, buscar);
        startActivity(i);
    }

    // El total de ejercicios lo da la API (totalElements de una página de uno); si no
    // llega, la ayuda dice «Busca un ejercicio». Nunca un número escrito a mano.
    private void cargarTotalEjercicios() {
        ejercicioApi.buscar(null, null, null, 0, 1).enqueue(new ApiCallback<PageDTO<Ejercicio>>() {
            @Override public void onOk(PageDTO<Ejercicio> p) {
                if (isAdded()) pintarAyudaBuscador(p != null && p.getTotalElements() > 0 ? p.getTotalElements() : null);
            }
            @Override public void onFail(int code, String message) {
                // Se ignora a propósito (GP-017): sin el total, la ayuda del buscador dice
                // «Busca un ejercicio», que es cierto; el buscador funciona igual.
            }
        });
    }

    private void pintarAyudaBuscador(@Nullable Long total) {
        TextView hint = findViewById(R.id.tvBuscarHint);
        hint.setText(total == null ? getString(R.string.biblioteca_buscar)
                : getString(R.string.biblioteca_buscar_n,
                NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(requireContext())).format(total)));
        findViewById(R.id.campoBuscar).setContentDescription(hint.getText());
    }
}
