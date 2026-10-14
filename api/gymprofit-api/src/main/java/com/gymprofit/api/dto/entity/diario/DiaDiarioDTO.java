package com.gymprofit.api.dto.entity.diario;

import com.gymprofit.api.enums.TipoComida;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

// ============================================================
// DiaDiarioDTO — GET /diario: lo que el diario necesita de cada día (lote 1.6.8)
// Las comidas con algo apuntado y las marcadas «Hoy no», para el día completo; el agua;
// y el entreno, de las sesiones terminadas ese día. Si el día está completo lo decide la
// app (decisión 10 del lienzo): aquí van los hechos, no la regla.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiaDiarioDTO {

    private LocalDate fecha;

    // Las comidas con al menos un alimento, en el orden del día (desayuno … cena).
    private List<TipoComida> comidasApuntadas;

    // Las comidas marcadas «Hoy no», en el mismo orden.
    private List<TipoComida> hoyNo;

    // Mililitros de agua; 0 si no se apuntó nada.
    private int aguaMl;

    // Sesiones terminadas ese día.
    private int entrenos;

    // Nombre de la rutina del último entreno con rutina, en el idioma de la petición;
    // null si no hubo entreno o fueron todos libres.
    private String rutinaEntreno;
}
