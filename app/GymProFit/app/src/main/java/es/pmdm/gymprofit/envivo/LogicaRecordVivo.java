package es.pmdm.gymprofit.envivo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.utils.Numeros;

// ============================================================
// LogicaRecordVivo — el récord en mitad de la serie (GP-210 paso 3, momento 25).
//
// La misma regla que CalculadoraRecords en la API (GP-088), para que lo que se celebra
// al marcar sea lo que luego sale en el resumen:
//   · Con peso, la marca es la serie más pesada y, a igual peso, la de más repeticiones.
//     Sin peso, las repeticiones; por tiempo, los segundos.
//   · Igualar una marca no es récord; superarla, sí.
//   · La primera marca de un ejercicio no es un récord: es el punto de partida. Sin
//     historial no se celebra nada.
//   · Con peso, sin peso y por tiempo son marcas distintas: no compiten entre sí.
// Y la de la decisión 4 de la firma visual: una sola celebración por ejercicio y sesión.
// Que Ejercicio.recordSerieId esté puesto ya es haberla celebrado, y sobrevive a matar la
// app porque va en el fichero de la sesión.
//
// Lógica pura, sin Android: se prueba con LogicaRecordVivoTest.
// ============================================================
public final class LogicaRecordVivo {

    private LogicaRecordVivo() { }

    /** Si {@code nueva} supera a {@code previa}: igualar no cuenta, y distinto tipo no compite. */
    public static boolean supera(@NonNull SesionEnCurso.Marca nueva, @NonNull SesionEnCurso.Marca previa) {
        if (nueva.tipo == null || !nueva.tipo.equals(previa.tipo)) return false;
        if (Record.TIPO_REPETICIONES.equals(nueva.tipo)) return nueva.repeticiones > previa.repeticiones;
        if (Record.TIPO_TIEMPO.equals(nueva.tipo)) return nueva.segundos > previa.segundos;
        BigDecimal a = kilos(nueva), b = kilos(previa);
        int porPeso = a.compareTo(b);
        return porPeso > 0 || (porPeso == 0 && nueva.repeticiones > previa.repeticiones);
    }

    private static BigDecimal kilos(SesionEnCurso.Marca m) {
        try {
            return m.peso == null ? BigDecimal.ZERO : new BigDecimal(m.peso);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * La marca de una serie tal como está escrita, o null si no es una marca todavía
     * (sin repeticiones, sin segundos o con algo que no es un número).
     */
    @Nullable
    public static SesionEnCurso.Marca deSerie(@NonNull SesionEnCurso.Ejercicio e, @NonNull SesionEnCurso.Serie s) {
        if (LogicaSesion.porTiempo(e)) {
            Integer segundos = LogicaSesion.segundosDe(s.segundos);
            return segundos != null && segundos > 0 ? new SesionEnCurso.Marca(Record.TIPO_TIEMPO, null, 0, segundos) : null;
        }
        Integer reps = Numeros.entero(s.repeticiones, 1, 100);
        if (reps == null) return null;
        String texto = s.peso == null ? "" : s.peso.trim();
        if (texto.isEmpty()) return new SesionEnCurso.Marca(Record.TIPO_REPETICIONES, null, reps, 0);
        BigDecimal peso = Numeros.exacto(texto, 0, 500);
        if (peso == null) return null;
        if (peso.signum() == 0) return new SesionEnCurso.Marca(Record.TIPO_REPETICIONES, null, reps, 0);
        return new SesionEnCurso.Marca(Record.TIPO_PESO, peso.stripTrailingZeros().toPlainString(), reps, 0);
    }

    /**
     * La serie marcada acaba de batir la mejor marca del ejercicio: devuelve la marca nueva
     * y deja puesto que ya se celebró. Null si no es récord o si este ejercicio ya celebró
     * el suyo en esta sesión.
     */
    @Nullable
    public static SesionEnCurso.Marca alMarcar(@NonNull SesionEnCurso.Ejercicio e, long serieId) {
        if (e.recordSerieId != null || !e.marcaCargada || e.marca == null) return null;
        for (SesionEnCurso.Serie s : e.series) {
            if (s.id != serieId || !s.hecha) continue;
            SesionEnCurso.Marca m = deSerie(e, s);
            if (m == null || !supera(m, e.marca)) return null;
            e.recordSerieId = serieId;
            return m;
        }
        return null;
    }

    // La mejor serie hecha hoy del tipo dado.
    @Nullable
    private static SesionEnCurso.Marca mejorDeHoy(SesionEnCurso.Ejercicio e, String tipo) {
        SesionEnCurso.Marca mejor = null;
        for (SesionEnCurso.Serie s : e.series) {
            if (!s.hecha) continue;
            SesionEnCurso.Marca m = deSerie(e, s);
            if (m == null || !tipo.equals(m.tipo)) continue;
            if (mejor == null || supera(m, mejor)) mejor = m;
        }
        return mejor;
    }

    /**
     * La marca que enseña «Tu récord»: la de siempre, o la de hoy si ya la ha superado.
     * Null si el ejercicio no tiene historial («Primera vez: es tu punto de partida»).
     */
    @Nullable
    public static SesionEnCurso.Marca mostrada(@NonNull SesionEnCurso.Ejercicio e) {
        if (e.marca == null) return null;
        SesionEnCurso.Marca hoy = mejorDeHoy(e, e.marca.tipo);
        return hoy != null && supera(hoy, e.marca) ? hoy : e.marca;
    }

    /** Si lo que enseña «Tu récord» es de hoy («Tu récord: 85 kg × 7, de hoy»). */
    public static boolean esDeHoy(@NonNull SesionEnCurso.Ejercicio e) {
        SesionEnCurso.Marca m = mostrada(e);
        return m != null && m != e.marca;
    }

    /**
     * Ids de los ejercicios cuya mejor marca falta por preguntar: los que ya se sabe que
     * tuvieron alguna vez (hay «Anterior»). Quien no tiene historial no tiene marca, y se
     * da por sabido aquí mismo; quien no sabe aún su última vez espera.
     */
    @NonNull
    public static List<Integer> sinMarca(@NonNull SesionEnCurso s) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (SesionEnCurso.Ejercicio e : s.ejercicios) {
            if (e.marcaCargada || !e.anteriorCargado) continue;
            if (e.anterior.isEmpty()) e.marcaCargada = true;
            else ids.add(e.ejercicioId);
        }
        return new ArrayList<>(ids);
    }

    /**
     * Pone lo que contestó la API en los ejercicios pedidos. Los que no vienen no tienen
     * marca. Una respuesta nula no cuenta como saber: se vuelve a preguntar.
     */
    public static void aplicarMarcas(@NonNull SesionEnCurso s, @NonNull List<Integer> pedidos,
                                     @Nullable Map<Integer, SesionEnCurso.Marca> marcas) {
        if (marcas == null) return;
        for (SesionEnCurso.Ejercicio e : s.ejercicios) {
            if (e.marcaCargada || !pedidos.contains(e.ejercicioId)) continue;
            e.marca = marcas.get(e.ejercicioId);
            e.marcaCargada = true;
        }
    }
}
