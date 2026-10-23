package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// FraseProfiTest — qué dice Profi en Inicio (GP-216, tablero 13): una frase con un dato
// real, no una de adorno. Si no hay dato que decir, no hay bocadillo.
// ============================================================
public class FraseProfiTest {

    private static FraseProfi.Datos datos() {
        return new FraseProfi.Datos();
    }

    @Test
    public void sinEntrenosInvitaAEmpezar() {
        assertEquals(FraseProfi.Tipo.SIN_ENTRENOS, FraseProfi.elegir(datos()).tipo);
    }

    @Test
    public void entrenadoHoyDiceLoQueHizo() {
        FraseProfi.Datos d = datos();
        d.hayEntrenos = true;
        d.entrenadoHoy = true;
        d.ultimaRutina = "Pierna A";
        d.recordsUltima = 2;
        FraseProfi f = FraseProfi.elegir(d);
        assertEquals(FraseProfi.Tipo.ENTRENADO_HOY, f.tipo);
        assertEquals("Pierna A", f.rutina);
        assertEquals(2, f.n);
    }

    @Test
    public void conRecordsEnLaUltimaSesionLosCuentaYNombraLoDeHoy() {
        FraseProfi.Datos d = datos();
        d.hayEntrenos = true;
        d.ultimaRutina = "Pierna A";
        d.recordsUltima = 2;
        d.rutinaHoy = "Torso B";
        d.primerEjercicioHoy = "Press banca";
        FraseProfi f = FraseProfi.elegir(d);
        assertEquals(FraseProfi.Tipo.RECORDS_ULTIMA, f.tipo);
        assertEquals("Pierna A", f.rutina);
        assertEquals(2, f.n);
        assertEquals("Torso B", f.rutinaHoy);
        assertEquals("Press banca", f.ejercicioHoy);
    }

    @Test
    public void sinRecordsPeroConSesionesEstaSemanaCuentaLasSesiones() {
        FraseProfi.Datos d = datos();
        d.hayEntrenos = true;
        d.ultimaRutina = "Pierna A";
        d.sesionesSemana = 2;
        FraseProfi f = FraseProfi.elegir(d);
        assertEquals(FraseProfi.Tipo.SEMANA, f.tipo);
        assertEquals(2, f.n);
    }

    @Test
    public void conFaltaDeProteinaHablaDeLaProteina() {
        FraseProfi.Datos d = datos();
        d.hayEntrenos = true;
        d.proteinaFalta = 48;
        FraseProfi f = FraseProfi.elegir(d);
        assertEquals(FraseProfi.Tipo.PROTEINA, f.tipo);
        assertEquals(48, f.n);
    }

    @Test
    public void menosDeVeinteGramosNoSeMencionan() {
        FraseProfi.Datos d = datos();
        d.hayEntrenos = true;
        d.proteinaFalta = 19;
        d.rutinaHoy = "Pierna B";
        assertEquals(FraseProfi.Tipo.HOY_TOCA, FraseProfi.elegir(d).tipo);
    }

    @Test
    public void sinNadaQueContarQuedaLoDeHoy() {
        FraseProfi.Datos d = datos();
        d.hayEntrenos = true;
        d.rutinaHoy = "Pierna B";
        FraseProfi f = FraseProfi.elegir(d);
        assertEquals(FraseProfi.Tipo.HOY_TOCA, f.tipo);
        assertEquals("Pierna B", f.rutinaHoy);
    }

    @Test
    public void sinNadaDeNadaNoHayFrase() {
        FraseProfi.Datos d = datos();
        d.hayEntrenos = true;
        assertEquals(FraseProfi.Tipo.NINGUNA, FraseProfi.elegir(d).tipo);
    }
}
