package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.pmdm.gymprofit.model.sesion.SeriesMusculo;
import es.pmdm.gymprofit.model.sesion.VezRutina;

// ============================================================
// TarjetasSesionTest — las líneas de las tarjetas de sesión y de rutina (GP-232)
//
// Todo en frase y sin «·», con las cadenas de verdad de values/ y values-en/: la frase de las
// zonas con una, dos y tres («Pecho, espalda y brazos», «Chest, back, and arms»), minutos,
// series y récords, la comparación con la anterior, las barras y cuándo fue.
// ============================================================
public class TarjetasSesionTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Locale ES = new Locale("es", "ES");
    private static final Locale EN = Locale.US;
    private static final String HOY = "2026-11-04";

    // ── Zonas en frase ─────────────────────────────────────────────────────────

    @Test
    public void lasZonasEnFraseConUnaDosYTresEnEspanol() throws IOException {
        FrasesTarjeta.Textos t = textos("values");
        assertEquals("Pierna", FrasesTarjeta.frase(zonas("values", "pierna"), ES, t));
        assertEquals("Pierna y abdomen", FrasesTarjeta.frase(zonas("values", "pierna", "core"), ES, t));
        assertEquals("Pecho, espalda y brazos",
                FrasesTarjeta.frase(zonas("values", "pecho", "espalda", "brazos"), ES, t));
    }

    @Test
    public void lasZonasEnFraseConUnaDosYTresEnInglesConLaComaDeSerie() throws IOException {
        FrasesTarjeta.Textos t = textos("values-en");
        assertEquals("Legs", FrasesTarjeta.frase(zonas("values-en", "pierna"), EN, t));
        assertEquals("Legs and core", FrasesTarjeta.frase(zonas("values-en", "pierna", "core"), EN, t));
        assertEquals("Chest, back, and arms",
                FrasesTarjeta.frase(zonas("values-en", "pecho", "espalda", "brazos"), EN, t));
    }

    @Test
    public void lasZonasVanDeMasAMenosSeriesYComoMuchoTres() {
        List<Zonas.Zona> z = FrasesTarjeta.zonas(Arrays.asList(
                new SeriesMusculo("pecho", 4), new SeriesMusculo("cuadriceps", 3), new SeriesMusculo("isquiotibiales", 3),
                new SeriesMusculo("biceps", 2), new SeriesMusculo("abdominales", 1), new SeriesMusculo(null, 9)));
        // Pierna suma 6 (cuádriceps e isquios), pecho 4, brazos 2; el abdomen se queda fuera.
        assertEquals(Arrays.asList(Zonas.PIERNA, Zonas.PECHO, Zonas.BRAZOS), z);
    }

    // ── Minutos, series y récords ────────────────────────────────────────────

    @Test
    public void minutosSeriesYRecordsEnFrase() throws IOException {
        FrasesTarjeta.Textos es = textos("values");
        FrasesTarjeta.Linea l = FrasesTarjeta.meta(58, 18, 1, false, es);
        assertEquals("58 minutos, 18 series y 1 récord", sinDuros(l.texto));
        assertEquals("1 récord", sinDuros(l.texto.substring(l.inicioRecord)));
        assertEquals("52 minutos y 21 series", sinDuros(FrasesTarjeta.meta(52, 21, 0, false, es).texto));
        assertEquals(-1, FrasesTarjeta.meta(52, 21, 0, false, es).inicioRecord);
        assertEquals("67 minutos, 21 series y 2 récords", sinDuros(FrasesTarjeta.meta(67, 21, 2, false, es).texto));
        // Sin ejercicios apuntados: solo los minutos.
        assertEquals("25 minutos", sinDuros(FrasesTarjeta.meta(25, 0, 0, true, es).texto));

        FrasesTarjeta.Textos en = textos("values-en");
        assertEquals("58 minutes, 18 sets, and 1 record", sinDuros(FrasesTarjeta.meta(58, 18, 1, false, en).texto));
        assertEquals("1 minute and 1 set", sinDuros(FrasesTarjeta.meta(1, 1, 0, false, en).texto));
    }

    @Test
    public void laPautaDeUnaRutina() throws IOException {
        assertEquals("7 ejercicios y 55 minutos", sinDuros(FrasesTarjeta.pauta(7, 55, textos("values"))));
        assertEquals("1 exercise and 40 minutes", sinDuros(FrasesTarjeta.pauta(1, 40, textos("values-en"))));
    }

    @Test
    public void laUltimaVezConSusRecords() throws IOException {
        FrasesTarjeta.Textos es = textos("values");
        String con = cadena("values", "tarjeta_enlace_con");
        FrasesTarjeta.Linea l = FrasesTarjeta.conRecords("La última vez, hace 6 días", con, 2, es);
        assertEquals("La última vez, hace 6 días, con 2 récords", sinDuros(l.texto));
        assertEquals("2 récords", sinDuros(l.texto.substring(l.inicioRecord)));
        assertEquals("La última vez, ayer", FrasesTarjeta.conRecords("La última vez, ayer", con, 0, es).texto);
    }

    @Test
    public void elCicloDeUnProgramaEnFrase() throws IOException {
        FrasesTarjeta.Textos es = textos("values");
        assertEquals("Cuerpo completo A, B y C", FrasesTarjeta.ciclo(
                Arrays.asList("Cuerpo completo A", "Cuerpo completo B", "Cuerpo completo C"), es));
        assertEquals("Torso A, Pierna A, Torso B y Pierna B", FrasesTarjeta.ciclo(
                Arrays.asList("Torso A", "Pierna A", "Torso B", "Pierna B"), es));
        assertEquals("Empuje, Tirón y Pierna B", FrasesTarjeta.ciclo(Arrays.asList("Empuje", "Tirón", "Pierna B"), es));
        assertEquals("Full body A, B, and C", FrasesTarjeta.ciclo(
                Arrays.asList("Full body A", "Full body B", "Full body C"), textos("values-en")));
    }

    @Test
    public void ningunaLineaDeLasTarjetasLlevaPunto() throws IOException {
        for (String carpeta : new String[]{"values", "values-en"}) {
            String xml = xml(carpeta);
            Matcher m = Pattern.compile("<(string|plurals) name=\"(tarjeta_|historial_semana|historial_borrada|programa_frase|programa_dias_o|programa_minutos_de_a|programa_ciclo_a11y)[^\"]*\"[^>]*>(.*?)</(string|plurals)>",
                    Pattern.DOTALL).matcher(xml);
            int vistas = 0;
            while (m.find()) {
                vistas++;
                assertFalse(carpeta + ": " + m.group(0) + " lleva «·»", m.group(3).contains("·"));
            }
            assertTrue(carpeta + ": no se encontró ninguna cadena de las tarjetas", vistas > 20);
        }
    }

    // ── Comparación con la anterior ─────────────────────────────────────────

    @Test
    public void laComparacionConLaAnteriorDeLaMismaRutina() {
        assertNull("libre: no se compara", ComparacionSesion.de(true, 1850, null));
        assertNull("sin kilos: no se compara", ComparacionSesion.de(false, 0, 1000.0));
        assertNull("la anterior sin kilos: no hay porcentaje", ComparacionSesion.de(false, 1000, 0.0));
        assertEquals(ComparacionSesion.Tipo.PRIMERA, ComparacionSesion.de(false, 7380, null).tipo);
        ComparacionSesion sube = ComparacionSesion.de(false, 13640, 13250.0);
        assertEquals(ComparacionSesion.Tipo.SUBE, sube.tipo);
        assertEquals(3, sube.porcentaje);
        ComparacionSesion baja = ComparacionSesion.de(false, 11980, 12230.0);
        assertEquals(ComparacionSesion.Tipo.BAJA, baja.tipo);
        assertEquals(2, baja.porcentaje);
        assertEquals("redondea a 0 %: igual", ComparacionSesion.Tipo.IGUAL, ComparacionSesion.de(false, 10010, 10000.0).tipo);
    }

    // ── Barras ───────────────────────────────────────────────────────────────

    @Test
    public void lasBarrasEmpiezanEnCeroYLaMasAltaLlena() {
        float[] a = BarrasVeces.alturas(veces(11020, 13640, 12180));
        assertEquals(11020 / 13640f, a[0], 1e-4);
        assertEquals(1f, a[1], 1e-6);
        assertEquals(0f, BarrasVeces.alturas(veces(0, 0))[0], 0f);
    }

    @Test
    public void laDeEstaSesionVaEnNaranjaYLaDelRecordEnDorado() {
        List<VezRutina> v = new ArrayList<>();
        v.add(new VezRutina(1, null, 100, false));
        v.add(new VezRutina(2, null, 110, true));
        v.add(new VezRutina(3, null, 120, false));
        assertArrayEquals(new BarrasVeces.Color[]{BarrasVeces.Color.RESTO, BarrasVeces.Color.ORO, BarrasVeces.Color.ESTA},
                BarrasVeces.colores(v));
        v.add(new VezRutina(4, null, 130, true));
        assertEquals("si esta batió un récord, va en dorado", BarrasVeces.Color.ORO, BarrasVeces.colores(v)[3]);
        assertEquals(BarrasVeces.Color.RESTO, BarrasVeces.colores(v)[2]);
    }

    @Test
    public void comoMuchoSeisBarras() {
        List<VezRutina> siete = veces(1, 2, 3, 4, 5, 6, 7);
        List<VezRutina> seis = BarrasVeces.recortar(siete);
        assertEquals(6, seis.size());
        assertEquals(2, seis.get(0).getKilos(), 0);
        assertEquals(7, seis.get(5).getKilos(), 0);
    }

    // ── Cuándo ───────────────────────────────────────────────────────────────

    @Test
    public void cuandoEnElHistorialCompleto() throws IOException {
        CuandoTarjeta.Textos es = cuando("values");
        assertEquals("Hoy, 09:30", CuandoTarjeta.historial("2026-11-04T09:30:00", HOY, ES, es));
        assertEquals("Ayer, 18:05", CuandoTarjeta.historial("2026-11-03T18:05:00", HOY, ES, es));
        assertEquals("Lunes 2, 19:10", CuandoTarjeta.historial("2026-11-02T19:10:00", HOY, ES, es));
        // Sin el mes, como en español: lo da la cabecera de la semana.
        assertEquals("Monday 2, 7:10 PM", CuandoTarjeta.historial("2026-11-02T19:10:00", HOY, EN, cuando("values-en")));
    }

    @Test
    public void cuandoParaTalkBack() throws IOException {
        CuandoTarjeta.Textos es = cuando("values");
        assertEquals("ayer a las 18:05", CuandoTarjeta.a11y("2026-11-03T18:05:00", HOY, ES, es));
        assertEquals("el lunes 2 a las 19:10", CuandoTarjeta.a11y("2026-11-02T19:10:00", HOY, ES, es));
        assertEquals("on Monday 2 at 7:10 PM", CuandoTarjeta.a11y("2026-11-02T19:10:00", HOY, EN, cuando("values-en")));
    }

    @Test
    public void elDiaRelativoDeInicioYProgresoSinPunto() throws IOException {
        assertEquals("Ayer, martes", CuandoTarjeta.relativo("2026-11-03T18:05:00", HOY, ES, cuando("values")));
        assertEquals("Yesterday, Tuesday", CuandoTarjeta.relativo("2026-11-03T18:05:00", HOY, EN, cuando("values-en")));
    }

    @Test
    public void laCabeceraDeLaSemana() throws IOException {
        CuandoTarjeta.Textos es = cuando("values");
        assertEquals("ESTA SEMANA", CuandoTarjeta.semana("2026-11-02", HOY, ES, es));
        assertEquals("SEMANA PASADA", CuandoTarjeta.semana("2026-10-26", HOY, ES, es));
        assertEquals("SEMANA DEL 19 OCT", CuandoTarjeta.semana("2026-10-19", HOY, ES, es));
        assertEquals("WEEK OF OCT 19", CuandoTarjeta.semana("2026-10-19", HOY, EN, cuando("values-en")));
    }

    @Test
    public void laCabeceraDeLaSemanaParaTalkBackVaEnFraseConElMesBienEscrito() throws IOException {
        // El mes entero: abreviado, el sintetizador lo deletrea o lo lee a medias («sept»).
        assertEquals("Week of October 19", CuandoTarjeta.semanaEnFrase("2026-10-19", HOY, EN, cuando("values-en")));
        assertEquals("Semana del 19 de octubre", CuandoTarjeta.semanaEnFrase("2026-10-19", HOY, ES, cuando("values")));
        assertEquals("Esta semana", CuandoTarjeta.semanaEnFrase("2026-11-02", HOY, ES, cuando("values")));
    }

    @Test
    public void laCabeceraDeUnaSemanaSinKilosSoloCuentaLosEntrenos() throws IOException {
        CifraTarjeta.Textos es = cifra("values"), en = cifra("values-en");
        assertEquals("2 entrenos y 22.050 kg", sinDuros(CifraTarjeta.semana(2, 22050, es)));
        assertEquals("3 entrenos", sinDuros(CifraTarjeta.semana(3, 0, es)));
        assertEquals("1 entreno", sinDuros(CifraTarjeta.semana(1, 0, es)));
        assertEquals("2 workouts and 22,050 kg", sinDuros(CifraTarjeta.semana(2, 22050, en)));
        assertEquals("3 workouts", sinDuros(CifraTarjeta.semana(3, 0.3, en)));
        // A TalkBack, igual: nunca «0 kilos».
        assertEquals("2 entrenos y 22.050 kilos", sinDuros(CifraTarjeta.semanaA11y(2, 22050, es)));
        assertEquals("3 entrenos", sinDuros(CifraTarjeta.semanaA11y(3, 0, es)));
        assertEquals("1 workout", sinDuros(CifraTarjeta.semanaA11y(1, 0, en)));
    }

    // ── La cifra grande: kilos, series o aviso ────────────────────────────────

    @Test
    public void conKilosLaCifraSonLosKilosConSusBarrasYLaComparacion() throws IOException {
        CifraTarjeta c = CifraTarjeta.deSesion(13640, 18, 5);
        assertEquals(CifraTarjeta.Tipo.KILOS, c.tipo);
        assertTrue(c.conKilos());
        assertFalse(c.esAviso());
        assertNull(c.texto(cifra("values")));
        assertEquals("13.640 kilos", sinDuros(c.a11y(cifra("values"))));
        assertEquals("13,640 kilos", sinDuros(c.a11y(cifra("values-en"))));
    }

    @Test
    public void sinKilosYConSeriesLaCifraSonLasSeriesEnEspanolYEnIngles() throws IOException {
        CifraTarjeta tres = CifraTarjeta.deSesion(0, 3, 2);
        assertEquals(CifraTarjeta.Tipo.SERIES, tres.tipo);
        assertFalse(tres.conKilos());
        assertFalse(tres.esAviso());
        assertEquals("3 series", sinDuros(tres.texto(cifra("values"))));
        assertEquals("3 sets", sinDuros(tres.texto(cifra("values-en"))));
        assertEquals("3 series", sinDuros(tres.a11y(cifra("values"))));
        CifraTarjeta una = CifraTarjeta.deSesion(0, 1, 1);
        assertEquals("1 serie", sinDuros(una.texto(cifra("values"))));
        assertEquals("1 set", sinDuros(una.a11y(cifra("values-en"))));
        // Medio kilo redondea a 0: no son kilos.
        assertEquals(CifraTarjeta.Tipo.SERIES, CifraTarjeta.deSesion(0.4, 2, 1).tipo);
    }

    @Test
    public void sinSeriesElAvisoDistingueSinEjerciciosDeSinSeries() throws IOException {
        CifraTarjeta sinEjercicios = CifraTarjeta.deSesion(0, 0, 0);
        assertEquals(CifraTarjeta.Tipo.SIN_EJERCICIOS, sinEjercicios.tipo);
        assertTrue(sinEjercicios.esAviso());
        assertEquals("Sin ejercicios apuntados", sinEjercicios.texto(cifra("values")));
        assertEquals("No exercises logged", sinEjercicios.a11y(cifra("values-en")));
        CifraTarjeta sinSeries = CifraTarjeta.deSesion(0, 0, 2);
        assertEquals(CifraTarjeta.Tipo.SIN_SERIES, sinSeries.tipo);
        assertTrue(sinSeries.esAviso());
        assertFalse(sinSeries.conKilos());
        assertEquals("Sin series hechas", sinSeries.texto(cifra("values")));
        assertEquals("No sets done", sinSeries.texto(cifra("values-en")));
        // Dentro de una frase («Hoy, sin series hechas»), el aviso va en minúscula; las cifras no cambian.
        assertEquals("sin series hechas", sinSeries.enFrase(cifra("values")));
        assertEquals("no exercises logged", sinEjercicios.enFrase(cifra("values-en")));
        assertEquals("3 series", sinDuros(CifraTarjeta.deSesion(0, 3, 1).enFrase(cifra("values"))));
    }

    @Test
    public void laUltimaVezDeUnaRutinaSigueLaMismaCascada() throws IOException {
        assertEquals(CifraTarjeta.Tipo.KILOS, CifraTarjeta.deVez(new VezRutina(1, null, 850, false, 4)).tipo);
        CifraTarjeta series = CifraTarjeta.deVez(new VezRutina(2, null, 0, true, 3));
        assertEquals(CifraTarjeta.Tipo.SERIES, series.tipo);
        assertEquals("3 sets", sinDuros(series.texto(cifra("values-en"))));
        // De una vez no se sabe si tenía ejercicios: «Sin series hechas» vale en los dos casos.
        assertEquals(CifraTarjeta.Tipo.SIN_SERIES, CifraTarjeta.deVez(new VezRutina(3, null, 0, false, 0)).tipo);
    }

    // ── Ayudas: las cadenas de verdad ───────────────────────────────────────

    private static List<VezRutina> veces(double... kilos) {
        List<VezRutina> l = new ArrayList<>();
        for (int i = 0; i < kilos.length; i++) l.add(new VezRutina(i + 1, null, kilos[i], false));
        return l;
    }

    private static List<String> zonas(String carpeta, String... claves) throws IOException {
        List<String> l = new ArrayList<>();
        for (String c : claves) l.add(cadena(carpeta, "zona_" + c));
        return l;
    }

    private static FrasesTarjeta.Textos textos(String carpeta) throws IOException {
        return new FrasesTarjeta.Textos(cadena(carpeta, "enumeracion_y"), cadena(carpeta, "tarjeta_y_serie"),
                plural(carpeta, "tarjeta_minutos"), plural(carpeta, "tarjeta_series"),
                plural(carpeta, "tarjeta_records"), plural(carpeta, "tarjeta_ejercicios"));
    }

    private static CifraTarjeta.Textos cifra(String carpeta) throws IOException {
        Locale idioma = carpeta.endsWith("-en") ? EN : ES;
        return new CifraTarjeta.Textos(idioma, plural(carpeta, "tarjeta_series"), pluralCon(carpeta, "tarjeta_kilos_a11y", false),
                cadena(carpeta, "tarjeta_sin_ejercicios"), cadena(carpeta, "tarjeta_sin_series"),
                pluralCon(carpeta, "historial_semana_resumen", true), pluralCon(carpeta, "historial_semana_a11y", true),
                plural(carpeta, "historial_semana_sin_kilos"));
    }

    // Un plural con la cifra ya escrita: «%1$s kilos» (conN false) o «%1$d entrenos y %2$s kg».
    private static CifraTarjeta.ContarCon pluralCon(String carpeta, String nombre, boolean conN) throws IOException {
        Matcher m = Pattern.compile("<plurals name=\"" + nombre + "\">(.*?)</plurals>", Pattern.DOTALL).matcher(xml(carpeta));
        if (!m.find()) throw new AssertionError(carpeta + ": falta el plural " + nombre);
        String uno = item(m.group(1), "one"), otro = item(m.group(1), "other");
        return (n, c) -> conN ? String.format(Locale.ROOT, n == 1 ? uno : otro, n, c)
                : String.format(Locale.ROOT, n == 1 ? uno : otro, c);
    }

    private static CuandoTarjeta.Textos cuando(String carpeta) throws IOException {
        return new CuandoTarjeta.Textos(cadena(carpeta, "tiempo_hoy_mayuscula"), cadena(carpeta, "tiempo_ayer_mayuscula"),
                cadena(carpeta, "tarjeta_cuando"), cadena(carpeta, "tarjeta_dia_patron"),
                cadena(carpeta, "tarjeta_hora_patron"), cadena(carpeta, "tarjeta_cuando_hoy_a11y"),
                cadena(carpeta, "tarjeta_cuando_ayer_a11y"), cadena(carpeta, "tarjeta_cuando_otro_a11y"),
                cadena(carpeta, "historial_esta_semana"), cadena(carpeta, "historial_semana_pasada"),
                cadena(carpeta, "historial_semana_del"), cadena(carpeta, "inicio_fecha_corta_patron"),
                new FechaUtils.TextosDia(cadena(carpeta, "tiempo_hoy_mayuscula"), cadena(carpeta, "tiempo_ayer_mayuscula"),
                        cadena(carpeta, "tarjeta_cuando"), cadena(carpeta, "inicio_dia_pasado"),
                        cadena(carpeta, "inicio_fecha_corta_patron")),
                cadena(carpeta, "historial_semana_patron_a11y"));
    }

    private static String sinDuros(String s) {
        return s.replace(' ', ' ');
    }

    private static String xml(String carpeta) throws IOException {
        return new String(Files.readAllBytes(RES.resolve(carpeta).resolve("strings.xml")), StandardCharsets.UTF_8);
    }

    private static String cadena(String carpeta, String nombre) throws IOException {
        Matcher m = Pattern.compile("<string name=\"" + nombre + "\"[^>]*>(.*?)</string>", Pattern.DOTALL)
                .matcher(xml(carpeta));
        if (!m.find()) throw new AssertionError(carpeta + ": falta " + nombre);
        return android(m.group(1));
    }

    // Un plural de inglés o español: «one» con 1 y «other» con lo demás.
    private static FrasesTarjeta.Contar plural(String carpeta, String nombre) throws IOException {
        Matcher m = Pattern.compile("<plurals name=\"" + nombre + "\">(.*?)</plurals>", Pattern.DOTALL).matcher(xml(carpeta));
        if (!m.find()) throw new AssertionError(carpeta + ": falta el plural " + nombre);
        String cuerpo = m.group(1);
        String uno = item(cuerpo, "one"), otro = item(cuerpo, "other");
        return n -> String.format(Locale.ROOT, n == 1 ? uno : otro, n);
    }

    private static String item(String cuerpo, String cantidad) {
        Matcher m = Pattern.compile("<item quantity=\"" + cantidad + "\">(.*?)</item>").matcher(cuerpo);
        if (!m.find()) throw new AssertionError("falta el plural " + cantidad);
        return android(m.group(1));
    }

    // Lo que hace aapt con el texto de un recurso: comillas que guardan espacios,   y \'.
    private static String android(String s) {
        s = s.replace("\\u00A0", " ").replace("\\'", "'");
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
        return s;
    }
}
