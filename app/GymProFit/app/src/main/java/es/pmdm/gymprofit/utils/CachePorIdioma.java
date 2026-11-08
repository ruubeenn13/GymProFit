package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

// ============================================================
// CachePorIdioma — un valor pedido a la API guardado en memoria junto al idioma en que llegó
// (GP-251).
//
// La API localiza los textos del catálogo con Accept-Language. Cambiar de idioma no reinicia el
// proceso, así que lo guardado en memoria antes del cambio seguía en el idioma de antes: con la
// app en inglés, el detalle de una rutina sacaba «Press de banca» (del catálogo pedido en español)
// junto a «Bench press» (de la rutina, pedida ya en inglés). Lo guardado solo sirve para el idioma
// con el que se pidió. Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class CachePorIdioma<T> {

    @Nullable private T valor;
    @Nullable private String idioma;

    /** Lo guardado si se pidió en {@code idioma}; si no, null. */
    @Nullable
    public synchronized T get(@NonNull String idioma) {
        return Objects.equals(this.idioma, idioma) ? valor : null;
    }

    /** Guarda {@code valor}, pedido en {@code idioma}; lo de otro idioma se pierde. */
    public synchronized void put(@NonNull String idioma, @NonNull T valor) {
        this.idioma = idioma;
        this.valor = valor;
    }
}
