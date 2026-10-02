package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.model.comida.AnadirRespuesta;
import es.pmdm.gymprofit.model.comida.CantidadAnterior;

// ============================================================
// Anadidos — lo que se ha añadido sin salir de Añadir (decisión 1, lote 1.6.3)
//
// Cada fila tiene un «+» que añade al momento y se vuelve ✓; tocar el ✓ lo quita,
// exacto. Lo que se ve cambia al tocar, sin esperar a la API (momento 15), y la API va
// detrás, por orden y de una en una por alimento:
//   · Un segundo toque mientras el primero viaja espera su turno. Al volver el primero,
//     se mira lo que quiere la fila ahora y se hace lo que falta: nunca dos líneas a la
//     vez, ni un borrado perdido.
//   · Si añadir falla, la fila vuelve a «+»; si quitar falla, vuelve a ✓. Las dos cosas
//     se dicen (Oyente.fallo).
// Quitar es exacto (A2): si la línea es nueva, se borra; si el alimento ya estaba en la
// comida y se sumó, la línea vuelve a la cantidad que tenía (CantidadAnterior).
// Lo añadido desde la ficha, la hoja del escáner o tras crear un alimento entra aquí
// también ya hecho (marcar), y cuenta en la barra igual.
// Sin vistas ni red: la red la pone quien lo usa (Servidor), para probarlo solo.
// ============================================================
public final class Anadidos {

    /** Lo hecho de un alimento: la línea en que acabó y lo que tenía antes. */
    public static final class Anadido {
        private int alimentoId;
        @Nullable private String barcode;
        private String tipoComida;
        private AlimentoComida linea;
        @Nullable private CantidadAnterior anterior;
        // Lo añadido, para la fila y la barra: «1 huevo L (50 g)» y sus kcal.
        private String texto;
        private long kcal;

        public Anadido() {
        }

        public Anadido(int alimentoId, @Nullable String barcode, @NonNull String tipoComida,
                       @NonNull AlimentoComida linea, @Nullable CantidadAnterior anterior,
                       @NonNull String texto, long kcal) {
            this.alimentoId = alimentoId;
            this.barcode = barcode;
            this.tipoComida = tipoComida;
            this.linea = linea;
            this.anterior = anterior;
            this.texto = texto;
            this.kcal = kcal;
        }

        /** De la respuesta de POST /comidas/anadir. */
        @NonNull
        public static Anadido de(@NonNull AnadirRespuesta r, @Nullable String barcode, @NonNull String tipoComida,
                                 @NonNull String texto, long kcal) {
            return new Anadido(r.getLinea().getAlimentoId(), barcode, tipoComida, r.getLinea(), r.getAnterior(),
                    texto, kcal);
        }

        public int getAlimentoId() { return alimentoId; }

        @Nullable public String getBarcode() { return barcode; }

        @NonNull public String getTipoComida() { return tipoComida; }

        @NonNull public AlimentoComida getLinea() { return linea; }

        @Nullable public CantidadAnterior getAnterior() { return anterior; }

        @NonNull public String getTexto() { return texto; }

        public long getKcal() { return kcal; }

        /** El mismo, con la línea como quedó al actualizarla desde la ficha. */
        @NonNull
        public Anadido actualizado(@NonNull AlimentoComida nueva, @NonNull String texto, long kcal) {
            return new Anadido(alimentoId, barcode, tipoComida, nueva, anterior, texto, kcal);
        }
    }

    /** Lo que pide un «+»: a qué alimento, a qué comida, el cuerpo y lo que se enseña. */
    public static final class Pedido {
        public final int alimentoId;
        @Nullable public final String barcode;
        @NonNull public final String tipoComida;
        @NonNull public final Map<String, Object> cuerpo;
        @NonNull public final String texto;
        public final long kcal;

        public Pedido(int alimentoId, @Nullable String barcode, @NonNull String tipoComida,
                      @NonNull Map<String, Object> cuerpo, @NonNull String texto, long kcal) {
            this.alimentoId = alimentoId;
            this.barcode = barcode;
            this.tipoComida = tipoComida;
            this.cuerpo = cuerpo;
            this.texto = texto;
            this.kcal = kcal;
        }
    }

    /** La red: quien usa esto la pone, y las respuestas llegan en el hilo principal. */
    public interface Servidor {
        void anadir(@NonNull Pedido pedido, @NonNull Respuesta<AnadirRespuesta> respuesta);

        /** Borra la línea, o la deja con su cantidad anterior. */
        void deshacer(@NonNull Anadido anadido, @NonNull Respuesta<Void> respuesta);
    }

    public interface Respuesta<T> {
        void ok(@Nullable T valor);

        void fallo(int code, @Nullable String message);
    }

    /** Quien pinta: la fila de esa clave ha cambiado, o algo ha fallado. */
    public interface Oyente {
        void cambio(@NonNull String clave);

        /**
         * @param alAnadir true si falló añadir (la fila vuelve a «+»); false si falló
         *                 quitar (vuelve a ✓).
         */
        void fallo(@NonNull String clave, boolean alAnadir, int code, @Nullable String message);
    }

    /** Lo que se lleva de un alimento: lo que quiere la fila y lo que hay de verdad. */
    private static final class Estado {
        final String clave;
        boolean quiere;
        @Nullable Anadido hecho;
        @Nullable Pedido pedido;
        boolean viajando;

        Estado(String clave) {
            this.clave = clave;
        }
    }

    private final Servidor servidor;
    private final Oyente oyente;
    // Por clave («a:12», «c:8410…»); un producto sin materializar acaba con las dos.
    private final Map<String, Estado> estados = new HashMap<>();
    private final List<Runnable> alTerminar = new ArrayList<>();
    private boolean huboCambios;

    public Anadidos(@NonNull Servidor servidor, @NonNull Oyente oyente) {
        this.servidor = servidor;
        this.oyente = oyente;
    }

    /** La clave de un alimento: su id o, si aún no está en el catálogo, su código. */
    @NonNull
    public static String clave(int alimentoId, @Nullable String barcode) {
        return alimentoId > 0 ? "a:" + alimentoId : "c:" + barcode;
    }

    // ── Lo que hace la fila ─────────────────────────────────────────────────

    /** El «+» o el ✓ de una fila: cambia al momento y la API va detrás. */
    public void tocar(@NonNull Pedido pedido) {
        Estado e = estado(clave(pedido.alimentoId, pedido.barcode));
        e.quiere = !e.quiere;
        if (e.quiere) e.pedido = pedido;
        oyente.cambio(e.clave);
        reconciliar(e);
    }

    /** Lo añadido por otro camino (la ficha, el escáner, crear): ya está hecho. */
    public void marcar(@NonNull Anadido a) {
        Estado e = estado(clave(a.getAlimentoId(), null));
        if (a.getBarcode() != null) estados.put(clave(0, a.getBarcode()), e);
        e.quiere = true;
        e.hecho = a;
        huboCambios = true;
        oyente.cambio(e.clave);
    }

    /** La línea de un ✓ actualizada desde la ficha. */
    public void actualizar(int alimentoId, @NonNull AlimentoComida linea, @NonNull String texto, long kcal) {
        Estado e = estados.get(clave(alimentoId, null));
        if (e == null || e.hecho == null) return;
        e.hecho = e.hecho.actualizado(linea, texto, kcal);
        huboCambios = true;
        oyente.cambio(e.clave);
    }

    // ── Lo que se pregunta ──────────────────────────────────────────────────

    /** ¿Está la fila en ✓? */
    public boolean marcado(@NonNull String clave) {
        Estado e = estados.get(clave);
        return e != null && e.quiere;
    }

    /** Lo hecho de esa fila, o null si no hay nada hecho todavía. */
    @Nullable
    public Anadido hecho(@NonNull String clave) {
        Estado e = estados.get(clave);
        return e != null ? e.hecho : null;
    }

    /** Lo que dice la fila en ✓: lo hecho o, mientras viaja, lo pedido. */
    @Nullable
    public String texto(@NonNull String clave) {
        Estado e = estados.get(clave);
        if (e == null || !e.quiere) return null;
        if (e.hecho != null) return e.hecho.getTexto();
        return e.pedido != null ? e.pedido.texto : null;
    }

    @Nullable
    public Long kcal(@NonNull String clave) {
        Estado e = estados.get(clave);
        if (e == null || !e.quiere) return null;
        if (e.hecho != null) return e.hecho.getKcal();
        return e.pedido != null ? e.pedido.kcal : null;
    }

    /** Lo que va en la barra: cuántos, sus kcal y a qué comidas. */
    public static final class Resumen {
        public final int cuantos;
        public final long kcal;
        @NonNull public final Set<String> comidas;

        Resumen(int cuantos, long kcal, @NonNull Set<String> comidas) {
            this.cuantos = cuantos;
            this.kcal = kcal;
            this.comidas = comidas;
        }
    }

    @NonNull
    public Resumen resumen() {
        int n = 0;
        long kcal = 0;
        Set<String> comidas = new LinkedHashSet<>();
        for (Estado e : unicos()) {
            if (!e.quiere) continue;
            n++;
            if (e.hecho != null) {
                kcal += e.hecho.getKcal();
                comidas.add(e.hecho.getTipoComida());
            } else if (e.pedido != null) {
                kcal += e.pedido.kcal;
                comidas.add(e.pedido.tipoComida);
            }
        }
        return new Resumen(n, kcal, comidas);
    }

    /** ¿Queda algo por llegar a la API? */
    public boolean ocupado() {
        for (Estado e : estados.values()) if (e.viajando) return true;
        return false;
    }

    /** ¿Ha cambiado algo de verdad en las comidas (para que el diario recargue)? */
    public boolean huboCambios() {
        return huboCambios;
    }

    /** Hace {@code r} cuando todo haya llegado a la API (al momento, si ya está). */
    public void alTerminar(@NonNull Runnable r) {
        if (!ocupado()) {
            r.run();
            return;
        }
        alTerminar.add(r);
    }

    // ── La API, detrás ──────────────────────────────────────────────────────

    private void reconciliar(Estado e) {
        if (e.viajando) return;
        if (e.quiere && e.hecho == null && e.pedido != null) {
            Pedido p = e.pedido;
            e.viajando = true;
            servidor.anadir(p, new Respuesta<AnadirRespuesta>() {
                @Override
                public void ok(@Nullable AnadirRespuesta r) {
                    e.viajando = false;
                    if (r != null && r.getLinea() != null) {
                        e.hecho = Anadido.de(r, p.barcode, p.tipoComida, p.texto, p.kcal);
                        huboCambios = true;
                        // Un producto que se ha materializado: su id también es su clave.
                        estados.put(clave(e.hecho.getAlimentoId(), null), e);
                    } else {
                        e.quiere = false;
                    }
                    oyente.cambio(e.clave);
                    seguir(e);
                }

                @Override
                public void fallo(int code, @Nullable String message) {
                    e.viajando = false;
                    e.quiere = false;
                    oyente.cambio(e.clave);
                    oyente.fallo(e.clave, true, code, message);
                    seguir(e);
                }
            });
            return;
        }
        if (!e.quiere && e.hecho != null) {
            Anadido a = e.hecho;
            e.viajando = true;
            servidor.deshacer(a, new Respuesta<Void>() {
                @Override
                public void ok(@Nullable Void nada) {
                    e.viajando = false;
                    e.hecho = null;
                    huboCambios = true;
                    oyente.cambio(e.clave);
                    seguir(e);
                }

                @Override
                public void fallo(int code, @Nullable String message) {
                    e.viajando = false;
                    e.quiere = true;
                    oyente.cambio(e.clave);
                    oyente.fallo(e.clave, false, code, message);
                    seguir(e);
                }
            });
        }
    }

    // Tras una respuesta: lo que falte de esa fila y, si ya no queda nada, lo que esperaba.
    private void seguir(Estado e) {
        reconciliar(e);
        if (!ocupado() && !alTerminar.isEmpty()) {
            List<Runnable> pendientes = new ArrayList<>(alTerminar);
            alTerminar.clear();
            for (Runnable r : pendientes) r.run();
        }
    }

    private Estado estado(String clave) {
        Estado e = estados.get(clave);
        if (e == null) {
            e = new Estado(clave);
            estados.put(clave, e);
        }
        return e;
    }

    // Cada estado una vez, aunque tenga dos claves.
    private Set<Estado> unicos() {
        Set<Estado> s = new LinkedHashSet<>();
        for (Estado e : estados.values()) s.add(e);
        return s;
    }
}
