package es.pmdm.gymprofit.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.EjercicioApi;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.activities.CrearRutinaActivity;
import es.pmdm.gymprofit.ui.activities.EjerciciosActivity;
import es.pmdm.gymprofit.ui.activities.PlantillasActivity;
import es.pmdm.gymprofit.ui.activities.RegistrarSesionActivity;
import es.pmdm.gymprofit.ui.widget.MenuRutina;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.HoyToca;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// EntrenarFragment — pestaña Entrenar (GP-105), según 02-entrenar.png.
//
// Junta las antiguas pestañas Rutinas y Ejercicios:
//   · Mis rutinas: tarjetas con Empezar; tocar abre el detalle y la pulsación larga
//     el menú de siempre. La de Hoy toca lleva «Toca hoy» y el Empezar relleno.
//   · Empezar sin rutina: registrar sesión en entrenamiento libre.
//   · Plantillas de GymProFit: carrusel y «Ver todas» (PlantillasActivity, con el
//     filtro por nivel de GP-072). Sin plantillas, la sección no sale.
//   · Biblioteca: el buscador y las zonas abren la pantalla de ejercicios, ya
//     filtrada o con el teclado abierto. El número de la ayuda sale de la API.
// ============================================================
public class EntrenarFragment extends BaseFragment {

    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final EjercicioApi ejercicioApi = ApiClient.service(EjercicioApi.class);

    private ActivityResultLauncher<Intent> recargar;

    @Nullable private List<Rutina> propias;
    @Nullable private List<SesionEntrenamiento> sesiones;
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
        findViewById(R.id.btnVerTodas).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PlantillasActivity.class)));
        findViewById(R.id.campoBuscar).setOnClickListener(v -> abrirBiblioteca(null, true));

        zona(R.id.btnZonaPecho, "PECHO");
        zona(R.id.btnZonaEspalda, "ESPALDA");
        zona(R.id.btnZonaPierna, "PIERNAS");
        zona(R.id.btnZonaHombros, "HOMBROS");
        zona(R.id.btnZonaBrazos, "BRAZOS");
        zona(R.id.btnZonaCore, "ABDOMEN");

        pintarAyudaBuscador(null);
        cargarTotalEjercicios();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarMisRutinas();
        cargarPlantillas();
    }

    // ── Mis rutinas ─────────────────────────────────────────────────────────

    private void cargarMisRutinas() {
        int uid = prefsManager.getUsuarioId();
        propias = null;
        sesiones = null;
        falloPropias = false;
        if (uid == -1) {
            propias = new ArrayList<>();
            sesiones = new ArrayList<>();
            pintarMisRutinas();
            return;
        }
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

    private void listo() {
        if (!isAdded() || falloPropias || propias == null || sesiones == null) return;
        pintarMisRutinas();
    }

    private void pintarMisRutinas() {
        LinearLayout lista = findViewById(R.id.listaMisRutinas);
        lista.removeAllViews();
        boolean vacia = propias == null || propias.isEmpty();
        findViewById(R.id.layoutRutinasVacio).setVisibility(vacia ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.tvRutinasVacio)).setText(R.string.mis_rutinas_vacio);
        findViewById(R.id.btnCrearPrimera).setVisibility(View.VISIBLE);
        if (vacia) return;

        HoyToca.Eleccion hoy = HoyToca.elegir(propias, sesiones);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Rutina r : propias) {
            boolean toca = hoy != null && hoy.rutina.getId() == r.getId();
            View card = inflater.inflate(R.layout.item_rutina_entrenar, lista, false);
            ((TextView) card.findViewById(R.id.tvNombre)).setText(r.getNombre());
            String resumen = getString(R.string.rutina_resumen,
                    getResources().getQuantityString(R.plurals.rutina_num_ejercicios, r.getNumEjercicios(), r.getNumEjercicios()),
                    getString(R.string.duracion_min, r.getDuracionMinutos()));
            ((TextView) card.findViewById(R.id.tvResumen)).setText(resumen);
            card.findViewById(R.id.tvTocaHoy).setVisibility(toca ? View.VISIBLE : View.GONE);

            View lleno = card.findViewById(R.id.btnEmpezarLleno);
            View contorno = card.findViewById(R.id.btnEmpezarContorno);
            lleno.setVisibility(toca ? View.VISIBLE : View.GONE);
            contorno.setVisibility(toca ? View.GONE : View.VISIBLE);
            View boton = toca ? lleno : contorno;
            boton.setContentDescription(getString(R.string.empezar_rutina_a11y, r.getNombre()));
            boton.setOnClickListener(v -> empezar(r));

            card.setContentDescription(r.getNombre() + ". " + resumen + (toca ? ". " + getString(R.string.toca_hoy) : ""));
            card.setOnClickListener(v -> MenuRutina.abrirDetalle(requireActivity(), recargar, r));
            card.setOnLongClickListener(v -> {
                if (!verificarAccesoRegistrado()) return true;
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
        if (!verificarAccesoRegistrado()) return;
        recargar.launch(new Intent(requireContext(), CrearRutinaActivity.class));
    }

    // Registrar sesión con esa rutina, o en entrenamiento libre si es null.
    private void empezar(@Nullable Rutina r) {
        if (!verificarAccesoRegistrado()) return;
        Intent i = new Intent(requireContext(), RegistrarSesionActivity.class);
        if (r != null) i.putExtra(RegistrarSesionActivity.EXTRA_RUTINA_ID, r.getId());
        startActivity(i);
    }

    // ── Plantillas ──────────────────────────────────────────────────────────

    private void cargarPlantillas() {
        rutinaApi.getPredefinidas().enqueue(new ApiCallback<List<Rutina>>() {
            @Override public void onOk(List<Rutina> l) { if (isAdded()) pintarPlantillas(l); }
            @Override public void onFail(int code, String message) {
                // Sin plantillas la sección no sale (producción no tiene, GP-074). Un fallo
                // se trata igual: el resto de la pestaña se usa sin ellas, y el aviso de
                // red ya lo da Mis rutinas, que se pide a la vez.
                if (isAdded()) pintarPlantillas(null);
            }
        });
    }

    private void pintarPlantillas(@Nullable List<Rutina> plantillas) {
        boolean hay = plantillas != null && !plantillas.isEmpty();
        findViewById(R.id.cabeceraPlantillas).setVisibility(hay ? View.VISIBLE : View.GONE);
        findViewById(R.id.scrollPlantillas).setVisibility(hay ? View.VISIBLE : View.GONE);
        LinearLayout lista = findViewById(R.id.listaPlantillas);
        lista.removeAllViews();
        if (!hay) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Rutina r : plantillas) {
            View card = inflater.inflate(R.layout.item_plantilla_carrusel, lista, false);
            ((ImageView) card.findViewById(R.id.ivIcono)).setImageResource(iconoPlantilla(r));
            ((TextView) card.findViewById(R.id.tvNombre)).setText(r.getNombre());
            String resumen = getString(R.string.plantilla_resumen,
                    getResources().getQuantityString(R.plurals.rutina_num_ejercicios, r.getNumEjercicios(), r.getNumEjercicios()),
                    getString(R.string.duracion_min, r.getDuracionMinutos()),
                    UIHelper.traducirNivel(requireContext(), r.getNivel()));
            ((TextView) card.findViewById(R.id.tvResumen)).setText(resumen);
            card.setContentDescription(r.getNombre() + ". " + resumen);
            card.setOnClickListener(v -> MenuRutina.abrirDetalle(requireActivity(), recargar, r));
            lista.addView(card);
        }
    }

    // Icono por el tipo de plantilla: cuerpo completo, cardio o por partes.
    private static int iconoPlantilla(Rutina r) {
        String c = r.getCategoria() == null ? "" : r.getCategoria().toUpperCase(java.util.Locale.ROOT);
        if (c.contains("GENERAL")) return R.drawable.ic_ms_accessibility_new;
        if (c.contains("CARDIO")) return R.drawable.ic_ms_directions_run;
        return R.drawable.ic_ms_splitscreen;
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
