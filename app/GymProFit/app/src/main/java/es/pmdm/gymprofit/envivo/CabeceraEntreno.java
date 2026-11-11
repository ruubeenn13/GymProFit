package es.pmdm.gymprofit.envivo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.sesion.VezRutina;
import es.pmdm.gymprofit.utils.CifraTarjeta;
import es.pmdm.gymprofit.utils.Enumeracion;

// ============================================================
// CabeceraEntreno — lo que suma la cabecera de Apuntar entreno al marcar (GP-241, puntos 1
// y 4), en lógica pura para probarla en la JVM.
//
// Solo cuenta lo marcado, que es lo único que se guarda:
//   · los kilos, Σ peso × repeticiones de las series marcadas (GP-011, LogicaSesion.volumen);
//   · las series, «5 de 21» con rutina (las marcadas de las previstas) y solo las marcadas
//     en un entreno libre.
// Una rutina sin ejercicios con peso, ni en su última vez ni en lo marcado, no lleva el
// cuadro de kilos: a peso corporal sería un «0 kg» fijo (DEC-060, cifra en cascada).
//
// Debajo, la última vez de la rutina en cascada: «La última vez moviste 8.410 kg.», «La
// última vez hiciste 12 series.» y, la primera vez, nada. Y el récord, contado como en la
// sesión en vivo (LogicaRecordVivo): uno por ejercicio, la mejor serie marcada que supera
// su mejor marca del mismo tipo.
// ============================================================
public final class CabeceraEntreno {

    /** Los textos, de strings.xml (o puestos a mano en un test). */
    public interface Textos {
        /** «5 series apuntadas y 2.740 kg. Lo que no marques no se guarda.» */
        @NonNull String pieConKilos(int series, @NonNull String kilos);
        /** «12 series apuntadas. Lo que no marques no se guarda.» */
        @NonNull String pieSinKilos(int series);
        /** «Sin series apuntadas: se guardan el día y la duración.» */
        @NonNull String pieVacio();
        /** «La última vez moviste 8.410 kg.» */
        @NonNull String ultimaKilos(@NonNull String kilos);
        /** «La última vez hiciste 12 series.» */
        @NonNull String ultimaSeries(int series);
        /** «La última vez no marcaste ninguna serie.» */
        @NonNull String ultimaSinSeries();
        /** «Récord en press de banca: 72,5 kg × 8» */
        @NonNull String recordUno(@NonNull String ejercicio, @NonNull String marca);
        /** «2 récords: press de banca y remo con barra» */
        @NonNull String recordVarios(int n, @NonNull String ejercicios);
        /** « y », « and »: la conjunción entre dos ejercicios. */
        @NonNull String y();
        /** « y », «, and »: la de tres o más (la coma de serie en inglés). */
        @NonNull String ySerie();
        /** «72,5 kg × 8», «12 repeticiones», «1:15». */
        @NonNull String marca(@NonNull SesionEnCurso.Marca m);
        /** «Ejercicio 3», para uno sin nombre. */
        @NonNull String sinNombre(int posicion);
    }

    /** Un récord de hoy: el ejercicio, su serie y la marca nueva. */
    public static final class RecordHoy {
        @NonNull public final String ejercicio;
        public final long serieId;
        @NonNull public final SesionEnCurso.Marca marca;

        RecordHoy(@NonNull String ejercicio, long serieId, @NonNull SesionEnCurso.Marca marca) {
            this.ejercicio = ejercicio;
            this.serieId = serieId;
            this.marca = marca;
        }
    }

    /** Kilos de lo marcado. */
    public final double kilos;
    /** Series marcadas. */
    public final int marcadas;
    /** Series previstas: las de la rutina, o las que hay en un entreno libre. */
    public final int previstas;
    /** Si hay rutina: entonces las series van «5 de 21». */
    public final boolean conRutina;
    /** Si lleva el cuadro de kilos. */
    public final boolean cuadroKilos;

    private CabeceraEntreno(double kilos, int marcadas, int previstas, boolean conRutina, boolean cuadroKilos) {
        this.kilos = kilos;
        this.marcadas = marcadas;
        this.previstas = previstas;
        this.conRutina = conRutina;
        this.cuadroKilos = cuadroKilos;
    }

    /**
     * Las sumas de la cabecera.
     *
     * @param ejercicios los del borrador.
     * @param conRutina  si se apunta una rutina (false: entreno libre).
     * @param ultima     la última vez de la rutina, o null (primera vez o libre).
     */
    @NonNull
    public static CabeceraEntreno de(@NonNull List<SesionEnCurso.Ejercicio> ejercicios, boolean conRutina,
                                     @Nullable VezRutina ultima) {
        SesionEnCurso s = new SesionEnCurso();
        s.ejercicios = ejercicios;
        LogicaSesion.Totales t = LogicaSesion.totales(s);
        double kilos = LogicaSesion.volumen(s);
        boolean conPeso = kilos > 0 || (ultima != null && ultima.getKilos() > 0) || LogicaSesion.anteriorConPeso(ejercicios);
        return new CabeceraEntreno(kilos, t.seriesHechas, t.seriesTotales, conRutina, conPeso);
    }

    /** Los kilos redondeados, como se escriben: «2.740». */
    @NonNull
    public String kilosTexto(@NonNull Locale idioma) {
        return CifraTarjeta.numero(Math.round(kilos), idioma);
    }

    /** Lo que se va a guardar, en una frase, para el pie. */
    @NonNull
    public String pie(@NonNull Textos t, @NonNull Locale idioma) {
        if (marcadas == 0) return t.pieVacio();
        return Math.round(kilos) > 0 ? t.pieConKilos(marcadas, kilosTexto(idioma)) : t.pieSinKilos(marcadas);
    }

    /**
     * La última vez de la rutina, en cascada (DEC-060).
     *
     * @return null la primera vez (o en un entreno libre): no se dice nada.
     */
    @Nullable
    public static String ultimaVez(@Nullable VezRutina ultima, @NonNull Textos t, @NonNull Locale idioma) {
        if (ultima == null) return null;
        CifraTarjeta c = CifraTarjeta.deVez(ultima);
        switch (c.tipo) {
            case KILOS: return t.ultimaKilos(CifraTarjeta.numero(c.kilos, idioma));
            case SERIES: return t.ultimaSeries(c.series);
            default: return t.ultimaSinSeries();
        }
    }

    /** Los récords de lo marcado, uno por ejercicio como mucho, en el orden de los ejercicios. */
    @NonNull
    public static List<RecordHoy> records(@NonNull List<SesionEnCurso.Ejercicio> ejercicios, @NonNull Textos t) {
        List<RecordHoy> records = new ArrayList<>();
        for (int i = 0; i < ejercicios.size(); i++) {
            SesionEnCurso.Ejercicio e = ejercicios.get(i);
            if (!e.marcaCargada || e.marca == null) continue;
            SesionEnCurso.Serie mejor = null;
            SesionEnCurso.Marca mejorMarca = null;
            for (SesionEnCurso.Serie s : e.series) {
                if (!s.hecha) continue;
                SesionEnCurso.Marca m = LogicaRecordVivo.deSerie(e, s);
                if (m == null || !m.tipo.equals(e.marca.tipo)) continue;
                if (mejorMarca == null || LogicaRecordVivo.supera(m, mejorMarca)) {
                    mejor = s;
                    mejorMarca = m;
                }
            }
            if (mejor != null && LogicaRecordVivo.supera(mejorMarca, e.marca)) {
                records.add(new RecordHoy(e.nombre != null ? e.nombre : t.sinNombre(i + 1), mejor.id, mejorMarca));
            }
        }
        return records;
    }

    /**
     * El récord de la cabecera: «Récord en press de banca: 72,5 kg × 8» o «2 récords: press de
     * banca y remo con barra».
     *
     * @return null sin récords.
     */
    @Nullable
    public static String frase(@NonNull List<RecordHoy> records, @NonNull Textos t, @NonNull Locale idioma) {
        if (records.isEmpty()) return null;
        if (records.size() == 1) {
            RecordHoy r = records.get(0);
            return t.recordUno(Enumeracion.enMedio(r.ejercicio, idioma), t.marca(r.marca));
        }
        List<String> nombres = new ArrayList<>();
        for (RecordHoy r : records) nombres.add(Enumeracion.enMedio(r.ejercicio, idioma));
        return t.recordVarios(records.size(), Enumeracion.unir(nombres, nombres.size() <= 2 ? t.y() : t.ySerie()));
    }
}
