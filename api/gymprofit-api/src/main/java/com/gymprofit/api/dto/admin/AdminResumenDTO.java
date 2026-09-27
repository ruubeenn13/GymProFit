package com.gymprofit.api.dto.admin;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

// ============================================================
// AdminResumenDTO — la pantalla de Resumen de la web de administración (GP-085)
//
// Todo en una ruta, para que la pantalla no haga ocho peticiones. Solo recuentos:
// ninguna fila de nadie. Días y semanas (de lunes a domingo) son de Madrid; hoy dice
// cuál es «hoy» para quien calculó, y es la fecha que pinta la web.
//
// Sesiones: las completadas. Una sesión que alguien abrió y no terminó no es un
// entrenamiento.
// ============================================================
public record AdminResumenDTO(
        LocalDate hoy,
        Cuentas cuentas,
        Sesiones sesiones,
        // Cuentas distintas con al menos una sesión completada esta semana.
        long entrenaronSemana,
        // Cuentas distintas que apuntaron alguna comida hoy.
        long comidaHoy,
        // Las 8 últimas semanas, la más antigua primero; la última es la actual.
        List<Semana> altasPorSemana,
        // Los 14 últimos días, el más antiguo primero; el último es hoy.
        List<Dia> sesionesPorDia,
        Catalogo catalogo) implements Serializable {

    /** Cuentas: todas, altas de esta semana y cuántas están activas. */
    public record Cuentas(long total, long altasSemana, long activas) implements Serializable {
    }

    /** Sesiones completadas hoy, esta semana y desde siempre. */
    public record Sesiones(long hoy, long semana, long total) implements Serializable {
    }

    /** Altas de la semana que empieza el lunes indicado. */
    public record Semana(LocalDate lunes, long altas) implements Serializable {
    }

    /** Sesiones completadas un día. */
    public record Dia(LocalDate fecha, long sesiones) implements Serializable {
    }

    /** Lo pendiente del catálogo. */
    public record Catalogo(long ejerciciosActivos, long ejerciciosSinRevisar,
                           long alimentosCatalogo, long alimentosSinIngles) implements Serializable {
    }
}
