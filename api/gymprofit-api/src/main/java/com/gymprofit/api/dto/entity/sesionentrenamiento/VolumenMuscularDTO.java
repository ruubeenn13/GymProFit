package com.gymprofit.api.dto.entity.sesionentrenamiento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// VolumenMuscularDTO — series que ha recibido un músculo en una ventana de días
// Es lo que alimenta la silueta de Home: por cada músculo tocado, cuántas series
// han caído sobre él. La app tiñe con eso, así que aquí solo viajan la clave del
// músculo (normalizada, sin tildes ni mayúsculas) y el número.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VolumenMuscularDTO implements Serializable {

    // Clave normalizada del músculo: "pecho", "cuadriceps", "dorsales"…
    private String musculo;

    // Series acumuladas sobre ese músculo en la ventana consultada.
    private Integer series;
}
