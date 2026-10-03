package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.alimento.Favoritos;
import es.pmdm.gymprofit.model.comida.ComidaReciente;

// ============================================================
// GruposBusqueda — de los resultados de /alimentos/buscar a la lista de la pantalla (1.6.1)
//
// La API devuelve cada alimento con su grupo (TUYO, BASICO o PRODUCTO), ya en orden.
//   · Sin escribir (o con menos de 2 letras), la pestaña «Todo» de Añadir: «Recientes»
//     (TUYO) y «Habituales» (BASICO). Desde la 1.6.3, Recientes enseña seis y, si hay
//     más, su cabecera lleva «Ver todos», que despliega el resto ahí mismo.
//   · Buscando: «Tuyo», «Básicos» y «Productos», en ese orden, y al final
//     «¿No lo encuentras?», también cuando no hay nada.
//   · La pestaña «Favoritos» (1.6.3): arriba la propuesta, si la hay, y «Los que más
//     usas» con cuántos son. Con la propuesta y ningún favorito, debajo de ella el texto
//     de la pestaña vacía (GP-184, lote 1.6.4).
//   · Desde la 1.6.4, en «Todo», «Comidas recientes» entre Recientes y Habituales: hasta
//     tres comidas que se copian enteras con un toque.
// Cada grupo lleva su cabecera solo si tiene algo. Un grupo que no se conoce va con los
// básicos: mejor enseñarlo que perderlo.
// ============================================================
public final class GruposBusqueda {

    /** Menos letras que esto no es una búsqueda: se queda la lista de Añadir. */
    public static final int LETRAS_MINIMAS = 2;
    /** Recientes que se ven sin tocar «Ver todos». */
    public static final int RECIENTES_VISIBLES = 6;

    public enum Seccion { RECIENTES, COMIDAS_RECIENTES, HABITUALES, TUYO, BASICOS, PRODUCTOS, FAVORITOS }

    public enum Tipo { CABECERA, ALIMENTO, NO_LO_ENCUENTRAS, PROPUESTA, COMIDA_RECIENTE, SIN_FAVORITOS }

    /** Una fila de la lista: una cabecera, un alimento, «¿No lo encuentras?» o la propuesta. */
    public static final class Elemento {
        @NonNull public final Tipo tipo;
        @Nullable public final Seccion seccion;
        @Nullable public final Alimento alimento;
        /** En una cabecera: si lleva «Ver todos». */
        public final boolean verTodos;
        /** En la cabecera de favoritos: cuántos son; si no, -1. */
        public final int cuantos;
        /** La propuesta de favorito, en su fila. */
        @Nullable public final Favoritos.Propuesta propuesta;
        /** La comida reciente, en su fila (1.6.4). */
        @Nullable public final ComidaReciente comida;

        private Elemento(@NonNull Tipo tipo, @Nullable Seccion seccion, @Nullable Alimento alimento) {
            this(tipo, seccion, alimento, false, -1, null, null);
        }

        private Elemento(@NonNull Tipo tipo, @Nullable Seccion seccion, @Nullable Alimento alimento,
                         boolean verTodos, int cuantos, @Nullable Favoritos.Propuesta propuesta) {
            this(tipo, seccion, alimento, verTodos, cuantos, propuesta, null);
        }

        private Elemento(@NonNull Tipo tipo, @Nullable Seccion seccion, @Nullable Alimento alimento,
                         boolean verTodos, int cuantos, @Nullable Favoritos.Propuesta propuesta,
                         @Nullable ComidaReciente comida) {
            this.tipo = tipo;
            this.seccion = seccion;
            this.alimento = alimento;
            this.verTodos = verTodos;
            this.cuantos = cuantos;
            this.propuesta = propuesta;
            this.comida = comida;
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

    /** Como {@link #de(List, boolean, boolean)}, con todos los recientes a la vista. */
    @NonNull
    public static List<Elemento> de(@Nullable List<Alimento> alimentos, boolean buscando) {
        return de(alimentos, buscando, true);
    }

    /** Como {@link #de(List, boolean, boolean, List)}, sin comidas recientes. */
    @NonNull
    public static List<Elemento> de(@Nullable List<Alimento> alimentos, boolean buscando, boolean recientesTodos) {
        return de(alimentos, buscando, recientesTodos, null);
    }

    /**
     * @param alimentos       lo que devolvió la búsqueda, en su orden.
     * @param buscando        true con texto (los tres grupos y «¿No lo encuentras?»).
     * @param recientesTodos  sin texto: false enseña seis recientes y «Ver todos».
     * @param comidas         sin texto, las comidas recientes que se pueden copiar (1.6.4).
     */
    @NonNull
    public static List<Elemento> de(@Nullable List<Alimento> alimentos, boolean buscando, boolean recientesTodos,
                                    @Nullable List<ComidaReciente> comidas) {
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
            if (s == Seccion.HABITUALES && comidas != null && !comidas.isEmpty()) {
                lista.add(new Elemento(Tipo.CABECERA, Seccion.COMIDAS_RECIENTES, null));
                for (ComidaReciente c : comidas) {
                    lista.add(new Elemento(Tipo.COMIDA_RECIENTE, Seccion.COMIDAS_RECIENTES, null, false, -1, null, c));
                }
            }
            List<Alimento> deEsta = porSeccion.get(s);
            if (deEsta == null || deEsta.isEmpty()) continue;
            boolean recortar = s == Seccion.RECIENTES && !recientesTodos && deEsta.size() > RECIENTES_VISIBLES;
            lista.add(new Elemento(Tipo.CABECERA, s, null, recortar, -1, null));
            List<Alimento> visibles = recortar ? deEsta.subList(0, RECIENTES_VISIBLES) : deEsta;
            for (Alimento a : visibles) lista.add(new Elemento(Tipo.ALIMENTO, s, a));
        }
        if (buscando) lista.add(new Elemento(Tipo.NO_LO_ENCUENTRAS, null, null));
        return lista;
    }

    /**
     * La pestaña «Favoritos»: la propuesta arriba, si la hay, y los favoritos por uso
     * bajo «Los que más usas», con cuántos son. Sin favoritos, sin cabecera.
     */
    @NonNull
    public static List<Elemento> favoritos(@Nullable List<Alimento> favoritos, @Nullable Favoritos.Propuesta propuesta) {
        List<Elemento> lista = new ArrayList<>();
        if (propuesta != null && propuesta.getAlimento() != null) {
            lista.add(new Elemento(Tipo.PROPUESTA, null, propuesta.getAlimento(), false, -1, propuesta));
        }
        if (favoritos != null && !favoritos.isEmpty()) {
            lista.add(new Elemento(Tipo.CABECERA, Seccion.FAVORITOS, null, false, favoritos.size(), null));
            for (Alimento a : favoritos) lista.add(new Elemento(Tipo.ALIMENTO, Seccion.FAVORITOS, a));
        } else if (!lista.isEmpty()) {
            // Solo la propuesta: debajo, lo que diría la pestaña vacía (GP-184).
            lista.add(new Elemento(Tipo.SIN_FAVORITOS, Seccion.FAVORITOS, null));
        }
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
