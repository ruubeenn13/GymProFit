package es.pmdm.gymprofit.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import es.pmdm.gymprofit.ui.adapters.NuevaRutinaHeaderAdapter;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.ui.activities.CrearRutinaActivity;
import es.pmdm.gymprofit.ui.activities.DetalleRutinaActivity;
import es.pmdm.gymprofit.ui.activities.EditarRutinaActivity;
import es.pmdm.gymprofit.ui.activities.EditarRutinaAdminActivity;
import es.pmdm.gymprofit.ui.adapters.RutinaAdapter;
import es.pmdm.gymprofit.ui.adapters.RutinasVacioHeaderAdapter;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// RutinasFragment — pestaña "Mis rutinas": las rutinas PROPIAS del usuario, con
// filtros por nivel y CRUD. Muestra un menú contextual por rutina (editar,
// activar/desactivar o eliminar) y navega al detalle de cada una.
//
// Las rutinas PREDEFINIDAS ya no se mezclan con las propias: solo aparecen cuando
// el usuario no tiene ninguna, dentro del estado vacío y bajo su propio rótulo
// ("Rutinas para empezar"), como sugerencia de por dónde arrancar. Antes se
// concatenaban siempre, así que una cuenta recién creada veía seis rutinas ajenas
// presentadas como suyas y el estado vacío del layout era inalcanzable.
// ============================================================
public class RutinasFragment extends BaseFragment {
    private RecyclerView rvRutinas;
    private RutinaAdapter adapter;
    private RutinasVacioHeaderAdapter cabeceraVacio;
    private ChipGroup chipGroupNivel;
    private TextView tvEmpty;

    // Nivel del chip activo. Solo sirve para distinguir "esta cuenta no tiene nada"
    // de "el filtro no deja pasar nada", que son dos mensajes distintos.
    private String nivelActual = NIVEL_TODOS;

    private static final String NIVEL_TODOS = "Todos";

    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);

    private ActivityResultLauncher<Intent> crearRutinaLauncher;
    private ActivityResultLauncher<Intent> detalleLauncher;
    private ActivityResultLauncher<Intent> editarLauncher;

    // Registra los launchers antes de que el fragment llegue a STARTED.
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        crearRutinaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { if (result.getResultCode() == Activity.RESULT_OK) cargarRutinas(); });
        detalleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { if (result.getResultCode() == Activity.RESULT_OK) cargarRutinas(); });
        editarLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { if (result.getResultCode() == Activity.RESULT_OK) cargarRutinas(); });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_rutinas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMenuButton();
        inicializarVistas();
        configurarRecyclerView();
        configurarChips();
        cargarRutinas();
    }

    private void inicializarVistas() {
        rvRutinas = findViewById(R.id.rvRutinas);
        chipGroupNivel = findViewById(R.id.chipGroupNivel);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    private void configurarRecyclerView() {
        adapter = new RutinaAdapter(new ArrayList<>(), this::abrirDetalle);
        adapter.setOnLongClickListener(this::mostrarMenuContextual);
        adapter.setUserContext(prefsManager.isAdmin(), prefsManager.getUsuarioId());
        rvRutinas.setLayoutManager(new LinearLayoutManager(requireContext()));
        // La tarjeta "+ Nueva rutina" va como primer elemento (header) del listado,
        // concatenada antes del adapter de rutinas. Sustituye al antiguo FAB (que ahora
        // quedaría oculto tras la barra de navegación flotante).
        NuevaRutinaHeaderAdapter header = new NuevaRutinaHeaderAdapter(this::crearRutina);
        // Entre la tarjeta de crear y el listado va el bloque de estado vacío, que
        // está oculto (0 ítems) mientras el usuario tenga rutinas propias.
        cabeceraVacio = new RutinasVacioHeaderAdapter();
        rvRutinas.setAdapter(new ConcatAdapter(header, cabeceraVacio, adapter));
    }

    // Abre la creación de una nueva rutina (requiere usuario registrado). Lo dispara la
    // tarjeta "+ Nueva rutina" que encabeza la lista.
    private void crearRutina() {
        if (!verificarAccesoRegistrado()) return;
        crearRutinaLauncher.launch(new Intent(requireContext(), CrearRutinaActivity.class));
    }

    // Abre la pantalla de detalle de una rutina pasando sus datos por extras.
    private void abrirDetalle(Rutina rutina) {
        Intent intent = new Intent(requireContext(), DetalleRutinaActivity.class);
        intent.putExtra("rutinaId",     rutina.getId());
        intent.putExtra("nombre",       rutina.getNombre());
        intent.putExtra("descripcion",  rutina.getDescripcion());
        intent.putExtra("nivel",        rutina.getNivel());
        intent.putExtra("duracion",     rutina.getDuracionMinutos());
        intent.putExtra("numEjercicios", rutina.getNumEjercicios());
        intent.putExtra("predefinida",  rutina.isPredefinida());
        intent.putExtra("usuarioId",    rutina.getUsuarioId());
        detalleLauncher.launch(intent);
    }

    // Carga la lista. Primero las PROPIAS: si hay alguna, la pantalla enseña solo
    // esas, que es lo único que "Mis rutinas" puede prometer. Si no hay ninguna se
    // piden las predefinidas y se muestran dentro del estado vacío, atribuidas a
    // GymProFit y no al usuario.
    private void cargarRutinas() {
        final FragmentActivity act = requireActivity();
        int usuarioId = prefsManager.getUsuarioId();

        LoadingDialog.show(act);

        // Invitado: no hay rutinas propias que pedir, pero sí tiene que poder ver
        // por dónde se empieza.
        if (usuarioId == -1) {
            cargarPredefinidasComoSugerencia(act);
            return;
        }

        rutinaApi.getDeUsuarioActivas(usuarioId).enqueue(new ApiCallback<List<Rutina>>() {
            @Override
            public void onOk(List<Rutina> propias) {
                if (propias != null && !propias.isEmpty()) {
                    LoadingDialog.hide(act);
                    if (!isAdded()) return;
                    mostrarRutinas(propias, false);
                } else {
                    cargarPredefinidasComoSugerencia(act);
                }
            }
            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(act);
                if (!isAdded()) return;
                // Desde GP-069 una lista vacía llega como 200 con [], así que aquí
                // solo caen fallos de verdad. Y con un fallo no se sabe si el usuario
                // tiene rutinas propias: sin saberlo no se puede atribuir nada, se
                // avisa y se deja la lista vacía antes que enseñar predefinidas que
                // podrían estar tapando las suyas.
                UiFeedback.toastError(act, code, message);
                mostrarRutinas(new ArrayList<>(), false);
            }
        });
    }

    // Pide las rutinas predefinidas para ofrecerlas como sugerencia dentro del
    // estado vacío. Solo se llama cuando consta que el usuario no tiene ninguna.
    private void cargarPredefinidasComoSugerencia(final FragmentActivity act) {
        rutinaApi.getPredefinidas().enqueue(new ApiCallback<List<Rutina>>() {
            @Override
            public void onOk(List<Rutina> predefinidas) {
                LoadingDialog.hide(act);
                if (!isAdded()) return;
                List<Rutina> sugerencias = predefinidas != null ? predefinidas : new ArrayList<>();
                // El bloque de estado vacío solo se enseña si hay algo que sugerir: en
                // producción el catálogo de predefinidas puede estar vacío, y desde
                // GP-069 eso llega como 200 con [] en vez de como 404. Sin esta
                // comprobación quedaría el rótulo «Rutinas para empezar» sin ninguna
                // rutina debajo, que es exactamente el tipo de promesa vacía que
                // GP-060 vino a quitar.
                mostrarRutinas(sugerencias, !sugerencias.isEmpty());
            }
            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(act);
                if (!isAdded()) return;
                // Sin sugerencias que ofrecer, el bloque de estado vacío mentiría:
                // queda solo la tarjeta de crear rutina. Que no haya predefinidas
                // publicadas ya no llega como fallo, llega como 200 con [].
                UiFeedback.toastError(act, code, message);
                mostrarRutinas(new ArrayList<>(), false);
            }
        });
    }

    // Pinta la lista y decide si el bloque de estado vacío acompaña a las tarjetas.
    private void mostrarRutinas(List<Rutina> rutinas, boolean comoSugerencia) {
        cabeceraVacio.setVisible(comoSugerencia);
        adapter.setRutinas(rutinas);
        nivelActual = NIVEL_TODOS;
        actualizarEstadoVacio();
    }

    // El mensaje de lista vacía solo aparece cuando no queda ni una tarjeta: con el
    // estado vacío y sus sugerencias en pantalla siempre hay algo, así que en la
    // práctica lo que se ve aquí es el filtro de nivel sin resultados.
    private void actualizarEstadoVacio() {
        boolean sinTarjetas = adapter.getItemCount() == 0;
        tvEmpty.setText(NIVEL_TODOS.equals(nivelActual)
                ? R.string.feedback_lista_vacia
                : R.string.rutinas_vacio_filtro);
        tvEmpty.setVisibility(sinTarjetas ? View.VISIBLE : View.GONE);
        rvRutinas.setVisibility(View.VISIBLE);
    }

    // Menú contextual por rutina (editar, activar/desactivar o eliminar).
    private void mostrarMenuContextual(Rutina rutina, View anchorView) {
        if (!verificarAccesoRegistrado()) return;
        final FragmentActivity act = requireActivity();

        List<UIHelper.MenuAction> actions = new ArrayList<>();

        actions.add(new UIHelper.MenuAction(R.drawable.ic_edit, getString(R.string.rutinas_editar), () -> {
            if (rutina.isPredefinida()) {
                Intent intent = new Intent(requireContext(), EditarRutinaAdminActivity.class);
                intent.putExtra("id",                  rutina.getId());
                intent.putExtra("nombre",              rutina.getNombre());
                intent.putExtra("descripcion",         rutina.getDescripcion());
                intent.putExtra("nivel",               rutina.getNivel());
                intent.putExtra("duracionMinutos",     rutina.getDuracionMinutos());
                intent.putExtra("categoria",           rutina.getCategoria());
                intent.putExtra("diasSemana",          rutina.getDiasSemana());
                editarLauncher.launch(intent);
            } else {
                Intent intent = new Intent(requireContext(), EditarRutinaActivity.class);
                intent.putExtra("rutinaId",    rutina.getId());
                intent.putExtra("nombre",      rutina.getNombre());
                intent.putExtra("descripcion", rutina.getDescripcion());
                intent.putExtra("nivel",       rutina.getNivel());
                intent.putExtra("duracion",    rutina.getDuracionMinutos());
                editarLauncher.launch(intent);
            }
        }));

        if (rutina.isPredefinida()) {
            int iconToggle = rutina.isActiva() ? R.drawable.ic_visibility_off : R.drawable.ic_check;
            String labelToggle = rutina.isActiva()
                    ? getString(R.string.rutinas_desactivar)
                    : getString(R.string.rutinas_activar);
            actions.add(new UIHelper.MenuAction(iconToggle, labelToggle,
                    () -> toggleActivaRutinaPredefinida(rutina)));
        } else {
            actions.add(new UIHelper.MenuAction(R.drawable.ic_delete, getString(R.string.rutinas_eliminar), true,
                    () -> UIHelper.mostrarDialogoConIcono(act,
                            getString(R.string.rutinas_eliminar),
                            getString(R.string.rutinas_confirmar_eliminar),
                            R.drawable.ic_delete,
                            () -> eliminarRutina(rutina))));
        }

        UIHelper.mostrarMenuAnclado(act, anchorView, rutina.getNombre(), actions);
    }

    // Activa o desactiva una rutina predefinida (solo admin) y recarga el listado.
    private void toggleActivaRutinaPredefinida(Rutina rutina) {
        final FragmentActivity act = requireActivity();
        ApiCallback<Void> cb = new ApiCallback<Void>() {
            @Override
            public void onOk(Void body) {
                UIHelper.mostrarToastExito(act, getString(R.string.admin_exito_toggle_rutina));
                cargarRutinas();
            }
            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(act, getString(R.string.error_conexion));
            }
        };
        if (rutina.isActiva()) {
            rutinaApi.eliminar(rutina.getId()).enqueue(cb);
        } else {
            rutinaApi.activar(rutina.getId()).enqueue(cb);
        }
    }

    // Elimina una rutina propia del usuario y recarga el listado.
    private void eliminarRutina(Rutina rutina) {
        final FragmentActivity act = requireActivity();
        rutinaApi.eliminar(rutina.getId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void body) {
                UIHelper.mostrarToastExito(act, getString(R.string.rutinas_eliminada_exito));
                cargarRutinas();
            }
            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(act, getString(R.string.error_conexion));
            }
        });
    }

    // Filtra el listado de rutinas por nivel según el chip seleccionado.
    private void configurarChips() {
        chipGroupNivel.setOnCheckedStateChangeListener(((chipGroup, list) -> {
            if (list.isEmpty()) return;

            int id = list.get(0);
            String nivel;

            if (id == R.id.chipTodos)              nivel = NIVEL_TODOS;
            else if (id == R.id.chipPrincipiante)  nivel = "Principiante";
            else if (id == R.id.chipIntermedio)    nivel = "Intermedio";
            else if (id == R.id.chipAvanzado)      nivel = "Avanzado";
            else                                   nivel = NIVEL_TODOS;

            nivelActual = nivel;
            adapter.filtrarPorNivel(nivel);
            actualizarEstadoVacio();
        }));
    }
}
