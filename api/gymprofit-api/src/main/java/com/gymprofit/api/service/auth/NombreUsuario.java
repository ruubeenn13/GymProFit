package com.gymprofit.api.service.auth;

import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

// ============================================================
// NombreUsuario — el nombre de usuario de una cuenta nueva (GP-103, lote 1.5.0)
//
// El alta nueva no lo pide: la API lo propone con la parte del correo antes de la «@»,
// en minúsculas, sin tildes y solo con letras, números, punto y guion bajo, de 3 a 30
// caracteres. Si se queda corto o ya existe, se le añade un número.
//
// Y ningún nombre nuevo lleva «@», se cree la cuenta por donde se cree: el login decide
// por la «@» si lo escrito es un correo (POST /auth/login), y un usuario con «@» podría
// llamarse como el correo de otra cuenta.
// ============================================================
@Component
@AllArgsConstructor
public class NombreUsuario {

    /** Largo mínimo de un nombre de usuario, el de RegisterDTO. */
    static final int MIN = 3;

    /** Largo máximo del nombre propuesto; la columna admite 50 para los que se escriben. */
    static final int MAX_PROPUESTO = 30;

    /** Base cuando el correo no deja nada aprovechable (p. ej. «+++@x.com»). */
    static final String BASE_VACIA = "usuario";

    private final IUsuarioRepository usuarioRepository;

    /**
     * Rechaza un nombre de usuario escrito que lleve «@».
     *
     * @param username el nombre que llega del cliente.
     * @throws InvalidDataException 400 con {@code USERNAME_NO_VALIDO} en "cause".
     */
    public static void comprobar(String username) {
        if (username != null && username.indexOf('@') >= 0) {
            throw InvalidDataException.conCodigo(InvalidDataException.USERNAME_NO_VALIDO, "error.username.arroba");
        }
    }

    /**
     * Propone un nombre libre a partir del correo: la base limpia si está libre y tiene
     * 3 caracteres; si no, la base con el primer número que la deja libre y en 3 o más,
     * recortando la base para no pasar de 30. «ana» choca → «ana2», «ana3»…
     *
     * @param email el correo de la cuenta nueva.
     * @return un nombre de usuario libre en este momento.
     */
    public String proponer(String email) {
        String base = base(email);
        if (base.length() >= MIN && !usuarioRepository.existsByUsername(base)) {
            return base;
        }
        // Una base corta empieza en 1 («al1»); una ocupada, en 2, que se lee como «la segunda».
        for (long n = base.length() >= MIN ? 2 : 1; ; n++) {
            String sufijo = Long.toString(n);
            String candidato = recortar(base, MAX_PROPUESTO - sufijo.length()) + sufijo;
            if (candidato.length() >= MIN && !usuarioRepository.existsByUsername(candidato)) {
                return candidato;
            }
        }
    }

    // La parte del correo antes de la última «@», limpia y recortada a 30.
    static String base(String email) {
        int arroba = email.lastIndexOf('@');
        String local = arroba < 0 ? email : email.substring(0, arroba);
        String sinTildes = Normalizer.normalize(local.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String limpio = recortar(sinTildes.replaceAll("[^a-z0-9._]", ""), MAX_PROPUESTO);
        return limpio.isEmpty() ? BASE_VACIA : limpio;
    }

    private static String recortar(String texto, int max) {
        return texto.length() > max ? texto.substring(0, max) : texto;
    }
}
