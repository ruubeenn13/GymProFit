package es.pmdm.gymprofit.envivo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;

// ============================================================
// AlmacenSesionTest — el fichero de la sesión en curso (GP-012): de ida y vuelta con
// todo lo necesario para seguir, uno por cuenta, y un fichero roto o de otra versión
// no rompe nada.
// ============================================================
public class AlmacenSesionTest {

    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static SesionEnCurso sesion(int usuarioId) {
        SesionEnCurso s = new SesionEnCurso();
        s.usuarioId = usuarioId;
        s.rutinaId = 31;
        s.rutinaNombre = "Pierna B";
        s.programaNombre = "Torso y pierna";
        s.inicioMs = 1_790_000_000_000L;
        s.siguienteId = 9;

        SesionEnCurso.Ejercicio e = new SesionEnCurso.Ejercicio();
        e.id = 1;
        e.ejercicioId = 12;
        e.nombre = "Plancha";
        e.medida = "SEGUNDOS";
        e.minimo = 30;
        e.maximo = 60;
        e.porLado = "LADO";
        e.deRutina = true;
        e.anteriorCargado = true;
        e.anterior.add(new SesionEnCurso.SerieAnterior(1, null, 0, 40));
        SesionEnCurso.Serie serie = new SesionEnCurso.Serie();
        serie.id = 2;
        serie.segundos = "0:4";  // a medio escribir
        serie.hecha = true;
        e.series.add(serie);
        s.ejercicios.add(e);

        SesionEnCurso.Cronometro c = new SesionEnCurso.Cronometro();
        c.serieId = 2;
        c.inicioMs = 1_790_000_100_000L;
        c.avisoMinimo = true;
        s.cronometro = c;

        s.guardado = SesionEnCurso.Guardado.FALLO;
        s.claveIdempotencia = "clave";
        s.duracionEnviada = 43;
        return s;
    }

    @Test
    public void de_ida_y_vuelta_con_todo_lo_necesario_para_seguir() throws Exception {
        AlmacenSesion almacen = new AlmacenSesion(tmp.newFolder("sesion_en_curso"));
        assertTrue(almacen.escribir(sesion(7)));

        SesionEnCurso leida = almacen.leer(7);

        assertNotNull(leida);
        assertEquals(SesionEnCurso.FORMATO, leida.formato);
        assertEquals(Integer.valueOf(31), leida.rutinaId);
        assertEquals("Pierna B", leida.rutinaNombre);
        assertEquals("Torso y pierna", leida.programaNombre);
        assertEquals(1_790_000_000_000L, leida.inicioMs);
        assertEquals(9, leida.siguienteId);
        SesionEnCurso.Ejercicio e = leida.ejercicios.get(0);
        assertEquals("SEGUNDOS", e.medida);
        assertEquals(Integer.valueOf(60), e.maximo);
        assertEquals("LADO", e.porLado);
        assertEquals("tal como se escribió", "0:4", e.series.get(0).segundos);
        assertTrue(e.series.get(0).hecha);
        assertEquals(Integer.valueOf(40), e.anterior.get(0).segundos);
        assertEquals(1_790_000_100_000L, leida.cronometro.inicioMs);
        assertTrue(leida.cronometro.avisoMinimo);
        assertEquals(SesionEnCurso.Guardado.FALLO, leida.guardado);
        assertEquals("clave", leida.claveIdempotencia);
        assertEquals(Integer.valueOf(43), leida.duracionEnviada);
    }

    @Test
    public void escribir_encima_sustituye_y_no_deja_temporales() throws Exception {
        File carpeta = tmp.newFolder("sesion_en_curso");
        AlmacenSesion almacen = new AlmacenSesion(carpeta);
        almacen.escribir(sesion(7));
        SesionEnCurso otra = sesion(7);
        otra.rutinaNombre = "Torso A";
        almacen.escribir(otra);

        assertEquals("Torso A", almacen.leer(7).rutinaNombre);
        assertFalse(new File(carpeta, "7.json.tmp").exists());
    }

    @Test
    public void otra_cuenta_no_la_ve() throws Exception {
        AlmacenSesion almacen = new AlmacenSesion(tmp.newFolder("sesion_en_curso"));
        almacen.escribir(sesion(7));

        assertNull(almacen.leer(8));
        assertNotNull(almacen.leer(7));
    }

    @Test
    public void un_fichero_de_otra_cuenta_renombrado_no_cuela() throws Exception {
        File carpeta = tmp.newFolder("sesion_en_curso");
        AlmacenSesion almacen = new AlmacenSesion(carpeta);
        almacen.escribir(sesion(7));
        assertTrue(new File(carpeta, "7.json").renameTo(new File(carpeta, "8.json")));

        assertNull(almacen.leer(8));
    }

    @Test
    public void roto_o_de_otra_version_se_ignora() throws Exception {
        File carpeta = tmp.newFolder("sesion_en_curso");
        AlmacenSesion almacen = new AlmacenSesion(carpeta);
        try (FileOutputStream out = new FileOutputStream(new File(carpeta, "7.json"))) {
            out.write("{roto".getBytes(StandardCharsets.UTF_8));
        }
        assertNull(almacen.leer(7));

        SesionEnCurso futura = sesion(7);
        futura.formato = SesionEnCurso.FORMATO + 1;
        almacen.escribir(futura);
        assertNull(almacen.leer(7));
    }

    @Test
    public void borrar_la_quita() throws Exception {
        AlmacenSesion almacen = new AlmacenSesion(tmp.newFolder("sesion_en_curso"));
        almacen.escribir(sesion(7));
        almacen.borrar(7);
        assertNull(almacen.leer(7));
    }
}
