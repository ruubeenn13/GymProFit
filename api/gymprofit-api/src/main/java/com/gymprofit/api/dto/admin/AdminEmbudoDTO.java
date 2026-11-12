package com.gymprofit.api.dto.admin;

import com.gymprofit.api.enums.PasoAlta;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

// ============================================================
// AdminEmbudoDTO — el embudo del alta y el núcleo, para la web de administración (GP-019)
//
// Solo recuentos, como el Resumen: ninguna fila de nadie (DEC-064). Semanas de lunes a
// domingo en Madrid, la más antigua primero; la última es la actual.
//
//  · embudo: cuántas veces se llegó a cada pantalla del alta esa semana, con todos los
//    pasos en su orden, también los que se quedan a 0.
//  · nucleo: de las cuentas creadas esa semana, cuántas en sus primeros 7 días (el del
//    alta y los seis siguientes) completaron un entreno, apuntaron una comida con algún
//    alimento y volvieron, es decir, apuntaron algo (entreno, comida, agua o peso) un día
//    distinto al del alta. completa dice si a la última cuenta de la semana ya le han
//    pasado sus 7 días; si no, las cifras aún pueden subir.
// ============================================================
public record AdminEmbudoDTO(
        LocalDate hoy,
        List<SemanaEmbudo> embudo,
        List<SemanaNucleo> nucleo) implements Serializable {

    /** Las llegadas a cada paso en una semana, en el orden del alta. */
    public record SemanaEmbudo(LocalDate lunes, List<PasoTotal> pasos) implements Serializable {
    }

    /** Un paso y sus llegadas. */
    public record PasoTotal(PasoAlta paso, long total) implements Serializable {
    }

    /** Las cuentas de una semana de alta y lo que hicieron en sus primeros 7 días. */
    public record SemanaNucleo(LocalDate lunes, long cuentas, long entrenan, long comen, long vuelven,
                               boolean completa) implements Serializable {
    }
}
