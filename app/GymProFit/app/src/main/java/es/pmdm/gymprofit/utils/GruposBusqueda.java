package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.alimento.Alimento;

// ============================================================
// GruposBusqueda — de los resultados de /alimentos/buscar a la lista de la pantalla (1.6.1)
//
// La API devuelve cada alimento con su grupo (TUYO, BASICO o PRODUCTO), ya en orden.
//   · Sin escribir (o con menos de 2 letras), Añadir: «Recientes» (TUYO) y
//     «Habituales» (BASICO).
//   · Buscando: «Tuyo», «Básicos» y «Productos», en ese orden, y al final
//     «¿No lo encuentras?», también cuando no hay nada.
// Cada grupo lleva su cabecera solo si tiene algo. Un grupo que no se conoce va con los
// básicos: mejor enseñarlo que perderlo.
// ============================================================
public final class GruposBusqueda {

    /** Menos letras que esto no es una búsqueda: se queda la lista de Añadir. */
    public static final int LETRAS_MINIMAS = 2;

    public enum Seccion { RECIENTES, HABITUALES, TUYO, BASICOS, PRODUCTOS }

    public enum Tipo { CABECERA, ALIMENTO, NO_LO_ENCUENTRAS }

    /** Una fila de la lista: una cabecera de sección, un alimento o «¿No lo encuentras?». */
    public static final class Elemento {
        @NonNull public final Tipo tipo;
        @Nullable public final Seccion seccion;
        @Nullable public final Alimento alimento;

        private Elemento(@NonNull Tipo tipo, @Nullable Seccion seccion, @Nullable Alimento alimento) {
            this.tipo = tipo;
            this.seccion = seccion;
            this.alimento = alimento;
        }

        public boolean esCabecera() {
            return tipo == Tipo.CABECERA;
        }
    }

    private GruposBusqueda() {
    }

    /** ¿Es lo escrito una búsqueda (2 letras o más, sin contar espacios)? */
    public static boolean esBusqueda(@Nullable CharSequence texto) {
        return texto != null && texto.toString().trim().length() >= LETRAS_MINIMAS;
    }

    /**
     * @param alimentos lo que devolvió la búsqueda, en su orden.
     * @param buscando  true con texto (los tres grupos y «¿No lo encuentras?»).
     */
    @NonNull
    public static List<Elemento> de(@Nullable List<Alimento> alimentos, boolean buscando) {
        Map<Seccion, List<Alimento>> porSeccion = new EnumMap<>(Seccion.class);
        if (alimentos != null) {
            for (Alimento a : alimentos) {
                porSeccion.computeIfAbsent(seccion(a.getGrupo(), buscando), k -> new ArrayList<>()).add(a);
            }
        }
        Seccion[] orden = buscando
                ? new Seccion[]{Seccion.TUYO, Seccion.BASICOS, Seccion.PRODUCTOS}
                : new Seccion[]{Seccion.RECIENTES, Seccion.HABITUALES};
        List<Elemento> lista = new ArrayList<>();
        for (Seccion s : orden) {
            List<Alimento> deEsta = porSeccion.get(s);
            if (deEsta == null || deEsta.isEmpty()) continue;
            lista.add(new Elemento(Tipo.CABECERA, s, null));
            for (Alimento a : deEsta) lista.add(new Elemento(Tipo.ALIMENTO, s, a));
        }
        if (buscando) lista.add(new Elemento(Tipo.NO_LO_ENCUENTRAS, null, null));
        return lista;
    }

    public static boolean tieneNoLoEncuentras(@NonNull List<Elemento> lista) {
        for (Elemento e : lista) if (e.tipo == Tipo.NO_LO_ENCUENTRAS) return true;
        return false;
    }

    private static Seccion seccion(@Nullable String grupo, boolean buscando) {
        if ("TUYO".equals(grupo)) return buscando ? Seccion.TUYO : Seccion.RECIENTES;
        if ("PRODUCTO".equals(grupo) && buscando) return Seccion.PRODUCTOS;
        return buscando ? Seccion.BASICOS : Seccion.HABITUALES;
    }
}
