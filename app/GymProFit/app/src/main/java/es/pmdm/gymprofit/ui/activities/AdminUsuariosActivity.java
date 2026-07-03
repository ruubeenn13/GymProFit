package es.pmdm.gymprofit.ui.activities;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.AdminApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.ui.adapters.AdminUsuarioAdapter;

// ============================================================
// AdminUsuariosActivity — pantalla de administración de usuarios
// Permite al rol ADMIN listar, filtrar por estado/rol/username, activar o
// desactivar cuentas y cambiar el rol (USER/ADMIN) de cualquier usuario.
// ============================================================
public class AdminUsuariosActivity extends BaseActivity {

    private RecyclerView rv;
    private AdminUsuarioAdapter adapter;
    // Lista de usuarios actualmente mostrada en el RecyclerView
    private final List<Usuario> lista = new ArrayList<>();

    // Interfaz Retrofit tipada del panel de administración (etapa 2)
    private final AdminApi api = ApiClient.service(AdminApi.class);

    // Filtro por estado activo/inactivo (null = sin filtrar)
    private Boolean filtroActivo = null;
    // Filtro por rol (ROLE_USER / ROLE_ADMIN, null = sin filtrar)
    private String filtroRol = null;
    // Filtro por username introducido en el buscador (null = sin filtrar)
    private String filtroUsername = null;

    // Configura RecyclerView, chips de filtro, buscador y carga inicial de datos
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_usuarios);

        setupMenuButton();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rvUsuarios);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUsuarioAdapter(lista, new AdminUsuarioAdapter.OnAccionListener() {
            @Override
            public void onToggleActivo(Usuario u, int pos) {
                mostrarDialogoToggle(u, pos);
            }
            @Override
            public void onCambiarRol(Usuario u, int pos) {
                mostrarDialogoCambiarRol(u, pos);
            }
        });
        rv.setAdapter(adapter);

        configurarChips();
        configurarBusqueda();
        cargar();
    }

    // Configura los chips de filtro por estado (activo/inactivo) y por rol; excluyentes entre sí
    private void configurarChips() {
        ChipGroup cg = findViewById(R.id.chipGroupEstado);
        cg.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipEstadoTodos) {
                filtroActivo = null;
                filtroRol = null;
            } else if (id == R.id.chipEstadoActivos) {
                filtroActivo = true;
            } else if (id == R.id.chipEstadoInactivos) {
                filtroActivo = false;
            } else if (id == R.id.chipRolUser) {
                filtroRol = "ROLE_USER";
                filtroActivo = null;
            } else if (id == R.id.chipRolAdmin) {
                filtroRol = "ROLE_ADMIN";
                filtroActivo = null;
            }
            cargar();
        });
    }

    // Configura el buscador por username; filtra en cada cambio de texto
    private void configurarBusqueda() {
        SearchView sv = findViewById(R.id.searchView);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String q) {
                filtroUsername = q.trim().isEmpty() ? null : q.trim();
                cargar();
                return true;
            }
            @Override
            public boolean onQueryTextChange(String q) {
                filtroUsername = q.trim().isEmpty() ? null : q.trim();
                cargar();
                return true;
            }
        });
    }

    // Llama al endpoint admin de usuarios filtrados (primera página, hasta 100 resultados)
    private void cargar() {
        api.getUsuarios(filtroActivo, filtroRol, filtroUsername, 0, 100)
                .enqueue(new ApiCallback<List<Usuario>>() {
                    @Override
                    public void onOk(List<Usuario> nuevos) {
                        lista.clear();
                        if (nuevos != null) lista.addAll(nuevos);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    // Muestra un diálogo de confirmación para activar/desactivar la cuenta del usuario
    private void mostrarDialogoToggle(Usuario u, int pos) {
        String msg = u.isActivo()
                ? getString(R.string.admin_desactivar) + " " + u.getUsername() + "?"
                : getString(R.string.admin_activar) + " " + u.getUsername() + "?";
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.admin_toggle_activo_titulo))
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, (d, w) ->
                        api.toggleActivoUsuario(u.getId()).enqueue(new ApiCallback<Void>() {
                            @Override
                            public void onOk(Void body) {
                                u.setActivo(!u.isActivo());
                                adapter.actualizarItem(pos, u);
                                Toast.makeText(AdminUsuariosActivity.this,
                                        getString(R.string.admin_exito_toggle_usuario), Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onFail(int code, String message) {
                                Toast.makeText(AdminUsuariosActivity.this,
                                        getString(R.string.admin_error_generico), Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // Muestra un diálogo con RadioGroup (USER/ADMIN) para cambiar el rol del usuario seleccionado
    private void mostrarDialogoCambiarRol(Usuario u, int pos) {
        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);
        rg.setPadding(48, 24, 48, 0);

        RadioButton rbUser = new RadioButton(this);
        rbUser.setText("USER");
        rbUser.setId(1);

        RadioButton rbAdmin = new RadioButton(this);
        rbAdmin.setText("ADMIN");
        rbAdmin.setId(2);

        rg.addView(rbUser);
        rg.addView(rbAdmin);

        String rolActual = u.getRol() != null ? u.getRol() : "";
        if (rolActual.contains("ADMIN")) rg.check(2);
        else rg.check(1);

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.admin_cambiar_rol_titulo))
                .setView(rg)
                .setPositiveButton(getString(R.string.admin_guardar), (d, w) -> {
                    String nuevoRol = rg.getCheckedRadioButtonId() == 2 ? "ROLE_ADMIN" : "ROLE_USER";
                    api.cambiarRol(u.getId(), nuevoRol).enqueue(new ApiCallback<Void>() {
                        @Override
                        public void onOk(Void body) {
                            u.setRol(nuevoRol);
                            adapter.actualizarItem(pos, u);
                            Toast.makeText(AdminUsuariosActivity.this,
                                    getString(R.string.admin_exito_cambiar_rol), Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFail(int code, String message) {
                            Toast.makeText(AdminUsuariosActivity.this,
                                    getString(R.string.admin_error_generico), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
