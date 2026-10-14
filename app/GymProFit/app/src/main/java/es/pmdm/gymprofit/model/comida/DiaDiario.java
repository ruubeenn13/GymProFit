package es.pmdm.gymprofit.model.comida;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

// ============================================================
// DiaDiario — un día del diario (lote 1.6.8, GET /diario)
// Las comidas con algo apuntado y las marcadas «Hoy no» (tipos de ComidaQueToca), el
// agua en mililitros y el entreno: cuántas sesiones terminadas y el nombre de la rutina
// del último con rutina (null si fue libre o no hubo).
// ============================================================
public class DiaDiario {

    private String fecha;
    private List<String> comidasApuntadas;
    private List<String> hoyNo;
    private int aguaMl;
    private int entrenos;
    private String rutinaEntreno;

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    @NonNull
    public List<String> getComidasApuntadas() {
        return comidasApuntadas != null ? comidasApuntadas : new ArrayList<>();
    }
    public void setComidasApuntadas(List<String> comidasApuntadas) { this.comidasApuntadas = comidasApuntadas; }

    @NonNull
    public List<String> getHoyNo() {
        return hoyNo != null ? hoyNo : new ArrayList<>();
    }
    public void setHoyNo(List<String> hoyNo) { this.hoyNo = hoyNo; }

    public int getAguaMl() { return aguaMl; }
    public void setAguaMl(int aguaMl) { this.aguaMl = aguaMl; }

    public int getEntrenos() { return entrenos; }
    public void setEntrenos(int entrenos) { this.entrenos = entrenos; }

    @Nullable
    public String getRutinaEntreno() { return rutinaEntreno; }
    public void setRutinaEntreno(String rutinaEntreno) { this.rutinaEntreno = rutinaEntreno; }
}
