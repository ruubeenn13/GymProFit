package com.gymprofit.api.service.usuario;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.usuario.EliminarCuentaDTO;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UnauthorizedException;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.alimentocomida.IAlimentoComidaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// ============================================================
// BorradoCuentaService — borrado definitivo de una cuenta y de todo lo suyo
//
// Borrado real e inmediato: ni periodo de gracia, ni copia anonimizada, ni baja
// lógica. Es lo que gymprofit.app/eliminar-cuenta promete por escrito, y una
// promesa de borrado que deja filas por ahí es peor que no prometer nada.
//
// El borrado es EXPLÍCITO y ORDENADO, aquí, y no delegado en las cascadas del
// motor. Las cascadas que hay en la base de datos pueden quedarse, pero nada de lo
// que importa depende de ellas: una cascada no se lee, no se prueba con nombre
// propio y no avisa cuando alguien añade una tabla. Este método sí se lee de arriba
// abajo, y el test lo compara contra el esquema real.
//
// Dos casos que no son obvios y están tratados a propósito:
//
//  · Las plantillas del sistema (rutinas con usuario_id NULL) NO se tocan nunca.
//    Los borrados filtran por usuario_id = :id, que en SQL ya excluye los NULL.
//  · Los alimentos personalizados se BORRAN. La FK de la base de datos dice
//    ON DELETE SET NULL, y eso, en esta tabla, no borra: PUBLICA. usuario_id NULL
//    es justo lo que marca un alimento como catálogo global, así que dejar actuar a
//    la cascada convertiría la dieta privada de alguien que se va en catálogo
//    público. Es un fallo de privacidad, no una decisión, y por eso el servicio
//    borra las filas antes de que la cascada llegue a tener nada que hacer.
// ============================================================
@Service
@RequiredArgsConstructor
public class BorradoCuentaService implements IBorradoCuentaService {

    private static final Logger logger = LoggerFactory.getLogger(BorradoCuentaService.class);

    @PersistenceContext
    private EntityManager em;

    private final IUsuarioRepository usuarioRepository;
    private final IAlimentoComidaService alimentoComidaService;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;

    /**
     * Borra la cuenta del usuario autenticado y todos sus datos, en una sola transacción.
     * <p>
     * El id sale del token (DEC-013): el cuerpo solo trae la contraseña, así que no hay
     * forma de pedir el borrado de otro. La contraseña se comprueba ANTES de tocar nada.
     *
     * @param dto contraseña actual.
     * @throws UnauthorizedException (→ 403) si la contraseña no es la de la cuenta.
     */
    @Override
    @Transactional
    public void eliminarCuentaPropia(EliminarCuentaDTO dto) {
        Integer usuarioId = securityUtils.getCurrentUserId();

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + usuarioId + " no existe"));

        // Reautenticación. Va la primera: si falla, no se ha borrado ni una fila.
        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPassword())) {
            logger.warn("Intento de borrado de cuenta con contraseña incorrecta, usuario id={}", usuarioId);
            throw new UnauthorizedException("La contraseña no es correcta");
        }

        logger.info("Borrando definitivamente la cuenta del usuario id={}", usuarioId);

        // El resto va por consultas masivas, que no pasan por el contexto de persistencia.
        // Se vacía antes para que no queden entidades gestionadas apuntando a filas muertas.
        em.flush();
        em.clear();

        borrarDatosDelUsuario(usuarioId);

        logger.info("Cuenta del usuario id={} borrada por completo", usuarioId);
    }

    // Orden de borrado: de las hojas hacia la raíz. Cada paso deja el siguiente sin
    // referencias que lo bloqueen, porque casi todas las FK son RESTRICT.
    private void borrarDatosDelUsuario(Integer usuarioId) {
        // --- Entrenamiento -------------------------------------------------
        borrar("Series realizadas", """
                DELETE FROM SerieRealizada s WHERE s.ejercicioRealizado.id IN (
                    SELECT er.id FROM EjercicioRealizado er WHERE er.sesion.id IN (
                        SELECT ses.id FROM SesionEntrenamiento ses WHERE ses.usuario.id = :id))""", usuarioId);

        borrar("Ejercicios realizados", """
                DELETE FROM EjercicioRealizado er WHERE er.sesion.id IN (
                    SELECT ses.id FROM SesionEntrenamiento ses WHERE ses.usuario.id = :id)""", usuarioId);

        borrar("Sesiones de entrenamiento",
                "DELETE FROM SesionEntrenamiento s WHERE s.usuario.id = :id", usuarioId);

        borrar("Ejercicios de sus rutinas", """
                DELETE FROM RutinaEjercicio re WHERE re.rutina.id IN (
                    SELECT r.id FROM Rutina r WHERE r.usuario.id = :id)""", usuarioId);

        desvincularSesionesAjenasDeSusRutinas(usuarioId);

        // usuario.id = :id nunca casa con las plantillas del sistema, que tienen
        // usuario_id NULL: en SQL, NULL = :id no es cierto. Las plantillas se quedan.
        borrar("Rutinas", "DELETE FROM Rutina r WHERE r.usuario.id = :id", usuarioId);

        // --- Nutrición -----------------------------------------------------
        borrar("Líneas de sus comidas", """
                DELETE FROM AlimentoComida ac WHERE ac.comida.id IN (
                    SELECT c.id FROM Comida c WHERE c.usuario.id = :id)""", usuarioId);

        borrarLineasAjenasConSusAlimentos(usuarioId);

        borrar("Comidas", "DELETE FROM Comida c WHERE c.usuario.id = :id", usuarioId);

        // Se borran, NO se despublican: ver la cabecera de la clase.
        borrar("Alimentos personalizados", "DELETE FROM Alimento a WHERE a.usuario.id = :id", usuarioId);

        // --- Progreso y perfil ---------------------------------------------
        borrar("Progreso de ejercicios", "DELETE FROM ProgresoEjercicio p WHERE p.usuario.id = :id", usuarioId);
        borrar("Mediciones corporales", "DELETE FROM MedicionCorporal m WHERE m.usuario.id = :id", usuarioId);
        borrar("Objetivos personales", "DELETE FROM ObjetivoPersonal o WHERE o.usuario.id = :id", usuarioId);
        borrar("Logros obtenidos", "DELETE FROM UsuarioLogro ul WHERE ul.usuario.id = :id", usuarioId);
        borrar("Notificaciones", "DELETE FROM Notificacion n WHERE n.usuario.id = :id", usuarioId);
        borrar("Foto de perfil", "DELETE FROM FotoPerfil f WHERE f.usuarioId = :id", usuarioId);

        // --- Sesiones abiertas y avisos ------------------------------------
        // Explícito y no por cascada: que alguien siga con la app abierta o reciba una
        // push de una cuenta que ya no existe es el fallo más visible de todos, y no
        // puede depender de una cláusula del esquema que nadie vuelve a mirar.
        borrar("Refresh tokens (todas sus sesiones)",
                "DELETE FROM RefreshToken rt WHERE rt.usuario.id = :id", usuarioId);
        borrar("Device tokens (todos sus dispositivos)",
                "DELETE FROM DeviceToken dt WHERE dt.usuario.id = :id", usuarioId);
        borrar("Códigos de recuperación",
                "DELETE FROM PasswordResetCodigo prc WHERE prc.usuario.id = :id", usuarioId);

        // --- La cuenta -----------------------------------------------------
        // usuario_roles es tabla de unión sin entidad propia: se borra en nativo.
        int roles = em.createNativeQuery("DELETE FROM usuario_roles WHERE usuario_id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        logger.info("Borrado de cuenta id={} · Roles asignados: {} filas", usuarioId, roles);

        borrar("Usuario", "DELETE FROM Usuario u WHERE u.id = :id", usuarioId);
    }

    /**
     * Desvincula de las rutinas del usuario las sesiones de OTROS usuarios.
     * <p>
     * No debería existir ninguna: la aplicación solo ofrece tus propias rutinas al
     * empezar una sesión. Si hay alguna es porque se fabricó a mano contra la API.
     * Se desvincula en vez de borrarla —la sesión ajena es un entrenamiento real de otra
     * persona y se conserva entera, solo pierde el enlace a una rutina que va a dejar de
     * existir— y queda constancia en el log. No se avisa a esos usuarios: contarles por
     * qué su sesión perdió la rutina sería contarles que alguien ha borrado su cuenta.
     */
    private void desvincularSesionesAjenasDeSusRutinas(Integer usuarioId) {
        int sesiones = em.createQuery("""
                        UPDATE SesionEntrenamiento s SET s.rutina = NULL
                        WHERE s.usuario.id <> :id AND s.rutina.id IN (
                            SELECT r.id FROM Rutina r WHERE r.usuario.id = :id)""")
                .setParameter("id", usuarioId)
                .executeUpdate();

        if (sesiones > 0) {
            logger.warn("Borrado de cuenta id={} · {} sesiones de OTROS usuarios apuntaban a sus rutinas "
                    + "y se han desvinculado. Eso no debería poder ocurrir por la aplicación.", usuarioId, sesiones);
        }
    }

    /**
     * Borra las líneas de comidas de OTROS usuarios que referencien alimentos del que se va.
     * <p>
     * Tampoco debería existir ninguna: la aplicación solo enseña tus propios alimentos. Las
     * únicas filas posibles son las fabricadas explotando la IDOR de escritura de
     * {@code POST /alimentos-comida}, ya cerrada. Entre el derecho de supresión, prometido
     * por escrito, y una comida montada abusando de un fallo, gana la supresión.
     * <p>
     * Queda constancia en el log de cuántas líneas y de qué comidas, porque es una
     * modificación de datos de terceros y tiene que poder reconstruirse. A esos usuarios
     * <strong>no se les avisa</strong>: explicarles por qué desapareció un ingrediente les
     * estaría contando que otra persona ha borrado su cuenta.
     */
    private void borrarLineasAjenasConSusAlimentos(Integer usuarioId) {
        List<Integer> comidasAfectadas = em.createQuery("""
                        SELECT DISTINCT ac.comida.id FROM AlimentoComida ac
                        WHERE ac.alimento.usuario.id = :id AND ac.comida.usuario.id <> :id""", Integer.class)
                .setParameter("id", usuarioId)
                .getResultList();

        if (comidasAfectadas.isEmpty()) {
            return;
        }

        int lineas = em.createQuery("""
                        DELETE FROM AlimentoComida ac
                        WHERE ac.alimento.id IN (SELECT a.id FROM Alimento a WHERE a.usuario.id = :id)
                          AND ac.comida.id IN :comidas""")
                .setParameter("id", usuarioId)
                .setParameter("comidas", comidasAfectadas)
                .executeUpdate();

        logger.warn("Borrado de cuenta id={} · {} líneas de comidas AJENAS referenciaban sus alimentos "
                        + "y se han borrado. Comidas afectadas: {}. Sus totales se recalculan. "
                        + "A esos usuarios no se les notifica, a propósito.",
                usuarioId, lineas, comidasAfectadas);

        // Los totales de una comida están guardados en columnas, no se calculan al leer,
        // así que sin recalcular quedarían contando un alimento que ya no está.
        em.flush();
        em.clear();
        comidasAfectadas.forEach(alimentoComidaService::recalcularTotales);
    }

    // Ejecuta un borrado masivo y deja en el log cuántas filas se llevó. El nombre es
    // para el log: quien lea la traza tiene que ver qué se borró sin abrir el código.
    private void borrar(String queSeBorra, String jpql, Integer usuarioId) {
        int filas = em.createQuery(jpql)
                .setParameter("id", usuarioId)
                .executeUpdate();

        logger.info("Borrado de cuenta id={} · {}: {} filas", usuarioId, queSeBorra, filas);
    }
}
