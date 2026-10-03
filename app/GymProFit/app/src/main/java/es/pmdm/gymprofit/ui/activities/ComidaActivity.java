package es.pmdm.gymprofit.ui.activities;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.model.comida.AnadirRespuesta;
import es.pmdm.gymprofit.model.comida.Comida;
import es.pmdm.gymprofit.model.comida.ComidaReciente;
import es.pmdm.gymprofit.model.comida.CopiaRespuesta;
import es.pmdm.gymprofit.network.AlimentoApi;
import es.pmdm.gymprofit.network.AlimentoComidaApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.ui.adapters.AlimentoComidaAdapter;
import es.pmdm.gymprofit.ui.adapters.BusquedaAlimentoAdapter;
import es.pmdm.gymprofit.ui.nutricion.AnilloComida;
import es.pmdm.gymprofit.ui.nutricion.CopiarComida;
import es.pmdm.gymprofit.ui.nutricion.TarjetaCopiarAyer;
import es.pmdm.gymprofit.ui.nutricion.ElegirComida;
import es.pmdm.gymprofit.utils.AnadirRapido;
import es.pmdm.gymprofit.utils.CantidadFicha;
import es.pmdm.gymprofit.utils.Cantidades;
import es.pmdm.gymprofit.utils.ComidaQueToca;
import es.pmdm.gymprofit.utils.ComidasRecientes;
import es.pmdm.gymprofit.utils.DiaNutricion;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PedidoCantidad;
import es.pmdm.gymprofit.utils.QuitarConDeshacer;
import es.pmdm.gymprofit.utils.ResultadoNutricional;
import es.pmdm.gymprofit.utils.ResumenComida;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// ComidaActivity — una comida (decisión 16, tableros 2 y 2b, lote 1.6.2)
//
// Cabecera con la comida y el día. El resumen (ResumenComida): el anillo con las kcal
// dentro y el reparto de la ficha, los gramos de cada macro y qué parte son del objetivo
// del día, y la comida frente a las kcal del día con su barra; sin objetivo, solo el
// anillo y los gramos. Debajo, «N alimentos» y sus filas. Tocar una abre la ficha con
// «Actualizar».
// Quitar no pregunta (QuitarConDeshacer): deslizar la fila, la acción de TalkBack o el
// menú de mantener pulsado la quitan, los números pasan a su valor nuevo y sale el aviso
// con «Deshacer» encima de la barra de abajo. El borrado se manda cuando el aviso se va;
// al salir se espera su respuesta antes de volver al diario. Si falla, la fila vuelve.
// Sin alimentos (tablero 2b): el icono de la comida, «Aún no has apuntado…» y, si toca
// por la hora y el día es hoy, «Es la que toca ahora». Sin tarjeta de resumen.
// Desde la 1.6.4, vacía (tablero 2b): «¿Copiar la de ayer?» si la misma comida del día
// anterior tuvo algo, sea el día que sea (la ✓ copia y sus alimentos caen en la lista,
// momento 18; la ✗ la pliega para ese día y esa comida), y «Lo que sueles merendar», con
// un «+» que añade y deja la pantalla con la comida ya apuntada. Sin nada, no salen.
// Abajo, fija, «Añadir alimento» (Añadir con esta comida) y el escáner (con esta comida).
// Cargando y error, con view_estado; nada de diálogos que bloqueen.
// Extras: tipoComida, comidaId (-1 si aún no existe) y fecha (yyyy-MM-dd).
// ============================================================
public class ComidaActivity extends BaseActivity {

    private String tipoComida;
    private int comidaId;
    private String fecha;

    private final List<AlimentoComida> lineas = new ArrayList<>();
    private AlimentoComidaAdapter adapter;
    private VistaEstado estado;
    @Nullable private ResultadoNutricional objetivo;
    // La primera vez que hay algo que enseñar, el anillo se dibuja y las filas entran.
    private boolean yaPintada;
    private int kcalPintadas;
    @Nullable private ValueAnimator cuentaKcal;
    @Nullable private Snackbar aviso;

    private final AlimentoComidaApi alimentoComidaApi = ApiClient.service(AlimentoComidaApi.class);
    private final AlimentoApi alimentoApi = ApiClient.service(AlimentoApi.class);
    private QuitarConDeshacer<AlimentoComida> quitar;

    private ActivityResultLauncher<Intent> volverYRecargar;

    // Comida vacía (1.6.4): la de ayer que se copiaría y lo que sueles; null si no hay o
    // si falló. Lo que se está copiando o añadiendo, para no pedirlo dos veces.
    @Nullable private ComidaReciente copiaAyer;
    @Nullable private List<Alimento> sueles;
    private boolean copiando;
    private final java.util.Set<Integer> anadiendo = new java.util.HashSet<>();
    private int turnoSugerencias;
    // Al recargar tras copiar, las filas caen (momento 18); tras «Lo que sueles», entran.
    private boolean caerAlCargar;
    private boolean entrarAlCargar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comida);

        tipoComida = getIntent().getStringExtra("tipoComida");
        if (tipoComida == null) tipoComida = ComidaQueToca.ahora();
        comidaId = getIntent().getIntExtra("comidaId", -1);
        fecha = getIntent().getStringExtra("fecha");
        objetivo = DiaNutricion.objetivo(prefsManager);

        ((TextView) findViewById(R.id.tvTituloComida)).setText(ComidaQueToca.titulo(tipoComida));
        ((TextView) findViewById(R.id.tvDiaComida)).setText(ElegirComida.textoDia(this, fecha));
        findViewById(R.id.btnAtrasComida).setOnClickListener(v -> salir());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                salir();
            }
        });

        estado = new VistaEstado(findViewById(R.id.estadoComida));
        apilarConLetraGrande();
        findViewById(R.id.cardAlimentos).setClipToOutline(true);
        configurarLista();
        configurarQuitar();

        volverYRecargar = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() != RESULT_OK) return;
            // Se añadió a otra comida que esta: se dice a cuál (decisión 15).
            String a = r.getData() != null ? r.getData().getStringExtra(AnadirAlimentoActivity.EXTRA_ANADIDO_A) : null;
            if (a != null) UIHelper.mostrarToastExito(this, getString(ElegirComida.anadidoA(a)));
        });
        findViewById(R.id.btnAnadirComida).setOnClickListener(v -> volverYRecargar.launch(
                new Intent(this, AnadirAlimentoActivity.class)
                        .putExtra(AnadirAlimentoActivity.EXTRA_TIPO, tipoComida)
                        .putExtra(AnadirAlimentoActivity.EXTRA_FECHA, fecha)));
        findViewById(R.id.btnEscanearComida).setOnClickListener(v -> volverYRecargar.launch(
                new Intent(this, EscanerActivity.class)
                        .putExtra(AnadirAlimentoActivity.EXTRA_TIPO, tipoComida)
                        .putExtra(AnadirAlimentoActivity.EXTRA_FECHA, fecha)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargar();
    }

    // Salir a otra app también es salir de la pantalla: lo quitado se manda ya, y el aviso
    // ya no puede deshacerlo.
    @Override
    protected void onStop() {
        super.onStop();
        if (!isFinishing() && quitar.hayPendiente()) {
            quitar.confirmar();
            if (aviso != null) aviso.dismiss();
        }
    }

    // ── La lista ────────────────────────────────────────────────────────────

    private void configurarLista() {
        RecyclerView rv = findViewById(R.id.rvAlimentosComida);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlimentoComidaAdapter(lineas, new AlimentoComidaAdapter.Acciones() {
            @Override public void abrir(@NonNull AlimentoComida item) { abrirFicha(item); }
            @Override public void menu(@NonNull AlimentoComida item, @NonNull View fila) { mostrarMenu(item, fila); }
            @Override public void quitar(@NonNull AlimentoComida item) { quitarLinea(item); }
        });
        rv.setAdapter(adapter);
        // La comida se recoloca, y la fila vuelve con «Deshacer», con RECOLOCA (momento 20).
        DefaultItemAnimator animador = new DefaultItemAnimator();
        animador.setRemoveDuration(Movimiento.RECOLOCA);
        animador.setMoveDuration(Movimiento.RECOLOCA);
        animador.setAddDuration(Movimiento.RECOLOCA);
        animador.setChangeDuration(Movimiento.RECOLOCA);
        rv.setItemAnimator(animador);
        new ItemTouchHelper(new Deslizar()).attachToRecyclerView(rv);
    }

    /** Deslizar una fila hasta el final la quita; detrás, el rojo con «Eliminar». */
    private final class Deslizar extends ItemTouchHelper.SimpleCallback {
        private final Paint fondo = new Paint();
        private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
        @Nullable private final Drawable icono;
        private final float d = getResources().getDisplayMetrics().density;

        Deslizar() {
            super(0, ItemTouchHelper.LEFT);
            int rojo = ContextCompat.getColor(ComidaActivity.this, R.color.gp_error);
            fondo.setColor(ContextCompat.getColor(ComidaActivity.this, R.color.gp_error_container));
            texto.setColor(rojo);
            texto.setTextSize(getResources().getDimension(R.dimen.text_eyebrow));
            texto.setFakeBoldText(true);
            texto.setTextAlign(Paint.Align.CENTER);
            icono = ContextCompat.getDrawable(ComidaActivity.this, R.drawable.ic_ms_delete);
            if (icono != null) icono.setTintList(ColorStateList.valueOf(rojo));
        }

        @Override
        public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder a, @NonNull RecyclerView.ViewHolder b) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder h, int direccion) {
            int pos = h.getBindingAdapterPosition();
            if (pos >= 0 && pos < lineas.size()) quitarLinea(lineas.get(pos));
        }

        @Override
        public long getAnimationDuration(@NonNull RecyclerView rv, int tipo, float dx, float dy) {
            return tipo == ItemTouchHelper.ANIMATION_TYPE_SWIPE_SUCCESS ? Movimiento.SALE : Movimiento.RECOLOCA;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder h,
                                float dx, float dy, int accion, boolean activo) {
            View f = h.itemView;
            if (dx < 0) {
                c.drawRect(f.getRight() + dx, f.getTop(), f.getRight(), f.getBottom(), fondo);
                float cx = f.getRight() - 48 * d;
                float cy = f.getTop() + f.getHeight() / 2f;
                if (icono != null) {
                    int m = Math.round(12 * d);
                    icono.setBounds(Math.round(cx - m), Math.round(cy - 2 * m), Math.round(cx + m), Math.round(cy));
                    icono.draw(c);
                }
                c.drawText(getString(R.string.comida_eliminar_fondo), cx, cy + 16 * d, texto);
            }
            super.onChildDraw(c, rv, h, dx, dy, accion, activo);
        }
    }

    // Con letra grande, el anillo y los macros no caben lado a lado sin recortar el nombre
    // del macro: el anillo va arriba, centrado, y los macros debajo a todo el ancho.
    private void apilarConLetraGrande() {
        if (getResources().getConfiguration().fontScale < 1.3f) return;
        android.widget.LinearLayout fila = findViewById(R.id.filaAnilloMacros);
        fila.setOrientation(android.widget.LinearLayout.VERTICAL);
        fila.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        View columna = findViewById(R.id.columnaMacros);
        android.widget.LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) columna.getLayoutParams();
        lp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
        lp.weight = 0;
        lp.setMarginStart(0);
        lp.topMargin = Math.round(16 * getResources().getDisplayMetrics().density);
        columna.setLayoutParams(lp);
    }

    // ── Cargar ──────────────────────────────────────────────────────────────

    private void cargar() {
        if (!yaPintada) estado.cargando();
        if (comidaId == -1) {
            buscarComida();
        } else {
            cargarLineas();
        }
    }

    // Sin id (la comida no existía al abrir): se busca la del día, que puede haberse
    // creado al añadir desde aquí.
    private void buscarComida() {
        int usuarioId = prefsManager.getUsuarioId();
        String dia = fecha != null ? fecha
                : new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        ApiClient.service(ComidaApi.class).getDeUsuarioFecha(usuarioId, dia).enqueue(new ApiCallback<List<Comida>>() {
            @Override
            public void onOk(List<Comida> lista) {
                if (isDestroyed()) return;
                if (lista != null) {
                    for (Comida c : lista) if (tipoComida.equals(c.getTipoComida())) comidaId = c.getId();
                }
                if (comidaId == -1) {
                    lineas.clear();
                    adapter.notifyDataSetChanged();
                    pintar();
                } else {
                    cargarLineas();
                }
            }

            @Override
            public void onFail(int code, String message) {
                if (!isDestroyed()) fallo(code, message);
            }
        });
    }

    private void cargarLineas() {
        alimentoComidaApi.getDeComida(comidaId).enqueue(new ApiCallback<List<AlimentoComida>>() {
            @Override
            public void onOk(List<AlimentoComida> lista) {
                if (isDestroyed()) return;
                // Lo quitado no vuelve a la lista: ni lo que aún se puede deshacer ni lo
                // enviado sin responder (GP-179).
                lineas.clear();
                if (lista != null) {
                    for (AlimentoComida a : lista) {
                        if (!apartada(a)) lineas.add(a);
                    }
                }
                if (caerAlCargar) adapter.caerUnaTrasOtra();
                else if (!yaPintada || entrarAlCargar) adapter.entrarEnCascada();
                caerAlCargar = false;
                entrarAlCargar = false;
                adapter.notifyDataSetChanged();
                pintar();
            }

            @Override
            public void onFail(int code, String message) {
                if (!isDestroyed()) fallo(code, message);
            }
        });
    }

    private boolean apartada(AlimentoComida a) {
        for (AlimentoComida q : quitar.apartados()) if (q.getId() == a.getId()) return true;
        return false;
    }

    // Sin nada en pantalla, el error ocupa su sitio con «Reintentar»; con algo, se avisa y
    // se deja lo que había.
    private void fallo(int code, String message) {
        if (!yaPintada) {
            estado.error(VistaEstado.mensaje(this, R.string.comida_error_lista, code, message), this::cargar);
        } else {
            UiFeedback.toastError(this, code, message);
        }
    }

    // ── Pintar ──────────────────────────────────────────────────────────────

    private void pintar() {
        estado.oculto();
        boolean entrada = !yaPintada;
        yaPintada = true;
        ResumenComida r = ResumenComida.de(lineas, objetivo);
        View card = findViewById(R.id.cardResumen);
        View cuantos = findViewById(R.id.tvCuantos);
        View cardAlimentos = findViewById(R.id.cardAlimentos);
        View vacia = findViewById(R.id.bloqueVacia);
        if (r.vacia) {
            boolean yaVacia = vacia.getVisibility() == View.VISIBLE;
            card.setVisibility(View.GONE);
            cuantos.setVisibility(View.GONE);
            cardAlimentos.setVisibility(View.GONE);
            vacia.setVisibility(View.VISIBLE);
            pintarVacia(!yaVacia);
            if (!yaVacia) pedirSugerencias();
            kcalPintadas = 0;
            return;
        }
        boolean entraResumen = entrada || card.getVisibility() != View.VISIBLE;
        vacia.setVisibility(View.GONE);
        findViewById(R.id.bloqueSugerencias).setVisibility(View.GONE);
        card.setVisibility(View.VISIBLE);
        cuantos.setVisibility(View.VISIBLE);
        cardAlimentos.setVisibility(View.VISIBLE);
        ((TextView) cuantos).setText(getResources().getQuantityString(R.plurals.comida_n_alimentos,
                lineas.size(), lineas.size()));
        if (entraResumen) Movimiento.entrarUna(card, 0, Movimiento.ENTRA, 16);
        pintarResumen(r, entraResumen);
    }

    private void pintarResumen(ResumenComida r, boolean entrada) {
        NumberFormat nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(this));
        ((AnilloComida) findViewById(R.id.anilloComida)).mostrar(r.reparto, entrada);
        contarKcal(r.kcal, entrada, nf);
        View marco = findViewById(R.id.marcoAnillo);
        marco.setContentDescription(r.reparto == null
                ? getString(R.string.comida_anillo_sin_macros_a11y, nf.format(r.kcal))
                : getString(R.string.comida_anillo_a11y, nf.format(r.kcal), r.reparto[0], r.reparto[1], r.reparto[2]));

        findViewById(R.id.tvDeTuDia).setVisibility(r.sinObjetivo ? View.GONE : View.VISIBLE);
        int[] filas = {R.id.filaProteina, R.id.filaCarbos, R.id.filaGrasa};
        int[] nombres = {R.string.comida_proteina, R.string.comida_carbohidratos, R.string.comida_grasa};
        int[] colores = {R.color.gp_macro_proteinas, R.color.gp_macro_carbos, R.color.gp_macro_grasas};
        for (int i = 0; i < 3; i++) {
            View fila = findViewById(filas[i]);
            String nombre = getString(nombres[i]);
            String gramos = nf.format(Math.round(r.gramos(i)));
            fila.findViewById(R.id.vPuntoMacro).setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, colores[i])));
            ((TextView) fila.findViewById(R.id.tvNombreMacro)).setText(nombre);
            ((TextView) fila.findViewById(R.id.tvGramosMacro)).setText(getString(R.string.comida_gramos, gramos));
            TextView pct = fila.findViewById(R.id.tvPctMacro);
            int p = r.pctMacro(i);
            pct.setVisibility(p < 0 ? View.GONE : View.VISIBLE);
            if (p >= 0) pct.setText(getString(R.string.comida_pct, p));
            fila.setFocusable(true);
            fila.setContentDescription(p < 0 ? getString(R.string.comida_macro_sin_objetivo_a11y, nombre, gramos)
                    : getString(R.string.comida_macro_a11y, nombre, gramos, p));
        }

        View bloqueDia = findViewById(R.id.bloqueDia);
        bloqueDia.setVisibility(r.sinObjetivo ? View.GONE : View.VISIBLE);
        if (r.sinObjetivo) return;
        String pctTexto = getString(R.string.comida_pct, r.pctDia());
        String frase = getString(R.string.comida_dia_pct, pctTexto);
        SpannableString s = new SpannableString(frase);
        int ini = frase.indexOf(pctTexto);
        if (ini >= 0) {
            s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), ini, ini + pctTexto.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(new ForegroundColorSpan(color(com.google.android.material.R.attr.colorOnSurface)),
                    ini, ini + pctTexto.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        ((TextView) findViewById(R.id.tvDiaPct)).setText(s);
        String de = getString(R.string.comida_dia_de, nf.format(r.kcal), nf.format(r.objetivoKcal()));
        ((TextView) findViewById(R.id.tvDiaDe)).setText(de);
        bloqueDia.setContentDescription(getString(R.string.comida_dia_a11y, r.pctDia(), nf.format(r.kcal),
                nf.format(r.objetivoKcal())));
        View barra = findViewById(R.id.vBarraDia);
        barra.setPivotX(0f);
        barra.animate().cancel();
        if (Movimiento.quieto(this)) {
            barra.setScaleX(r.barraDia());
        } else {
            barra.animate().scaleX(r.barraDia()).setStartDelay(entrada ? 350 : 0).setDuration(Movimiento.BARRA)
                    .setInterpolator(Movimiento.ESTANDAR).start();
        }
    }

    // Las kcal del anillo pasan de las de antes a las nuevas con BARRA (momento 20).
    private void contarKcal(int nuevas, boolean entrada, NumberFormat nf) {
        TextView tv = findViewById(R.id.tvKcalComida);
        if (cuentaKcal != null) cuentaKcal.cancel();
        int desde = entrada ? nuevas : kcalPintadas;
        kcalPintadas = nuevas;
        if (desde == nuevas || Movimiento.quieto(this)) {
            tv.setText(nf.format(nuevas));
            return;
        }
        cuentaKcal = ValueAnimator.ofInt(desde, nuevas);
        cuentaKcal.setDuration(Movimiento.BARRA);
        cuentaKcal.setInterpolator(Movimiento.ESTANDAR);
        cuentaKcal.addUpdateListener(a -> tv.setText(nf.format((int) a.getAnimatedValue())));
        cuentaKcal.start();
    }

    private void pintarVacia(boolean entra) {
        ImageView icono = findViewById(R.id.ivIconoVacia);
        icono.setImageResource(ElegirComida.icono(tipoComida));
        TextView titulo = findViewById(R.id.tvVacia);
        titulo.setText(vaciaTexto(tipoComida));
        boolean toca = ElegirComida.esHoy(fecha) && tipoComida.equals(ComidaQueToca.ahora());
        findViewById(R.id.tvVaciaToca).setVisibility(toca ? View.VISIBLE : View.GONE);
        if (entra) {
            Movimiento.entrarUna(findViewById(R.id.bloqueVacia), 0, Movimiento.ENTRA, 16);
            View marco = findViewById(R.id.marcoIconoVacia);
            if (!Movimiento.quieto(this)) {
                marco.setScaleX(0.6f);
                marco.setScaleY(0.6f);
                marco.animate().scaleX(1f).scaleY(1f).setStartDelay(150).setDuration(Movimiento.CAPITULO)
                        .setInterpolator(Movimiento.REBOTE).start();
            }
        }
    }

    // ── Comida vacía: copiar la de ayer y lo que sueles (1.6.4) ─────────────

    private String dia() {
        return fecha != null ? fecha
                : new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
    }

    // Las dos se piden a la vez; cada una sale cuando llega, si la comida sigue vacía. Un
    // fallo no se enseña: son atajos, y la barra de abajo sigue ahí.
    private void pedirSugerencias() {
        int miTurno = ++turnoSugerencias;
        copiaAyer = null;
        sueles = null;
        pintarSugerencias(false);
        ComidaApi api = ApiClient.service(ComidaApi.class);
        String dia = dia();
        if (!prefsManager.copiaAyerDescartada(dia, tipoComida)) {
            api.recientes(dia, tipoComida).enqueue(new ApiCallback<List<ComidaReciente>>() {
                @Override
                public void onOk(List<ComidaReciente> lista) {
                    if (isDestroyed() || miTurno != turnoSugerencias) return;
                    copiaAyer = ComidasRecientes.deAyer(lista, tipoComida, dia);
                    pintarSugerencias(true);
                }

                @Override
                public void onFail(int code, String message) {
                    // Se ignora a propósito: sin la tarjeta, se apunta igual.
                }
            });
        }
        api.habituales(tipoComida).enqueue(new ApiCallback<List<Alimento>>() {
            @Override
            public void onOk(List<Alimento> lista) {
                if (isDestroyed() || miTurno != turnoSugerencias) return;
                sueles = lista != null && !lista.isEmpty() ? lista : null;
                pintarSugerencias(true);
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: sin «Lo que sueles», se apunta igual.
            }
        });
    }

    private void pintarSugerencias(boolean entra) {
        View bloque = findViewById(R.id.bloqueSugerencias);
        boolean vacia = lineas.isEmpty() && findViewById(R.id.bloqueVacia).getVisibility() == View.VISIBLE;
        android.view.ViewGroup ranura = findViewById(R.id.ranuraCopiarComida);
        View seccion = findViewById(R.id.seccionSueles);
        boolean conCopia = vacia && copiaAyer != null && !copiando;
        boolean conSueles = vacia && sueles != null;
        bloque.setVisibility(conCopia || conSueles ? View.VISIBLE : View.GONE);

        boolean copiaNueva = conCopia && ranura.getVisibility() != View.VISIBLE;
        ranura.setVisibility(conCopia ? View.VISIBLE : View.GONE);
        if (conCopia) {
            ranura.removeAllViews();
            View tarjeta = getLayoutInflater().inflate(R.layout.view_copiar_ayer_comida, ranura, false);
            ComidaReciente ayer = copiaAyer;
            TarjetaCopiarAyer.pintar(tarjeta, ayer, tipoComida, new TarjetaCopiarAyer.Respuesta() {
                @Override public void si() { copiarAyer(tarjeta, ayer); }
                @Override public void no() { noCopiarAyer(tarjeta); }
            });
            ranura.addView(tarjeta);
            if (entra && copiaNueva) Movimiento.entrarUna(tarjeta, 100, Movimiento.ENTRA, 16);
        }

        boolean suelesNuevos = conSueles && seccion.getVisibility() != View.VISIBLE;
        seccion.setVisibility(conSueles ? View.VISIBLE : View.GONE);
        if (conSueles) pintarSueles(entra && suelesNuevos);
    }

    private void pintarSueles(boolean entra) {
        ((TextView) findViewById(R.id.tvSueles)).setText(CopiarComida.sueles(tipoComida));
        android.widget.LinearLayout lista = findViewById(R.id.listaSueles);
        lista.removeAllViews();
        NumberFormat nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(this));
        List<Alimento> deEsta = sueles != null ? sueles : new ArrayList<>();
        for (int i = 0; i < deEsta.size(); i++) {
            Alimento a = deEsta.get(i);
            View fila = getLayoutInflater().inflate(R.layout.item_sueles, lista, false);
            CantidadFicha c = AnadirRapido.cantidad(a);
            String cantidad = AnadirRapido.texto(Cantidades.Formatos.de(this), FechaUtils.localeDeLaApp(this), c);
            String detalle = getString(R.string.fila_cantidad_kcal, cantidad, nf.format(AnadirRapido.kcal(a, c)));
            ((ImageView) fila.findViewById(R.id.ivIconoSueles)).setImageResource(BusquedaAlimentoAdapter.icono(a));
            ((TextView) fila.findViewById(R.id.tvNombreSueles)).setText(a.getNombre());
            ((TextView) fila.findViewById(R.id.tvCantidadSueles)).setText(detalle);
            fila.findViewById(R.id.textosSueles).setContentDescription(
                    getString(R.string.fila_a11y, a.getNombre(), detalle));
            fila.findViewById(R.id.rayaSueles).setVisibility(i < deEsta.size() - 1 ? View.VISIBLE : View.GONE);
            View mas = fila.findViewById(R.id.btnMasSueles);
            ImageView iconoMas = fila.findViewById(R.id.ivMasSueles);
            boolean viaja = anadiendo.contains(a.getId());
            BusquedaAlimentoAdapter.pintarMas(mas, iconoMas, viaja);
            mas.setContentDescription(viaja ? getString(R.string.fila_check_a11y, a.getNombre())
                    : getString(R.string.fila_mas_a11y, a.getNombre(), cantidad,
                    getString(AnadirAlimentoActivity.aLa(tipoComida))));
            mas.setOnClickListener(v -> anadirSueles(a, mas, iconoMas));
            lista.addView(fila);
            if (entra) Movimiento.entrarUna(fila, 200 + i * Movimiento.ENTRA_ESCALON, Movimiento.ENTRA, 16);
        }
    }

    // El «+» de «Lo que sueles»: se vuelve ✓ al momento (momento 15) y, al llegar la línea,
    // la pantalla pasa a la de la comida con ese alimento. Si falla, vuelve a «+» y se dice.
    private void anadirSueles(@NonNull Alimento a, @NonNull View mas, @NonNull ImageView iconoMas) {
        if (anadiendo.contains(a.getId())) return;
        anadiendo.add(a.getId());
        BusquedaAlimentoAdapter.pintarMas(mas, iconoMas, true);
        Movimiento.volverMas(mas, iconoMas, ContextCompat.getColor(this, R.color.gp_surface_2),
                ContextCompat.getColor(this, R.color.gp_success_container));
        Movimiento.vibrar(mas, Movimiento.Vibracion.LIGERA);
        ApiClient.service(ComidaApi.class).anadir(PedidoCantidad.anadir(a, AnadirRapido.cantidad(a), dia(), tipoComida))
                .enqueue(new ApiCallback<AnadirRespuesta>() {
                    @Override
                    public void onOk(AnadirRespuesta r) {
                        anadiendo.remove(a.getId());
                        if (isDestroyed()) return;
                        if (r != null && r.getComida() != null) comidaId = r.getComida().getId();
                        entrarAlCargar = true;
                        cargar();
                    }

                    @Override
                    public void onFail(int code, String message) {
                        anadiendo.remove(a.getId());
                        if (isDestroyed()) return;
                        pintarSugerencias(false);
                        UIHelper.mostrarToastError(ComidaActivity.this, getString(R.string.anadir_fallo_anadir,
                                a.getNombre(), UiFeedback.mensaje(ComidaActivity.this, code, message)));
                    }
                });
    }

    // La ✓: la tarjeta se pliega y la comida se copia; sus alimentos caen en la lista
    // (momento 18). Si falla, la tarjeta vuelve y se dice.
    private void copiarAyer(@NonNull View tarjeta, @NonNull ComidaReciente ayer) {
        if (copiando) return;
        copiando = true;
        Movimiento.vibrar(tarjeta, Movimiento.Vibracion.LIGERA);
        Movimiento.plegar(tarjeta, null);
        java.util.Map<String, Object> cuerpo = new java.util.HashMap<>();
        cuerpo.put("comidaId", ayer.getId());
        cuerpo.put("fecha", dia());
        cuerpo.put("tipoComida", tipoComida);
        ApiClient.service(ComidaApi.class).copiar(cuerpo).enqueue(new ApiCallback<CopiaRespuesta>() {
            @Override
            public void onOk(CopiaRespuesta r) {
                copiando = false;
                if (isDestroyed()) return;
                copiaAyer = null;
                if (r != null && r.getComida() != null) comidaId = r.getComida().getId();
                caerAlCargar = true;
                cargar();
            }

            @Override
            public void onFail(int code, String message) {
                copiando = false;
                if (isDestroyed()) return;
                pintarSugerencias(false);
                UIHelper.mostrarToastError(ComidaActivity.this, getString(R.string.copiar_ayer_fallo,
                        UiFeedback.mensaje(ComidaActivity.this, code, message)));
            }
        });
    }

    // La ✗: se pliega y no vuelve ese día para esta comida, ni aquí ni en el diario.
    private void noCopiarAyer(@NonNull View tarjeta) {
        prefsManager.descartarCopiaAyer(dia(), tipoComida);
        copiaAyer = null;
        Movimiento.plegar(tarjeta, () -> {
            if (!isDestroyed()) pintarSugerencias(false);
        });
    }

    private static int vaciaTexto(String tipo) {
        switch (tipo) {
            case "DESAYUNO": return R.string.comida_vacia_desayuno;
            case "ALMUERZO": return R.string.comida_vacia_almuerzo;
            case "COMIDA":   return R.string.comida_vacia_comida;
            case "CENA":     return R.string.comida_vacia_cena;
            default:         return R.string.comida_vacia_merienda;
        }
    }

    // ── Quitar con «Deshacer» ───────────────────────────────────────────────

    private void configurarQuitar() {
        quitar = new QuitarConDeshacer<>((item, respuesta) ->
                alimentoComidaApi.eliminar(item.getId()).enqueue(new ApiCallback<Void>() {
                    @Override
                    public void onOk(Void ignorado) {
                        respuesta.ok();
                    }

                    @Override
                    public void onFail(int code, String message) {
                        respuesta.fallo(code, message);
                    }
                }), new QuitarConDeshacer.Vista<AlimentoComida>() {
            @Override
            public void devolver(@NonNull AlimentoComida item, int posicion, int code, @Nullable String message) {
                if (isDestroyed()) return;
                volverAPoner(item, posicion);
                UiFeedback.toastError(ComidaActivity.this, code, message);
            }

            @Override
            public void listoParaSalir() {
                if (isDestroyed()) return;
                espera.removeCallbacks(verSaliendo);
                LoadingDialog.hide(ComidaActivity.this);
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void quitarLinea(@NonNull AlimentoComida item) {
        int pos = lineas.indexOf(item);
        if (pos < 0) return;
        Movimiento.vibrar(findViewById(R.id.rvAlimentosComida), Movimiento.Vibracion.LIGERA);
        lineas.remove(pos);
        adapter.notifyItemRemoved(pos);
        // La que queda última pierde su raya.
        if (pos > 0 && pos == lineas.size()) adapter.notifyItemChanged(pos - 1);
        quitar.quitar(item, pos);
        pintar();
        mostrarAviso(item);
    }

    // El aviso con «Deshacer», encima de la barra de abajo. 5 s: el LENGTH_LONG de Material
    // (2,75 s) no da para leer el nombre y llegar al botón. Material lo alarga además a lo
    // que pida el sistema por accesibilidad (getRecommendedTimeoutMillis); nunca menos.
    private static final int DURACION_AVISO = 5000;

    private void mostrarAviso(@NonNull AlimentoComida item) {
        aviso = Snackbar.make(findViewById(R.id.raizComida), getString(R.string.comida_quitado, item.getNombreAlimento()),
                DURACION_AVISO);
        aviso.setAnchorView(findViewById(R.id.barraComida));
        aviso.setAction(R.string.envivo_deshacer, v -> deshacer());
        // Invertido, como el lienzo: fondo del color del texto y la acción en naranja.
        aviso.setBackgroundTint(color(com.google.android.material.R.attr.colorOnSurface));
        aviso.setTextColor(color(com.google.android.material.R.attr.colorSurface));
        aviso.setActionTextColor(ContextCompat.getColor(this, R.color.gp_accion_inversa));
        aviso.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(Snackbar s, int evento) {
                // «Deshacer» ya ha devuelto la fila; si otro aviso lo ha sustituido, la
                // pendiente ya se mandó al quitar la siguiente. En los demás casos (tiempo,
                // deslizar el aviso, salir) se manda ahora.
                if (evento != DISMISS_EVENT_ACTION && evento != DISMISS_EVENT_CONSECUTIVE) quitar.confirmar();
                if (aviso == s) aviso = null;
            }
        });
        aviso.show();
    }

    private void deshacer() {
        QuitarConDeshacer.Pendiente<AlimentoComida> p = quitar.deshacer();
        if (p != null) volverAPoner(p.item, p.posicion);
    }

    private void volverAPoner(@NonNull AlimentoComida item, int posicion) {
        int pos = Math.max(0, Math.min(posicion, lineas.size()));
        boolean eraUltima = pos == lineas.size() && pos > 0;
        lineas.add(pos, item);
        adapter.notifyItemInserted(pos);
        if (eraUltima) adapter.notifyItemChanged(pos - 1);
        pintar();
    }

    // Al salir, lo quitado se manda y se espera su respuesta: el diario ya lo cuenta bien.
    // Si tarda más de ~300 ms, que se vea que está saliendo (GP-179): si no, parece que
    // atrás no ha hecho nada.
    private static final long ESPERA_SALIENDO = 300;
    private final android.os.Handler espera = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable verSaliendo = () -> {
        if (!isDestroyed()) LoadingDialog.show(this, getString(R.string.comida_saliendo));
    };

    private void salir() {
        quitar.salir();
        if (aviso != null) aviso.dismiss();
        if (!isFinishing()) {
            espera.removeCallbacks(verSaliendo);
            espera.postDelayed(verSaliendo, ESPERA_SALIENDO);
        }
    }

    @Override
    protected void onDestroy() {
        espera.removeCallbacks(verSaliendo);
        LoadingDialog.hide(this);
        super.onDestroy();
    }

    // ── Menú y ficha ────────────────────────────────────────────────────────

    // Mantener pulsado: cambiar la cantidad, quitar (sin preguntar, como deslizar) y, a un
    // ADMIN, desactivar el alimento del catálogo.
    private void mostrarMenu(AlimentoComida item, View fila) {
        boolean esAdmin = "ROLE_ADMIN".equals(prefsManager.getRol());
        List<UIHelper.MenuAction> acciones = new ArrayList<>();
        acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_edit, getString(R.string.comida_cambiar_cantidad),
                () -> abrirFicha(item)));
        if (esAdmin && item.getUsuarioIdAlimento() == null) {
            acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_visibility_off, getString(R.string.comida_desactivar_alimento),
                    () -> UIHelper.mostrarDialogoConIcono(this, getString(R.string.comida_desactivar_alimento),
                            getString(R.string.alimento_desactivar_confirmar), R.drawable.ic_ms_visibility_off,
                            () -> desactivarAlimento(item))));
        }
        acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_delete, getString(R.string.comida_quitar), true,
                () -> quitarLinea(item)));
        UIHelper.mostrarMenuAnclado(this, fila, item.getNombreAlimento(), acciones);
    }

    private void abrirFicha(AlimentoComida item) {
        volverYRecargar.launch(FichaAlimentoActivity.paraEditar(this, item, tipoComida, fecha));
    }

    // Solo ADMIN: desactiva el alimento del catálogo de esta línea (lo de antes).
    private void desactivarAlimento(AlimentoComida item) {
        alimentoApi.eliminar(item.getAlimentoId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignorado) {
                if (!isDestroyed()) cargar();
            }

            @Override
            public void onFail(int code, String message) {
                if (!isDestroyed()) UiFeedback.toastError(ComidaActivity.this, code, message);
            }
        });
    }

    private int color(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
