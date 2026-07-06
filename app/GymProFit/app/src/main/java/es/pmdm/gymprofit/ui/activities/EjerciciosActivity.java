package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.EjercicioApi;
import es.pmdm.gymprofit.ui.adapters.EjercicioAdapter;
import es.pmdm.gymprofit.utils.EjercicioNavHelper;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.UiFeedback;
// ============================================================
// EjerciciosActivity — pantalla del catálogo de ejercicios.
// Lista los ejercicios activos con buscador por texto y filtro por
// grupo muscular mediante chips, y navega al detalle de cada ejercicio.
// Incluye la barra de navegación inferior de la app.
// ============================================================
public class EjerciciosActivity extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView rvEjercicios;
    private EjercicioAdapter adapter;
    private TextInputEditText etBuscar;
    private ChipGroup chipGroupFiltros;
    private TextView tvEmpty;

    // Interfaz Retrofit tipada del dominio ejercicios (cacheada por ApiClient).
    private final EjercicioApi api = ApiClient.service(EjercicioApi.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ejercicios);

        setupMenuButton();
        inicializarVistas();
        configurarRecyclerView();
        configurarBuscador();
        configurarChips();
        configurarNavegacion();
        cargarEjercicios();
    }

    // Enlaza las referencias a las vistas del layout.
    private void inicializarVistas() {
        rvEjercicios = findViewById(R.id.rvEjercicios);
        etBuscar = findViewById(R.id.etBuscar);
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    // Configura el RecyclerView del catálogo con navegación al detalle al pulsar.
    private void configurarRecyclerView() {
        adapter = new EjercicioAdapter(new ArrayList<>(),
                e -> EjercicioNavHelper.abrir(this, e));
        rvEjercicios.setLayoutManager(new LinearLayoutManager(this));
        rvEjercicios.setAdapter(adapter);
    }

    // Obtiene de la API la lista de ejercicios activos y la carga en el adapter.
    private void cargarEjercicios() {
        // Spinner durante la carga (catálogo en blanco hasta responder).
        LoadingDialog.show(this);
        api.getActivos().enqueue(new ApiCallback<List<Ejercicio>>() {
            @Override
            public void onOk(List<Ejercicio> lista) {
                LoadingDialog.hide(EjerciciosActivity.this);
                adapter.setEjercicios(lista != null ? lista : new ArrayList<>());
                actualizarEstadoVacio();
            }

            @Override
            public void onFail(int code, String message) {
                // 404 = sin ejercicios (vacío benigno); -1/500 → toast.
                LoadingDialog.hide(EjerciciosActivity.this);
                UiFeedback.toastError(EjerciciosActivity.this, code, message);
                actualizarEstadoVacio();
            }
        });
    }

    // Muestra u oculta el mensaje "no hay nada aún" según los ítems visibles del adapter.
    private void actualizarEstadoVacio() {
        boolean vacio = adapter.getItemCount() == 0;
        tvEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        rvEjercicios.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    // Filtra la lista del adapter en tiempo real según el texto de búsqueda.
    private void configurarBuscador() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filtrarPorTexto(s.toString());
                actualizarEstadoVacio();
            }
        });
    }

    // Filtra la lista del adapter según el chip de grupo muscular seleccionado.
    private void configurarChips() {
        chipGroupFiltros.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int id = checkedIds.get(0);
            String grupo;

            if (id == R.id.chipTodos)        grupo = "Todos";
            else if (id == R.id.chipPecho)   grupo = "Pecho";
            else if (id == R.id.chipEspalda) grupo = "Espalda";
            else if (id == R.id.chipPiernas) grupo = "Piernas";
            else if (id == R.id.chipBrazos)  grupo = "Brazos";
            else if (id == R.id.chipHombros) grupo = "Hombros";
            else if (id == R.id.chipCore)    grupo = "ABOMEN";
            else                             grupo = "Todos";

            adapter.filtrarPorGrupo(grupo);
            actualizarEstadoVacio();
        });
    }

    // Configura la barra de navegación inferior para moverse entre las
    // secciones principales de la app sin animación de transición.
    private void configurarNavegacion() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_ejercicios);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                finish();
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                return true;
            } else if (itemId == R.id.nav_rutinas) {
                startActivity(new Intent(this, RutinasActivity.class));
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                finish();
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                return true;
            } else if (itemId == R.id.nav_ejercicios) {
                return true;
            } else if (itemId == R.id.nav_nutricion) {
                startActivity(new Intent(this, NutricionActivity.class));
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                finish();
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                return true;
            } else if (itemId == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                finish();
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                return true;
            }
            return false;
        });
    }

}
