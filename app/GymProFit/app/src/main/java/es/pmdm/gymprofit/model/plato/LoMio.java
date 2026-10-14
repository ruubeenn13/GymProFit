package es.pmdm.gymprofit.model.plato;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.model.alimento.Alimento;

// ============================================================
// LoMio — la pestaña «Mis platos» (lote 1.6.6, GET /alimentos/mios)
// Los platos de la cuenta y, aparte, los alimentos que ha creado; del usado más
// recientemente al menos.
// ============================================================
public class LoMio {

    @Nullable private List<Alimento> platos;
    @Nullable private List<Alimento> alimentos;

    public LoMio() {
    }

    public LoMio(@Nullable List<Alimento> platos, @Nullable List<Alimento> alimentos) {
        this.platos = platos;
        this.alimentos = alimentos;
    }

    @NonNull public List<Alimento> getPlatos() { return platos != null ? platos : new ArrayList<>(); }

    @NonNull public List<Alimento> getAlimentos() { return alimentos != null ? alimentos : new ArrayList<>(); }
}
