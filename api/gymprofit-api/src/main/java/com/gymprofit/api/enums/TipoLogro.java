package com.gymprofit.api.enums;

// ============================================================
// TipoLogro — tipos de logros/insignias que puede desbloquear un usuario.
//
// Cada tipo lleva QUÉ se cuenta (su métrica) y CUÁNTO hace falta (su umbral).
// Es el único sitio donde viven esos números (GP-079): los leen la evaluación
// que concede el logro, el progreso que ve la app y el aviso de «logro
// próximo». Antes estaban escritos en un switch de LogroService y repetidos a
// mano en RecordatorioNotificacionesTask, así que la barra de progreso podía
// decir «7 de 7» de un logro que la evaluación no concedía.
//
// El texto de la descripción que guarda la tabla logros («Completa 7
// sesiones…») repite el número en prosa. No se deriva de aquí porque es
// contenido traducido en la base; si cambia un umbral, hay que cambiar también
// esas dos columnas.
// ============================================================
public enum TipoLogro {
    // Se otorga al completar la primera sesión de entrenamiento.
    PRIMERA_SESION(Metrica.SESIONES_COMPLETADAS, 1),
    // Reconoce la regularidad/constancia en el entrenamiento.
    CONSTANCIA(Metrica.SESIONES_COMPLETADAS, 7),
    // Reconoce un alto nivel de dedicación del usuario.
    DEDICADO(Metrica.SESIONES_COMPLETADAS, 30),
    // Se otorga al llegar a 100 ejercicios realizados en total.
    CENTENARIO(Metrica.EJERCICIOS_REALIZADOS, 100),
    // Se otorga al completar un objetivo personal.
    OBJETIVO_CUMPLIDO(Metrica.OBJETIVOS_COMPLETADOS, 1),
    // Nivel máximo: diez objetivos personales completados.
    MAQUINA(Metrica.OBJETIVOS_COMPLETADOS, 10);

    /** Lo que se cuenta para un logro. La app lo usa para elegir la palabra: «sesiones», «ejercicios»… */
    public enum Metrica {
        SESIONES_COMPLETADAS,
        EJERCICIOS_REALIZADOS,
        OBJETIVOS_COMPLETADOS
    }

    private final Metrica metrica;
    private final int umbral;

    TipoLogro(Metrica metrica, int umbral) {
        this.metrica = metrica;
        this.umbral = umbral;
    }

    public Metrica getMetrica() {
        return metrica;
    }

    public int getUmbral() {
        return umbral;
    }

    /**
     * Si un valor de la métrica alcanza el logro. Es la ÚNICA comparación con el
     * umbral: evaluación y progreso pasan por aquí para no poder discrepar en el borde.
     *
     * @param valor lo que lleva el usuario en la métrica de este tipo
     * @return {@code true} a partir del umbral, incluido
     */
    public boolean alcanzado(long valor) {
        return valor >= umbral;
    }
}
