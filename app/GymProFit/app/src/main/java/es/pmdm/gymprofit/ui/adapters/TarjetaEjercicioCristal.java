package es.pmdm.gymprofit.ui.adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.util.LongSparseArray;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.cristal.Cristal;
import es.pmdm.gymprofit.ui.cristal.SombraCristal;

// ============================================================
// TarjetaEjercicioCristal — la tarjeta de cristal de cada ejercicio de la Sesión en vivo
// (GP-285, lote 2; DEC-070).
//
// La lista es plana: la cabecera de un ejercicio, cada serie y el pie son elementos sueltos,
// porque cada serie se quita deslizándola (SesionEnVivoAdapter). Con tarjetas opacas bastaba
// con pegar tres fondos del mismo color; el cristal estático no se puede partir así: su relleno
// es un degradado a 160° de esquina a esquina de la tarjeta, con borde y un brillo arriba, y
// partido en trozos se verían las juntas. Aquí la tarjeta entera se pinta de una pieza, detrás
// de sus elementos: el mismo dibujo que las tarjetas de Inicio (Cristal, radio 28) y, en claro,
// su misma sombra por fuera (SombraCristal).
//
// De una tarjeta más alta que la pantalla solo se ve un trozo, y lo que no se ve no está en la
// lista (se recicla). Para que el degradado no baile al desplazar, la caja se calcula entera:
// lo que queda fuera se mide con lo que cada pieza midió la última vez que se vio (y, si aún no
// se ha visto, con lo que mide una pieza igual).
//
// No crea nada al dibujar: un Cristal por ejercicio, al que solo se le cambia la caja cuando
// la tarjeta cambia de tamaño (al añadir o quitar una serie); al desplazar se mueve el lienzo.
// ============================================================
final class TarjetaEjercicioCristal extends RecyclerView.ItemDecoration {

    // Cuántas tarjetas se guardan dibujadas: de sobra para las que caben en una pantalla.
    private static final int MAXIMO = 12;

    private final Context contexto;
    private final float radio, difuminado, baja;
    private final int colorSombra;
    private final LongSparseArray<Cristal> cristales = new LongSparseArray<>();
    // Lo que midió cada cabecera (por su id) la última vez que se vio, y lo último que midieron
    // una cabecera, una serie y un pie cualesquiera.
    private final LongSparseArray<Integer> altoCabecera = new LongSparseArray<>();
    private int ultimaCabecera, ultimaSerie, ultimoPie;
    private SombraCristal sombra;

    /** @param contexto el de la lista: lleva el tema de cristal de la pantalla. */
    TarjetaEjercicioCristal(@NonNull Context contexto) {
        this.contexto = contexto;
        radio = contexto.getResources().getDimension(R.dimen.radio_protagonista);
        difuminado = contexto.getResources().getDimension(R.dimen.cristal_estatico_sombra_difuminado);
        baja = contexto.getResources().getDimension(R.dimen.cristal_estatico_sombra_baja);
        colorSombra = ContextCompat.getColor(contexto, R.color.gp_cristal_sombra);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView lista, @NonNull RecyclerView.State estado) {
        RecyclerView.Adapter<?> adapter = lista.getAdapter();
        if (adapter == null) return;
        int total = adapter.getItemCount();
        int n = lista.getChildCount();
        // La tarjeta abierta: de qué ejercicio es, dónde empieza y acaba lo que se ve de ella
        // y entre qué posiciones de la lista.
        long ejercicio = RecyclerView.NO_ID;
        float arriba = 0, abajo = 0;
        int izquierda = 0, derecha = 0, primera = -1, ultima = -1;
        for (int i = 0; i <= n; i++) {
            View hijo = i < n ? lista.getChildAt(i) : null;
            int posicion = hijo == null ? RecyclerView.NO_POSITION : lista.getChildAdapterPosition(hijo);
            int tipo = posicion == RecyclerView.NO_POSITION || posicion >= total
                    ? SesionEnVivoAdapter.FINAL : adapter.getItemViewType(posicion);
            boolean dePieza = hijo != null && posicion != RecyclerView.NO_POSITION
                    && tipo != SesionEnVivoAdapter.FINAL;
            if (dePieza) recordar(adapter, posicion, tipo, hijo);
            // Se cierra la tarjeta abierta si esto ya no es suyo: el final, otra cabecera o un salto.
            boolean sigue = dePieza && primera != -1 && tipo != SesionEnVivoAdapter.CABECERA
                    && posicion == ultima + 1;
            if (primera != -1 && !sigue) {
                pintar(canvas, adapter, ejercicio, izquierda, derecha, arriba, abajo, primera, ultima, total);
                primera = -1;
            }
            if (!dePieza) continue;
            float y0 = hijo.getTop() + hijo.getTranslationY(), y1 = hijo.getBottom() + hijo.getTranslationY();
            if (primera == -1) {
                primera = posicion;
                arriba = y0;
                izquierda = hijo.getLeft();
                derecha = hijo.getRight();
                ejercicio = cabeceraDe(adapter, posicion);
            }
            ultima = posicion;
            abajo = y1;
        }
    }

    // Lo que mide cada pieza, para cuando no esté a la vista.
    private void recordar(RecyclerView.Adapter<?> adapter, int posicion, int tipo, View hijo) {
        int alto = hijo.getHeight() + margenes(hijo);
        if (alto <= 0) return;
        if (tipo == SesionEnVivoAdapter.CABECERA) {
            ultimaCabecera = alto;
            altoCabecera.put(adapter.getItemId(posicion), alto);
        } else if (tipo == SesionEnVivoAdapter.SERIE) {
            ultimaSerie = alto;
        } else {
            ultimoPie = alto;
        }
    }

    private static int margenes(View hijo) {
        if (!(hijo.getLayoutParams() instanceof RecyclerView.LayoutParams)) return 0;
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) hijo.getLayoutParams();
        // El margen de arriba de la cabecera separa su tarjeta de la anterior: no es tarjeta.
        return lp.bottomMargin;
    }

    // El id de la cabecera del ejercicio al que pertenece una posición (hacia atrás hasta ella).
    private static long cabeceraDe(RecyclerView.Adapter<?> adapter, int posicion) {
        for (int p = posicion; p >= 0; p--) {
            if (adapter.getItemViewType(p) == SesionEnVivoAdapter.CABECERA) return adapter.getItemId(p);
        }
        return RecyclerView.NO_ID;
    }

    // La tarjeta de un ejercicio: lo que se ve de ella, más lo que queda por encima (hasta su
    // cabecera) y por debajo (hasta su pie), con lo que esas piezas midieron.
    private void pintar(Canvas canvas, RecyclerView.Adapter<?> adapter, long ejercicio, int izquierda, int derecha,
                        float arriba, float abajo, int primera, int ultima, int total) {
        for (int p = primera - 1; p >= 0 && adapter.getItemViewType(primera) != SesionEnVivoAdapter.CABECERA; p--) {
            int tipo = adapter.getItemViewType(p);
            if (tipo == SesionEnVivoAdapter.CABECERA) {
                Integer suya = altoCabecera.get(adapter.getItemId(p));
                arriba -= suya != null ? suya : ultimaCabecera;
                break;
            }
            arriba -= ultimaSerie;
        }
        for (int p = ultima + 1; p < total && adapter.getItemViewType(ultima) != SesionEnVivoAdapter.PIE; p++) {
            int tipo = adapter.getItemViewType(p);
            if (tipo == SesionEnVivoAdapter.PIE) {
                abajo += ultimoPie;
                break;
            }
            if (tipo != SesionEnVivoAdapter.SERIE) break;
            abajo += ultimaSerie;
        }
        int ancho = derecha - izquierda, alto = Math.round(abajo - arriba);
        if (ancho <= 0 || alto <= 0) return;
        Cristal cristal = cristales.get(ejercicio);
        if (cristal == null) {
            if (cristales.size() >= MAXIMO) cristales.removeAt(0);
            cristal = new Cristal(contexto, Cristal.TARJETA, radio);
            cristales.put(ejercicio, cristal);
        }
        if (cristal.getBounds().width() != ancho || cristal.getBounds().height() != alto) {
            cristal.setBounds(0, 0, ancho, alto);
        }
        canvas.save();
        canvas.translate(izquierda, Math.round(arriba));
        // En claro, la sombra del cristal estático, solo por fuera de la forma (en oscuro no hay).
        if (colorSombra >>> 24 != 0) {
            if (sombra == null) sombra = SombraCristal.de(radio, difuminado, baja);
            sombra.pintar(canvas, ancho, alto, colorSombra);
        }
        cristal.draw(canvas);
        canvas.restore();
    }
}
