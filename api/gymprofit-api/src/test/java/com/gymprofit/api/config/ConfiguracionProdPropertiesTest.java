package com.gymprofit.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// ConfiguracionProdPropertiesTest — el perfil prod no puede traer comodidades de
// desarrollo escondidas en un valor por defecto.
//
// Los dos incidentes que motivan estos tests eran el mismo error escrito dos veces:
// ${MAIL_HOST:} y una contraseña de admin literal en el código. En los dos casos la
// configuración se degradaba en silencio y la API arrancaba igual, aparentando
// normalidad. El fichero se lee como texto en vez de levantar el contexto porque el
// perfil prod exige base de datos gestionada, y lo que se comprueba aquí es la forma
// de la configuración, no su carga.
// ============================================================
class ConfiguracionProdPropertiesTest {

    private static final String FICHERO = "application-prod.properties";

    // Claves sin las que la API NO debe arrancar: placeholder pelado, sin dos puntos.
    private static final String[] OBLIGATORIAS = {
            "jwt.secret", "spring.mail.host", "spring.mail.username", "spring.mail.password",
            "app.mail.from",
            "spring.datasource.url", "spring.datasource.username", "spring.datasource.password"
    };

    @Test
    void lasClavesObligatoriasNoTienenValorPorDefecto() throws IOException {
        Properties propiedades = cargar();

        for (String clave : OBLIGATORIAS) {
            String valor = propiedades.getProperty(clave);
            assertThat(valor)
                    .as("%s no está definida en %s", clave, FICHERO)
                    .isNotNull();
            assertThat(valor)
                    .as("%s lleva valor por defecto en %s: en producción, un secreto que falta "
                            + "tiene que impedir el arranque, no degradarse en silencio", clave, FICHERO)
                    .matches("\\$\\{[A-Z0-9_]+}");
        }
    }

    /**
     * La contraseña del administrador es el caso distinto: su ausencia no impide arrancar
     * —la API funciona sin panel de administración—, pero tampoco puede traer un literal,
     * porque entonces sería una cuenta de administración con credenciales publicadas.
     */
    @Test
    void laContrasenaDelAdminSaleDelEntornoYNuncaLlevaLiteral() throws IOException {
        String valor = cargar().getProperty("app.seed.admin.password");

        assertThat(valor).isEqualTo("${ADMIN_PASSWORD:}");
    }

    private Properties cargar() throws IOException {
        Properties propiedades = new Properties();
        try (InputStream entrada = new ClassPathResource(FICHERO).getInputStream()) {
            propiedades.load(entrada);
        }
        return propiedades;
    }
}
