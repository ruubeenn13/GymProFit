package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.pmdm.gymprofit.model.alimento.Alimento;

// ============================================================
// LoQueTeEncaja — «Lo que te encaja» de Añadir (lote 1.6.7, GP-165, decisión 7 del lienzo)
//
// Sin IA y en el móvil, con lo que Añadir ya tiene cargado: de tus platos, favoritos y
// recientes, como mucho tres alimentos o platos sueltos que caben en las kcal que te
// quedan hoy. Nunca comidas combinadas (decisión del propietario del 14-10).
//   · Cada candidato va con lo que añadiría su «+» (AnadirRapido: la última cantidad que
//     usaste, o lo que propondría la ficha). Sin escalar: si con esa cantidad no cabe en
//     las kcal que quedan, no sale. Lo que no suma kcal, tampoco.
//   · Delante, lo que sueles tomar en esa comida (por el tipo, no por la hora): lo
//     habitual en ella (/comidas/habituales) y lo que llevó ayer.
//   · Dentro de cada bloque, lo que más cubre la proteína que falta (pasarse no cubre
//     más que llegar justo); con la misma proteína, el de menos kcal. Si ya no falta
//     proteína (menos de medio gramo), el que mejor encaja en las kcal: el más cercano a
//     lo que queda. Con todo igual, el orden de uso: platos, favoritos y recientes.
//   · El motivo: «Lo sueles tomar en…», «Tu … de ayer», «Mucha proteína para lo que te
//     falta» o, si nada de eso, «Cabe en lo que te queda». «Mucha proteína» se la lleva
//     uno solo: el que más proteína cubre de los que salen, si cubre más que todos los
//     demás (1.6.8; antes era desde el 25 % de la que falta, y con mucha pendiente lo
//     decían todos o ninguno).
//   · Lo que ya está apuntado en esa comida ese día no se propone (1.6.8).
// Sin kcal que queden o sin candidatos, no hay sección (null): no ocupa sitio.
// ============================================================
public final class LoQueTeEncaja {

    /** Cuántos como mucho. */
    public static final int MAXIMO = 3;
    /** Por debajo de esto ya no falta proteína (como en la ficha). */
    private static final double SIN_PROTEINA = 0.5;

    /** Por qué sale: la línea de debajo de cada uno. */
    public enum Motivo { SUELES, AYER, PROTEINA, CABE }

    /** Uno de los que encajan: el alimento, lo que añade su «+» y por qué sale. */
    public static final class Sugerencia {
        @NonNull public final Alimento alimento;
        @NonNull public final CantidadFicha cantidad;
        public final long kcal;
        public final double proteina;
        @NonNull public final Motivo motivo;
        /** Qué parte de la proteína que falta cubre, en % y sin tope; -1 si no falta. */
        public final int cubre;

        Sugerencia(@NonNull Alimento alimento, @NonNull CantidadFicha cantidad, long kcal, double proteina,
                   @NonNull Motivo motivo, int cubre) {
            this.alimento = alimento;
            this.cantidad = cantidad;
            this.kcal = kcal;
            this.proteina = proteina;
            this.motivo = motivo;
            this.cubre = cubre;
        }
    }

    /** La sección: lo que queda del día, para la cabecera, y los que encajan, en su orden. */
    public static final class Resultado {
        public final int kcalQuedan;
        /** Gramos de proteína que faltan, redondeados; 0 si ya no falta. */
        public final int proteinaQuedan;
        @NonNull public final List<Sugerencia> sugerencias;

        Resultado(int kcalQuedan, int proteinaQuedan, @NonNull List<Sugerencia> sugerencias) {
            this.kcalQuedan = kcalQuedan;
            this.proteinaQuedan = proteinaQuedan;
            this.sugerencias = Collections.unmodifiableList(sugerencias);
        }

        /** ¿Falta proteína? Sin ella, ni la cabecera la nombra ni hay barras. */
        public boolean conProteina() {
            return proteinaQuedan > 0;
        }
    }

    private LoQueTeEncaja() {
    }

    // Un candidato mientras se ordena: su bloque y su posición en el orden de uso.
    private static final class Candidato {
        final Alimento alimento;
        final CantidadFicha cantidad;
        final long kcal;
        final double proteina;
        final boolean sueles;
        final int uso;

        Candidato(Alimento alimento, CantidadFicha cantidad, long kcal, double proteina, boolean sueles, int uso) {
            this.alimento = alimento;
            this.cantidad = cantidad;
            this.kcal = kcal;
            this.proteina = proteina;
            this.sueles = sueles;
            this.uso = uso;
        }
    }

    /**
     * Lo que te encaja.
     *
     * @param kcalQuedan     kcal que quedan hoy hasta el objetivo (objetivo − lo apuntado).
     * @param proteinaQuedan gramos de proteína que faltan hoy; ≤ 0 si ya está.
     * @param platos         tus platos, del más usado al menos.
     * @param favoritos      tus favoritos, en su orden.
     * @param recientes      lo que has usado últimamente, en su orden.
     * @param habituales     ids de lo habitual en la comida a la que se añade.
     * @param deAyer         ids de lo que llevó ayer esa misma comida.
     * @param yaApuntados    ids de lo que ya lleva esa comida ese día: no se proponen.
     * @return null si no hay sección: sin kcal que queden o sin nada que quepa.
     */
    @Nullable
    public static Resultado de(int kcalQuedan, double proteinaQuedan, @Nullable List<Alimento> platos,
                               @Nullable List<Alimento> favoritos, @Nullable List<Alimento> recientes,
                               @Nullable Set<Integer> habituales, @Nullable Set<Integer> deAyer,
                               @Nullable Set<Integer> yaApuntados) {
        // Sin kcal que queden no hay nada que quepa (lo que no suma kcal no sale): null.
        double falta = proteinaQuedan >= SIN_PROTEINA ? proteinaQuedan : 0;
        Set<Integer> suele = habituales != null ? habituales : Collections.emptySet();
        Set<Integer> ayer = deAyer != null ? deAyer : Collections.emptySet();
        Set<Integer> yaEsta = yaApuntados != null ? yaApuntados : Collections.emptySet();

        List<Candidato> candidatos = new ArrayList<>();
        Set<Integer> vistos = new HashSet<>();
        List<List<Alimento>> fuentes = new ArrayList<>();
        fuentes.add(platos);
        fuentes.add(favoritos);
        fuentes.add(recientes);
        for (List<Alimento> fuente : fuentes) {
            if (fuente == null) continue;
            for (Alimento a : fuente) {
                if (a == null || a.esExterno() || yaEsta.contains(a.getId()) || !vistos.add(a.getId())) continue;
                CantidadFicha c = AnadirRapido.cantidad(a);
                long kcal = AnadirRapido.kcal(a, c);
                if (kcal <= 0 || kcal > kcalQuedan) continue;
                double proteina = a.getProteinas() * c.gramos() / 100.0;
                boolean sueles = suele.contains(a.getId()) || ayer.contains(a.getId());
                candidatos.add(new Candidato(a, c, kcal, proteina, sueles, candidatos.size()));
            }
        }
        if (candidatos.isEmpty()) return null;

        candidatos.sort(orden(falta, kcalQuedan));
        List<Candidato> salen = candidatos.subList(0, Math.min(MAXIMO, candidatos.size()));
        Candidato masProteico = falta > 0 ? masProteico(salen, falta) : null;
        List<Sugerencia> lista = new ArrayList<>();
        for (Candidato c : salen) {
            int cubre = falta > 0 ? (int) Math.round(c.proteina * 100.0 / falta) : -1;
            lista.add(new Sugerencia(c.alimento, c.cantidad, c.kcal, c.proteina,
                    motivo(c.alimento.getId(), suele, ayer, c == masProteico), cubre));
        }
        return new Resultado(kcalQuedan, (int) Math.round(falta), lista);
    }

    // Lo que sueles delante; después la proteína que cubre (o, sin ella pendiente, las kcal
    // que mejor encajan); y al final el orden de uso, que ya traen las listas.
    private static Comparator<Candidato> orden(double falta, int kcalQuedan) {
        Comparator<Candidato> bloque = Comparator.comparing(c -> !c.sueles);
        Comparator<Candidato> segun = falta > 0
                // A décimas de gramo, para que dos cifras iguales en pantalla empaten.
                ? Comparator.<Candidato>comparingLong(c -> -cubre(c, falta))
                .thenComparingLong(c -> c.kcal)
                : Comparator.comparingLong(c -> kcalQuedan - c.kcal);
        return bloque.thenComparing(segun).thenComparingInt(c -> c.uso);
    }

    // El que cubre más proteína que cada uno de los demás que salen, a décimas de gramo y
    // sin contar lo que pasa de la que falta; null si sale uno solo o hay empate arriba.
    @Nullable
    private static Candidato masProteico(List<Candidato> salen, double falta) {
        Candidato mejor = null;
        long mejorCubre = 0;
        boolean empate = false;
        for (Candidato c : salen) {
            long cubre = cubre(c, falta);
            if (cubre > mejorCubre) {
                mejor = c;
                mejorCubre = cubre;
                empate = false;
            } else if (cubre == mejorCubre && mejor != null) {
                empate = true;
            }
        }
        return salen.size() > 1 && !empate ? mejor : null;
    }

    private static long cubre(Candidato c, double falta) {
        return Math.round(Math.min(c.proteina, falta) * 10);
    }

    private static Motivo motivo(int id, Set<Integer> suele, Set<Integer> ayer, boolean masProteico) {
        if (suele.contains(id)) return Motivo.SUELES;
        if (ayer.contains(id)) return Motivo.AYER;
        if (masProteico) return Motivo.PROTEINA;
        return Motivo.CABE;
    }
}
