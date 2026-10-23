package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// ExtraEntrenoTest — la línea de color de cada entreno en «Tus últimos entrenos» (GP-216,
// tablero 13): récords en dorado, subida en verde y el resto en neutro. Bajar no se pinta
// de rojo (DEC-055): una sesión más floja no es un fallo.
// ============================================================
public class ExtraEntrenoTest {

    @Test
    public void conRecordsGanaElDorado() {
        ExtraEntreno e = ExtraEntreno.de(2, 8400, 8000);
        assertEquals(ExtraEntreno.Tipo.RECORDS, e.tipo);
        assertEquals(2, e.n);
    }

    @Test
    public void sinRecordsPeroSubiendoVaEnVerde() {
        ExtraEntreno e = ExtraEntreno.de(0, 8400, 8077);
        assertEquals(ExtraEntreno.Tipo.SUBE, e.tipo);
        assertEquals(4, e.n);
    }

    @Test
    public void sinRecordsYBajandoEsNeutroConElPorcentaje() {
        ExtraEntreno e = ExtraEntreno.de(0, 7000, 8000);
        assertEquals(ExtraEntreno.Tipo.BAJA, e.tipo);
        assertEquals(13, e.n);
    }

    @Test
    public void unaDiferenciaQueRedondeaACeroEsIgual() {
        assertEquals(ExtraEntreno.Tipo.SIN_RECORDS, ExtraEntreno.de(0, 8004, 8000).tipo);
        assertEquals(ExtraEntreno.Tipo.SIN_RECORDS, ExtraEntreno.de(0, 7996, 8000).tipo);
    }

    @Test
    public void sinSesionAnteriorNoHayPorcentaje() {
        assertEquals(ExtraEntreno.Tipo.SIN_RECORDS, ExtraEntreno.de(0, 8000, 0).tipo);
        assertEquals(ExtraEntreno.Tipo.SIN_RECORDS, ExtraEntreno.de(0, 8000, -1).tipo);
    }

    @Test
    public void sinKilosNoHayPorcentaje() {
        assertEquals(ExtraEntreno.Tipo.SIN_RECORDS, ExtraEntreno.de(0, 0, 8000).tipo);
    }
}
