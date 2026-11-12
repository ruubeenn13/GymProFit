package com.gymprofit.api.exceptions;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;

// ============================================================
// FalloInfraestructura — reconoce un fallo de la base de datos que no es culpa de la
// petición: el pool sin conexiones libres, la base caída o la conexión cortada (GP-260).
//
// Esas excepciones llegan envueltas de formas distintas según quién las vea primero:
// Hikari lanza SQLTransientConnectionException, Hibernate la envuelve en
// JDBCConnectionException, Spring en DataAccessResourceFailureException o en
// CannotCreateTransactionException si falla al abrir la transacción, y Spring Security
// en InternalAuthenticationServiceException si pasa dentro del login. Por eso se recorre
// la cadena de causas entera en vez de mirar el tipo de fuera.
// ============================================================
public final class FalloInfraestructura {

    private FalloInfraestructura() {
    }

    /**
     * Dice si la excepción, o alguna de sus causas, es un fallo de la base de datos.
     *
     * @param ex la excepción que llegó al manejador
     * @return true si el fallo es de infraestructura y la respuesta debe ser 503
     */
    public static boolean es(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof DataAccessResourceFailureException
                    || t instanceof CannotCreateTransactionException
                    || t instanceof org.hibernate.exception.JDBCConnectionException
                    || t instanceof SQLTransientConnectionException
                    || t instanceof SQLNonTransientConnectionException) {
                return true;
            }
        }
        return false;
    }
}
