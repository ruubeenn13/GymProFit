package com.gymprofit.api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// DtoSinSecretosEnToStringTest — GP-115
//
// Ningún DTO puede escribir en su toString una contraseña, un código de recuperación
// o un token. Con @Data, Lombok genera un toString con TODOS los campos, y un log de
// depuración, una excepción o un depurador lo imprimen sin que nadie lo haya decidido.
//
// Recorre todas las clases del paquete dto, no una lista escrita a mano: un DTO nuevo
// con un campo sensible queda cubierto sin tocar este test. Un campo es sensible por
// su nombre; si algún día uno lo es y no se llama así, hay que añadirlo al patrón.
// ============================================================
@DisplayName("GP-115 — ningún DTO imprime secretos en su toString")
class DtoSinSecretosEnToStringTest {

    private static final String PAQUETE = "com.gymprofit.api.dto";

    // password, currentPassword, newPassword, contrasena…, token, refreshToken, secret…
    // y «codigo», que en los DTO solo es el código de recuperación de ResetPasswordDTO.
    private static final Pattern SENSIBLE =
            Pattern.compile("(?i).*(password|contrasena|token|secret).*|codigo");

    // Campos que casan con el patrón y NO son secretos, uno a uno y con su clase: el
    // código de un programa o de una plantilla es su id público en el catálogo (GP-074).
    // Lista cerrada a propósito: un «codigo» nuevo en otro DTO sigue saltando.
    private static final Set<String> NO_SENSIBLES = Set.of(
            "com.gymprofit.api.dto.entity.programa.ProgramaDTO.codigo",
            "com.gymprofit.api.dto.entity.programa.RutinaConEjerciciosDTO.codigo");

    private static final String MARCA = "SECRETO-GP115-NO-DEBE-SALIR";

    @Test
    @DisplayName("ningún campo sensible aparece en el toString")
    void ningun_campo_sensible_en_to_string() throws Exception {
        List<String> fugas = new ArrayList<>();
        int revisados = 0;

        for (Class<?> clase : clasesDelPaquete()) {
            List<Field> sensibles = camposSensibles(clase);
            if (sensibles.isEmpty()) continue;

            Object instancia = instanciar(clase);
            for (Field campo : sensibles) {
                campo.setAccessible(true);
                campo.set(instancia, MARCA);
            }
            revisados++;

            if (String.valueOf(instancia).contains(MARCA)) {
                fugas.add(clase.getSimpleName() + " " + sensibles.stream().map(Field::getName).toList());
            }
        }

        // Si el escaneo no encontrara nada, el test pasaría sin comprobar nada.
        assertThat(revisados).as("DTO con campos sensibles revisados").isGreaterThanOrEqualTo(10);
        assertThat(fugas).as("DTO que imprimen un secreto en su toString").isEmpty();
    }

    private static List<Class<?>> clasesDelPaquete() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider escaner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(
                            org.springframework.beans.factory.annotation.AnnotatedBeanDefinition definicion) {
                        return definicion.getMetadata().isConcrete();
                    }
                };
        escaner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));

        List<Class<?>> clases = new ArrayList<>();
        for (BeanDefinition definicion : escaner.findCandidateComponents(PAQUETE)) {
            clases.add(Class.forName(definicion.getBeanClassName()));
        }
        return clases;
    }

    private static List<Field> camposSensibles(Class<?> clase) {
        List<Field> campos = new ArrayList<>();
        for (Class<?> c = clase; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field campo : c.getDeclaredFields()) {
                if (campo.getType() == String.class
                        && !Modifier.isStatic(campo.getModifiers())
                        && SENSIBLE.matcher(campo.getName()).matches()
                        && !NO_SENSIBLES.contains(c.getName() + "." + campo.getName())) {
                    campos.add(campo);
                }
            }
        }
        return campos;
    }

    // Un DTO con un campo sensible y sin constructor vacío no se puede revisar así:
    // mejor que el test falle y alguien lo mire que dejarlo fuera en silencio.
    private static Object instanciar(Class<?> clase) throws Exception {
        Constructor<?> vacio = clase.getDeclaredConstructor();
        vacio.setAccessible(true);
        return vacio.newInstance();
    }
}
