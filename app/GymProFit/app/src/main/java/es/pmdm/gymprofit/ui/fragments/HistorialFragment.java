package es.pmdm.gymprofit.ui.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.sesion.PaginaHistorial;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.activities.ResumenSesionActivity;
import es.pmdm.gymprofit.ui.adapters.SesionAdapter;
import es.pmdm.gymprofit.ui.tarjetas.EntradaTarjeta;
import es.pmdm.gymprofit.ui.tarjetas.TextosTarjeta;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.QuitarConDeshacer;
import es.pmdm.gymprofit.utils.Transiciones;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// HistorialFragment — el Historial completo de Progreso (GP-105; rehecho en GP-232).
//
// Las sesiones por semanas, de GET /sesiones/historial: una petición por página de hasta 8
// semanas, y la siguiente cuando la lista llega cerca del final. Cada tarjeta trae ya sus kilos,
// sus barras y sus récords: ninguna pide nada. Carga, error y vacío, dentro de la sección y sin
// toast (VistaEstado).
//
// Borrar no pregunta (QuitarConDeshacer, como en Comida): deslizar a la izquierda o la acción de
// TalkBack quitan la tarjeta y sale «Deshacer» durante 5 s; el borrado se manda cuando el aviso
// se va (por tiempo, al quitar otra o al salir). Si falla, la tarjeta vuelve y se dice. Tocar
// una tarjeta abre su resumen, que crece desde ella.
// ============================================================
public class HistorialFragment extends BaseFragment {

    // 5 s: el LENGTH_LONG de Material no da para leer el nombre y llegar al botón (como Comida).
    private static final int DURACION_AVISO = 5000;
    // A cuántas filas del final se pide la página siguiente.
    private static final int ANTES_DEL_FINAL = 4;

    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);

    private RecyclerView rv;
    private VistaEstado estado;
    private SesionAdapter adapter;
    private TextosTarjeta textos;
    private final EntradaTarjeta entrada = new EntradaTarjeta();
    private QuitarConDeshacer<PaginaHistorial.Sesion> quitar;
    @Nullable private Snackbar aviso;

    @Nullable private String siguiente;
    private boolean cargandoPagina;
    private boolean falloPagina;
    // Cada carga desde el principio invalida las respuestas de la anterior.
    private int tanda;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textos = new TextosTarjeta(requireContext());
        estado = new VistaEstado(findViewById(R.id.estadoHistorial));
        rv = findViewById(R.id.rvSesiones);
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        rv.setLayoutManager(lm);
        adapter = new SesionAdapter(textos, entrada, new SesionAdapter.Acciones() {
            @Override public void abrir(@NonNull PaginaHistorial.Sesion s, @NonNull View tarjeta) { abrirResumen(s, tarjeta); }
            @Override public void borrar(@NonNull PaginaHistorial.Sesion s) { quitarSesion(s); }
            @Override public void reintentarPagina() { pedirPagina(); }
        });
        rv.setAdapter(adapter);
        // Borrar y deshacer (momento 20): la lista se recoloca con RECOLOCA.
        DefaultItemAnimator animador = new DefaultItemAnimator();
        animador.setRemoveDuration(Movimiento.RECOLOCA);
        animador.setMoveDuration(Movimiento.RECOLOCA);
        animador.setAddDuration(Movimiento.RECOLOCA);
        animador.setChangeDuration(Movimiento.RECOLOCA);
        rv.setItemAnimator(animador);
        new ItemTouchHelper(new Deslizar()).attachToRecyclerView(rv);
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView r, int dx, int dy) {
                if (siguiente != null && !cargandoPagina && !falloPagina
                        && lm.findLastVisibleItemPosition() >= adapter.getItemCount() - ANTES_DEL_FINAL) pedirPagina();
            }
        });
        configurarQuitar();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    // Salir a otra pantalla también es salir: lo quitado se manda ya y el aviso ya no lo deshace.
    @Override
    public void onStop() {
        super.onStop();
        if (quitar.hayPendiente()) {
            quitar.confirmar();
            if (aviso != null) aviso.dismiss();
        }
    }

    // ── Carga ────────────────────────────────────────────────────────────────

    /** Desde la primera página. Lo que ya había se queda hasta que llega lo nuevo. */
    private void cargar() {
        int esta = ++tanda;
        cargandoPagina = true;
        falloPagina = false;
        if (adapter.sesiones() == 0) {
            rv.setVisibility(View.GONE);
            estado.cargando();
        }
        sesionApi.historial(null).enqueue(new ApiCallback<PaginaHistorial>() {
            @Override
            public void onOk(PaginaHistorial p) {
                if (!isAdded() || esta != tanda) return;
                cargandoPagina = false;
                adapter.vaciar();
                adapter.anadir(p != null ? p : PaginaHistorial.de(new java.util.ArrayList<>(), null), quitar.apartados());
                siguiente = p != null ? p.getSiguiente() : null;
                if (adapter.sesiones() == 0) {
                    rv.setVisibility(View.GONE);
                    estado.vacio(R.string.sesiones_sin_sesiones);
                } else {
                    estado.oculto();
                    rv.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFail(int code, String message) {
                if (!isAdded() || esta != tanda) return;
                cargandoPagina = false;
                // Con la lista ya puesta, un fallo al volver no la tapa: se queda lo que había.
                if (adapter.sesiones() > 0) return;
                rv.setVisibility(View.GONE);
                estado.error(VistaEstado.mensaje(requireContext(), R.string.historial_error, code, message),
                        HistorialFragment.this::cargar);
            }
        });
    }

    private void pedirPagina() {
        if (siguiente == null || cargandoPagina) return;
        int esta = tanda;
        cargandoPagina = true;
        falloPagina = false;
        adapter.setPie(SesionAdapter.Pie.CARGANDO, null);
        sesionApi.historial(siguiente).enqueue(new ApiCallback<PaginaHistorial>() {
            @Override
            public void onOk(PaginaHistorial p) {
                if (!isAdded() || esta != tanda) return;
                cargandoPagina = false;
                adapter.setPie(SesionAdapter.Pie.NADA, null);
                if (p != null) adapter.anadir(p, quitar.apartados());
                siguiente = p != null ? p.getSiguiente() : null;
            }

            @Override
            public void onFail(int code, String message) {
                if (!isAdded() || esta != tanda) return;
                cargandoPagina = false;
                falloPagina = true;
                adapter.setPie(SesionAdapter.Pie.ERROR,
                        VistaEstado.mensaje(requireContext(), R.string.historial_error, code, message));
            }
        });
    }

    private void abrirResumen(PaginaHistorial.Sesion sesion, View tarjeta) {
        Intent intent = new Intent(requireContext(), ResumenSesionActivity.class);
        intent.putExtra("sesionId", sesion.getId());
        Transiciones.abrirTarjeta(requireActivity(), tarjeta, intent);
    }

    // ── Quitar con «Deshacer» ───────────────────────────────────────────────

    private void configurarQuitar() {
        quitar = new QuitarConDeshacer<>((s, respuesta) ->
                sesionApi.eliminar(s.getId()).enqueue(new ApiCallback<Void>() {
                    @Override public void onOk(Void ignorado) { respuesta.ok(); }
                    @Override public void onFail(int code, String message) { respuesta.fallo(code, message); }
                }), new QuitarConDeshacer.Vista<PaginaHistorial.Sesion>() {
            @Override
            public void devolver(@NonNull PaginaHistorial.Sesion s, int posicion, int code, @Nullable String message) {
                if (!isAdded()) return;
                adapter.devolver(s);
                rv.setVisibility(View.VISIBLE);
                estado.oculto();
                // El fallo se dice en el mismo sitio que «Deshacer», sin toast.
                Snackbar.make(rv, UiFeedback.mensaje(requireContext(), code, message), DURACION_AVISO).show();
            }

            @Override
            public void listoParaSalir() {
                // El Historial no espera a nada para salir: la lista de detrás se recarga al volver.
            }
        });
    }

    private void quitarSesion(@NonNull PaginaHistorial.Sesion s) {
        Movimiento.vibrar(rv, Movimiento.Vibracion.LIGERA);
        adapter.quitar(s);
        quitar.quitar(s, 0);
        if (adapter.sesiones() == 0 && siguiente == null) {
            rv.setVisibility(View.GONE);
            estado.vacio(R.string.sesiones_sin_sesiones);
        }
        mostrarAviso(s);
    }

    private void mostrarAviso(@NonNull PaginaHistorial.Sesion s) {
        aviso = Snackbar.make(rv, getString(R.string.historial_borrada, textos.titulo(s)), DURACION_AVISO);
        aviso.setAction(R.string.envivo_deshacer, v -> deshacer());
        // Invertido, como en Comida: fondo del color del texto y la acción en naranja.
        aviso.setBackgroundTint(MaterialColors.getColor(rv, com.google.android.material.R.attr.colorOnSurface));
        aviso.setTextColor(MaterialColors.getColor(rv, com.google.android.material.R.attr.colorSurface));
        aviso.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.gp_accion_inversa));
        aviso.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(Snackbar sb, int evento) {
                // «Deshacer» ya la devolvió; si otro aviso lo sustituye, la pendiente ya se mandó al
                // quitar la siguiente. En los demás casos (tiempo, deslizar el aviso, salir) se manda.
                if (evento != DISMISS_EVENT_ACTION && evento != DISMISS_EVENT_CONSECUTIVE) quitar.confirmar();
                if (aviso == sb) aviso = null;
            }
        });
        aviso.show();
    }

    private void deshacer() {
        QuitarConDeshacer.Pendiente<PaginaHistorial.Sesion> p = quitar.deshacer();
        if (p == null) return;
        adapter.devolver(p.item);
        estado.oculto();
        rv.setVisibility(View.VISIBLE);
    }

    /** Deslizar una tarjeta hasta el final la quita; detrás, el rojo con «Borrar». */
    private final class Deslizar extends ItemTouchHelper.SimpleCallback {
        private final Paint fondo = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        @Nullable private final Drawable icono;
        private final float d = getResources().getDisplayMetrics().density;
        private final float radio = getResources().getDimension(R.dimen.radio_tarjeta);

        Deslizar() {
            super(0, ItemTouchHelper.LEFT);
            int rojo = ContextCompat.getColor(requireContext(), R.color.gp_error);
            fondo.setColor(ContextCompat.getColor(requireContext(), R.color.gp_error_container));
            texto.setColor(rojo);
            texto.setTextSize(getResources().getDimension(R.dimen.text_eyebrow));
            texto.setFakeBoldText(true);
            texto.setTextAlign(Paint.Align.CENTER);
            icono = ContextCompat.getDrawable(requireContext(), R.drawable.ic_ms_delete);
            if (icono != null) icono.setTintList(ColorStateList.valueOf(rojo));
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder h) {
            // Solo las tarjetas: ni la cabecera de la semana ni el pie se deslizan.
            return h instanceof SesionAdapter.SesionVH ? super.getMovementFlags(r, h) : 0;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder a, @NonNull RecyclerView.ViewHolder b) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder h, int direccion) {
            PaginaHistorial.Sesion s = adapter.sesionEn(h.getBindingAdapterPosition());
            if (s != null) quitarSesion(s);
        }

        @Override
        public long getAnimationDuration(@NonNull RecyclerView r, int tipo, float dx, float dy) {
            return tipo == ItemTouchHelper.ANIMATION_TYPE_SWIPE_SUCCESS ? Movimiento.SALE : Movimiento.RECOLOCA;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder h,
                                float dx, float dy, int accion, boolean activo) {
            View f = h.itemView;
            if (dx < 0) {
                rect.set(f.getLeft(), f.getTop(), f.getRight(), f.getBottom());
                c.drawRoundRect(rect, radio, radio, fondo);
                float cx = f.getRight() - 48 * d;
                float cy = f.getTop() + f.getHeight() / 2f;
                if (icono != null) {
                    int m = Math.round(12 * d);
                    icono.setBounds(Math.round(cx - m), Math.round(cy - 2 * m), Math.round(cx + m), Math.round(cy));
                    icono.draw(c);
                }
                c.drawText(getString(R.string.historial_borrar_fondo), cx, cy + 16 * d, texto);
            }
            super.onChildDraw(c, r, h, dx, dy, accion, activo);
        }
    }
}
