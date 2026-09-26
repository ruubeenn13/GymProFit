package com.gymprofit.api.service.auth;

import com.gymprofit.api.exceptions.ContrasenaRechazadaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

// ============================================================
// PoliticaContrasena — lo que una contraseña nueva no puede ser (GP-101, DEC-034).
//
// La forma (longitud, bytes, caracteres) la valida @ContrasenaNueva en el DTO. Aquí va
// lo que NIST SP 800-63B-4 pide comprobar contra una lista, y que necesita saber de
// quién es la cuenta:
//   · Contraseñas comunes o filtradas: seguridad/contrasenas-comunes.txt, dentro de la
//     API y sin servicios externos (fuente y licencia en la cabecera del fichero).
//   · La que es o contiene el nombre del servicio o el nombre de usuario.
// Sin distinguir mayúsculas. Cada rechazo lleva su código en "cause" y el mismo 400.
// Solo al poner una contraseña nueva (alta, recuperar, cambiar), nunca al entrar.
// ============================================================
@Component
public class PoliticaContrasena {

    /** Nombre del servicio: una contraseña que lo contiene es de las primeras que se prueban. */
    static final String NOMBRE_SERVICIO = "gymprofit";

    private static final String LISTA = "seguridad/contrasenas-comunes.txt";

    private static final Logger logger = LoggerFactory.getLogger(PoliticaContrasena.class);

    private final Set<String> comunes;

    public PoliticaContrasena() {
        this.comunes = cargar();
        logger.info("Lista de contraseñas comunes cargada: {} entradas", comunes.size());
    }

    /**
     * Rechaza la contraseña si está en la lista o contiene el servicio o el usuario.
     *
     * @param contrasena la contraseña nueva, ya validada en forma.
     * @param username   el nombre de usuario de la cuenta.
     * @throws ContrasenaRechazadaException (→ 400 con código en "cause").
     */
    public void comprobar(String contrasena, String username) {
        String c = contrasena.toLowerCase(Locale.ROOT);
        if (comunes.contains(c)) {
            throw new ContrasenaRechazadaException(
                    "Esa contraseña es demasiado común", ContrasenaRechazadaException.PASSWORD_COMUN);
        }
        if (c.contains(NOMBRE_SERVICIO)
                || (username != null && !username.isBlank() && c.contains(username.toLowerCase(Locale.ROOT)))) {
            throw new ContrasenaRechazadaException(
                    "La contraseña no puede contener el nombre del servicio ni tu usuario",
                    ContrasenaRechazadaException.PASSWORD_CONTIENE_NOMBRE);
        }
    }

    // Lee la lista una vez al arrancar. Sin ella no se arranca: una política que no se
    // aplica sin avisar es peor que no tenerla (DEC-016).
    private static Set<String> cargar() {
        Set<String> set = new HashSet<>(50_000);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new ClassPathResource(LISTA).getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = r.readLine()) != null) {
                if (!linea.isEmpty() && !linea.startsWith("#")) set.add(linea.toLowerCase(Locale.ROOT));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + LISTA, e);
        }
        if (set.isEmpty()) throw new IllegalStateException(LISTA + " está vacía");
        return set;
    }
}
