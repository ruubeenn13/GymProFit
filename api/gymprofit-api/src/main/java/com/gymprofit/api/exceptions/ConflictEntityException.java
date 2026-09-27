package com.gymprofit.api.exceptions;

// ============================================================
// ConflictEntityException — el valor pedido choca con el de otro registro (409).
//
// Distinta de DuplicateEntityException, que responde 400 y es el contrato que ya
// consumen el registro y el alta de administración: cambiarla movería esos códigos
// para las builds repartidas. Esta nace con GP-083, en el cambio de correo, donde el
// 409 es parte del contrato desde el principio.
// ============================================================
public class ConflictEntityException extends ExcepcionConClave {

    /**
     * @param clave clave del mensaje en messages*.properties (GP-109)
     * @param args  argumentos del mensaje
     */
    public ConflictEntityException(String clave, Object... args) {
        super(clave, args, null);
    }
}
