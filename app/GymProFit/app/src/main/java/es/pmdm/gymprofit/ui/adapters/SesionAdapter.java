package es.pmdm.gymprofit.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.sesion.PaginaHistorial;
import es.pmdm.gymprofit.ui.tarjetas.EntradaTarjeta;
import es.pmdm.gymprofit.ui.tarjetas.TextosTarjeta;
import es.pmdm.gymprofit.ui.widget.BarrasVecesView;
import es.pmdm.gymprofit.ui.widget.FilaQueBaja;
import es.pmdm.gymprofit.utils.ComparacionSesion;
import es.pmdm.gymprofit.utils.VistaEstado;

// ============================================================
// SesionAdapter — el Historial completo por semanas (GP-232, tablero «Historial»).
//
// Tres tipos de fila: la cabecera de cada semana («ESTA SEMANA», «2 entrenos y 22.050 kg»), la
// tarjeta de cada sesión (item_sesion) y, al final, el pie de la página siguiente (cargando o
// su error con «Reintentar», con VistaEstado). Todo sale de GET /sesiones/historial, una
// petición por página: ninguna tarjeta pide nada.
//
// Sin papelera: quitar una sesión (deslizar o la acción de TalkBack) la saca de la lista y
// descuenta su semana, que desaparece si se queda vacía; devolverla («Deshacer» o un fallo del
// borrado) la pone en su sitio y la semana vuelve con ella. El borrado lo manda quien lo usa,
// con QuitarConDeshacer.
// ============================================================
public class SesionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Lo que hace la pantalla con cada tarjeta. */
    public interface Acciones {
        void abrir(@NonNull PaginaHistorial.Sesion sesion, @NonNull View tarjeta);

        void borrar(@NonNull PaginaHistorial.Sesion sesion);

        /** El pie pide la página siguiente otra vez tras un fallo. */
        void reintentarPagina();
    }

    private static final int SEMANA = 0;
    private static final int SESION = 1;
    private static final int PIE = 2;

    /** Una semana de la lista, con lo que queda de ella tras quitar sesiones. */
    static final class Semana {
        final String lunes;
        int entrenos;
        double kilos;

        Semana(String lunes, int entrenos, double kilos) {
            this.lunes = lunes;
            this.entrenos = entrenos;
            this.kilos = kilos;
        }
    }

    /** Qué enseña el pie de la lista. */
    public enum Pie { NADA, CARGANDO, ERROR }

    private final List<Object> filas = new ArrayList<>();
    private final Map<Integer, Semana> semanaDe = new HashMap<>();
    private final TextosTarjeta textos;
    private final EntradaTarjeta entrada;
    private final Acciones acciones;
    private Pie pie = Pie.NADA;
    @Nullable private CharSequence errorPie;

    public SesionAdapter(@NonNull TextosTarjeta textos, @NonNull EntradaTarjeta entrada, @NonNull Acciones acciones) {
        this.textos = textos;
        this.entrada = entrada;
        this.acciones = acciones;
    }

    // ── Datos ────────────────────────────────────────────────────────────────

    /** Vacía la lista (al recargar desde la primera página). */
    public void vaciar() {
        int n = getItemCount();
        filas.clear();
        semanaDe.clear();
        pie = Pie.NADA;
        notifyItemRangeRemoved(0, n);
    }

    /**
     * Añade una página al final. Una semana nunca se parte entre páginas (la API lo garantiza);
     * lo apartado (quitado sin confirmar o en camino) no vuelve a la lista.
     */
    public void anadir(@NonNull PaginaHistorial pagina, @NonNull List<PaginaHistorial.Sesion> apartadas) {
        int desde = filas.size();
        for (PaginaHistorial.Semana w : pagina.getSemanas()) {
            Semana semana = new Semana(w.getLunes(), 0, 0);
            List<PaginaHistorial.Sesion> deLaSemana = new ArrayList<>();
            for (PaginaHistorial.Sesion s : w.getSesiones()) {
                semanaDe.put(s.getId(), semana);
                if (apartada(s, apartadas)) continue;
                deLaSemana.add(s);
                semana.entrenos++;
                semana.kilos += s.getKilos();
            }
            if (deLaSemana.isEmpty()) continue;
            filas.add(semana);
            filas.addAll(deLaSemana);
        }
        notifyItemRangeInserted(desde, filas.size() - desde);
    }

    private static boolean apartada(PaginaHistorial.Sesion s, List<PaginaHistorial.Sesion> apartadas) {
        for (PaginaHistorial.Sesion a : apartadas) if (a.getId() == s.getId()) return true;
        return false;
    }

    /** Cuántas sesiones hay en la lista. */
    public int sesiones() {
        int n = 0;
        for (Object f : filas) if (f instanceof PaginaHistorial.Sesion) n++;
        return n;
    }

    /** La sesión de una posición, o null si es una cabecera o el pie. */
    @Nullable
    public PaginaHistorial.Sesion sesionEn(int posicion) {
        if (posicion < 0 || posicion >= filas.size()) return null;
        Object f = filas.get(posicion);
        return f instanceof PaginaHistorial.Sesion ? (PaginaHistorial.Sesion) f : null;
    }

    /** Saca la sesión y descuenta su semana; la semana se va si se queda vacía. */
    public void quitar(@NonNull PaginaHistorial.Sesion s) {
        int pos = posicionDe(s.getId());
        if (pos < 0) return;
        filas.remove(pos);
        notifyItemRemoved(pos);
        Semana semana = semanaDe.get(s.getId());
        if (semana == null) return;
        semana.entrenos--;
        semana.kilos -= s.getKilos();
        int cabecera = filas.indexOf(semana);
        if (cabecera < 0) return;
        if (semana.entrenos <= 0) {
            filas.remove(cabecera);
            notifyItemRemoved(cabecera);
        } else {
            notifyItemChanged(cabecera);
        }
    }

    /** Devuelve la sesión a su sitio (por fecha, de la más nueva a la más vieja) y a su semana. */
    public void devolver(@NonNull PaginaHistorial.Sesion s) {
        if (posicionDe(s.getId()) >= 0) return;
        Semana semana = semanaDe.get(s.getId());
        if (semana == null) return;
        int cabecera = filas.indexOf(semana);
        if (cabecera < 0) {
            cabecera = 0;
            while (cabecera < filas.size() && !(filas.get(cabecera) instanceof Semana
                    && ((Semana) filas.get(cabecera)).lunes.compareTo(semana.lunes) < 0)) cabecera++;
            filas.add(cabecera, semana);
            notifyItemInserted(cabecera);
        }
        int pos = cabecera + 1;
        while (pos < filas.size() && filas.get(pos) instanceof PaginaHistorial.Sesion
                && despues((PaginaHistorial.Sesion) filas.get(pos), s)) pos++;
        filas.add(pos, s);
        notifyItemInserted(pos);
        semana.entrenos++;
        semana.kilos += s.getKilos();
        notifyItemChanged(cabecera);
    }

    // true si a va antes que b en la lista (es más nueva).
    private static boolean despues(PaginaHistorial.Sesion a, PaginaHistorial.Sesion b) {
        String fa = a.getFechaInicio() == null ? "" : a.getFechaInicio();
        String fb = b.getFechaInicio() == null ? "" : b.getFechaInicio();
        int c = fa.compareTo(fb);
        return c > 0 || (c == 0 && a.getId() > b.getId());
    }

    private int posicionDe(int sesionId) {
        for (int i = 0; i < filas.size(); i++) {
            Object f = filas.get(i);
            if (f instanceof PaginaHistorial.Sesion && ((PaginaHistorial.Sesion) f).getId() == sesionId) return i;
        }
        return -1;
    }

    /** El pie: nada, cargando la página siguiente o su error. */
    public void setPie(@NonNull Pie pie, @Nullable CharSequence error) {
        boolean habia = this.pie != Pie.NADA;
        this.pie = pie;
        this.errorPie = error;
        int pos = filas.size();
        if (habia && pie == Pie.NADA) notifyItemRemoved(pos);
        else if (!habia && pie != Pie.NADA) notifyItemInserted(pos);
        else if (pie != Pie.NADA) notifyItemChanged(pos);
    }

    // ── RecyclerView ─────────────────────────────────────────────────────────

    @Override
    public int getItemCount() {
        return filas.size() + (pie == Pie.NADA ? 0 : 1);
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= filas.size()) return PIE;
        return filas.get(position) instanceof Semana ? SEMANA : SESION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == SEMANA) return new SemanaVH(inf.inflate(R.layout.item_semana_historial, parent, false));
        if (viewType == PIE) return new PieVH(inf.inflate(R.layout.view_estado, parent, false));
        return new SesionVH(inf.inflate(R.layout.item_sesion, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        if (h instanceof SemanaVH) ((SemanaVH) h).pintar((Semana) filas.get(position));
        else if (h instanceof PieVH) ((PieVH) h).pintar();
        else ((SesionVH) h).pintar((PaginaHistorial.Sesion) filas.get(position));
    }

    final class SemanaVH extends RecyclerView.ViewHolder {
        final TextView titulo, resumen;

        SemanaVH(@NonNull View v) {
            super(v);
            titulo = v.findViewById(R.id.tvSemanaTitulo);
            resumen = v.findViewById(R.id.tvSemanaResumen);
            ViewCompat.setAccessibilityHeading(v, true);
        }

        void pintar(Semana w) {
            String t = textos.semana(w.lunes);
            titulo.setText(t);
            resumen.setText(textos.resumenSemana(w.entrenos, w.kilos));
            // En mayúsculas TalkBack deletrea: se le da en frase, con el mes como se escribe.
            itemView.setContentDescription(textos.semanaA11y(w.lunes) + ": "
                    + textos.resumenSemanaA11y(w.entrenos, w.kilos));
        }
    }

    final class PieVH extends RecyclerView.ViewHolder {
        final VistaEstado estado;

        PieVH(@NonNull View v) {
            super(v);
            estado = new VistaEstado(v);
        }

        void pintar() {
            if (pie == Pie.ERROR) estado.error(errorPie == null ? "" : errorPie, acciones::reintentarPagina);
            else estado.cargando();
        }
    }

    /** Una tarjeta de sesión; también la usa la pantalla para dibujar el fondo al deslizar. */
    public final class SesionVH extends RecyclerView.ViewHolder {
        final TextView rutina, cuando, zonas, kilos, comparacion, meta;
        final BarrasVecesView barras;
        final FilaQueBaja fila;
        private int accionBorrar = View.NO_ID;

        SesionVH(@NonNull View v) {
            super(v);
            rutina = v.findViewById(R.id.tvRutinaSesion);
            cuando = v.findViewById(R.id.tvCuandoSesion);
            zonas = v.findViewById(R.id.tvZonasSesion);
            kilos = v.findViewById(R.id.tvKilosSesion);
            comparacion = v.findViewById(R.id.tvComparacionSesion);
            meta = v.findViewById(R.id.tvMetaSesion);
            barras = v.findViewById(R.id.barrasSesion);
            fila = v.findViewById(R.id.filaKilosSesion);
        }

        void pintar(PaginaHistorial.Sesion s) {
            rutina.setText(textos.titulo(s));
            cuando.setText(textos.cuandoHistorial(s.getFechaInicio()));
            String z = textos.zonas(s.getMusculos());
            zonas.setText(z);
            zonas.setVisibility(z.isEmpty() ? View.GONE : View.VISIBLE);

            // Kilos con sus barras, «3 series» o el aviso (CifraTarjeta).
            textos.pintarCifra(kilos, textos.cifra(s), entrada, "s:" + s.getId(),
                    R.style.TextAppearance_GymProFit_Cifra_Pequena, R.dimen.text_tarjeta_kg, barras,
                    s.esLibre() ? new ArrayList<>() : s.getBarras());
            ComparacionSesion c = textos.comparacion(s);
            textos.pintarComparacion(comparacion, c);
            CharSequence m = textos.meta(s);
            meta.setText(m);
            meta.setVisibility(m.length() == 0 ? View.GONE : View.VISIBLE);
            // Sin la comparación, la línea de abajo se separa algo más de los kilos (el lienzo).
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) meta.getLayoutParams();
            lp.topMargin = itemView.getResources().getDimensionPixelSize(c == null ? R.dimen.espacio_8 : R.dimen.espacio_4);
            meta.setLayoutParams(lp);

            itemView.setContentDescription(textos.sesionA11y(s));
            itemView.setOnClickListener(v -> acciones.abrir(s, v));
            if (accionBorrar != View.NO_ID) ViewCompat.removeAccessibilityAction(itemView, accionBorrar);
            accionBorrar = ViewCompat.addAccessibilityAction(itemView, textos.borrarA11y(s), (v, args) -> {
                acciones.borrar(s);
                return true;
            });
        }
    }
}
