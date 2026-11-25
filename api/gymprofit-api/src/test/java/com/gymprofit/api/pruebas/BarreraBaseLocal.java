package com.gymprofit.api.pruebas;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

import javax.sql.DataSource;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// ============================================================
// BarreraBaseLocal — ningún test se conecta a una base que no sea la de este equipo
// o la del CI (GP-282).
//
// Durante GP-273 las pruebas corrieron contra el clúster de producción: bastó con que
// SPRING_DATASOURCE_URL apuntara fuera para que cada @SpringBootTest migrara, sembrara y
// consultara allí, y se fueron unas 858.000 RU en una tarde. Esta barrera lo impide.
//
// Cómo entra en todos los contextos sin que ningún test la importe: va registrada dos
// veces en src/test/resources/META-INF/spring.factories. Como ContextCustomizerFactory,
// el framework de test la aplica a cada contexto de test (también a los slices); como
// ApplicationContextInitializer, SpringApplication la aplica a cada aplicación que se
// arranque con el classpath de test, también las que un test levanta a mano con
// SpringApplicationBuilder (TareasUnaVezConDosInstanciasTest), que no pasan por el
// framework de test. Las dos añaden el mismo BeanPostProcessor que mira el DataSource ANTES de inicializarlo:
// Hikari no abre ninguna conexión hasta el primer getConnection(), así que la URL se
// rechaza sin haber tocado la red. Se mira en el bean y no solo en el entorno porque
// así cuentan también los @DynamicPropertySource, que llegan después de preparar el
// entorno. De paso se mira spring.flyway.url, que abriría su propia conexión.
//
// Hosts admitidos: localhost, 127.0.0.1 y ::1. La MariaDB del CI es un service container
// publicado en 127.0.0.1:3306 (application-ci.properties), así que entra en la misma
// regla. No hay forma de saltársela: si un test necesita otra base, se habla antes.
// ============================================================
public final class BarreraBaseLocal {

    /** Los únicos hosts a los que un test puede conectarse. */
    static final Set<String> HOSTS_LOCALES = Set.of("localhost", "127.0.0.1", "::1");

    private static final List<String> PREFIJOS = List.of("jdbc:mariadb:", "jdbc:mysql:");

    private BarreraBaseLocal() {
    }

    /**
     * Comprueba que una URL JDBC apunta solo a hosts locales.
     * <p>
     * Se analiza la parte de hosts ({@code //h1:p1,h2:p2/base}) y cada host tiene que ser
     * uno de {@link #HOSTS_LOCALES}, escrito tal cual: {@code localhost.ejemplo.com} no es
     * {@code localhost}. Lo que no se sabe leer se rechaza, no se deja pasar.
     *
     * @param url    la URL JDBC tal como la tiene el contexto
     * @param origen de dónde sale (propiedad o bean), para el mensaje
     * @throws IllegalStateException si la URL no es local, o no se entiende
     */
    public static void comprobar(String url, String origen) {
        if (url == null || url.isBlank()) {
            return; // sin URL no hay conexión que impedir; Spring fallará por su cuenta
        }
        List<String> hosts;
        try {
            hosts = hosts(url);
        } catch (IllegalArgumentException e) {
            throw rechazo(origen, e.getMessage());
        }
        for (String host : hosts) {
            if (!HOSTS_LOCALES.contains(host)) {
                throw rechazo(origen, "el host «" + host + "» no es local");
            }
        }
    }

    /**
     * Saca los hosts de una URL JDBC de MariaDB o MySQL, en minúsculas y sin puerto.
     *
     * @param url la URL JDBC
     * @return los hosts, al menos uno
     * @throws IllegalArgumentException si la URL no tiene la forma esperada
     */
    static List<String> hosts(String url) {
        String u = url.trim();
        String prefijo = PREFIJOS.stream().filter(u.toLowerCase(Locale.ROOT)::startsWith).findFirst()
                .orElseThrow(() -> ilegible("solo se admiten jdbc:mariadb: y jdbc:mysql:"));
        int barras = u.indexOf("//", prefijo.length());
        if (barras < 0) {
            throw ilegible("no tiene la parte //host");
        }
        String resto = u.substring(barras + 2);
        int fin = resto.length();
        for (char c : new char[]{'/', '?'}) {
            int i = resto.indexOf(c);
            if (i >= 0 && i < fin) fin = i;
        }
        String parteHosts = resto.substring(0, fin);
        if (parteHosts.isBlank()) {
            throw ilegible("no dice a qué host conecta");
        }
        return java.util.Arrays.stream(parteHosts.split(","))
                .map(BarreraBaseLocal::sinPuerto)
                .toList();
    }

    // "h:3306" → "h"; "[::1]:3306" → "::1". La forma address=(host=...) no se lee: se rechaza.
    private static String sinPuerto(String hostPuerto) {
        String h = hostPuerto.trim().toLowerCase(Locale.ROOT);
        if (h.isEmpty() || h.contains("(") || h.contains("@")) {
            throw ilegible("el host «" + hostPuerto + "» no tiene una forma que la barrera sepa leer");
        }
        if (h.startsWith("[")) {
            int cierre = h.indexOf(']');
            if (cierre < 0) throw ilegible("IPv6 sin cerrar");
            return h.substring(1, cierre);
        }
        int dosPuntos = h.indexOf(':');
        return dosPuntos >= 0 ? h.substring(0, dosPuntos) : h;
    }

    private static IllegalArgumentException ilegible(String motivo) {
        return new IllegalArgumentException(motivo);
    }

    private static IllegalStateException rechazo(String origen, String motivo) {
        return new IllegalStateException(
                "BarreraBaseLocal (GP-282): los tests no arrancan porque " + origen + " apunta fuera de "
                        + "este equipo (" + motivo + "). Los tests solo se conectan a la MariaDB local "
                        + "(localhost) o a la del CI (127.0.0.1); nunca a la base de producción. Revisa "
                        + "SPRING_DATASOURCE_URL y SPRING_PROFILES_ACTIVE en el entorno del comando.");
    }

    /**
     * Mira el DataSource y las URLs del entorno antes de que nada abra una conexión.
     *
     * @param entorno el entorno del contexto, ya completo cuando se crea el DataSource
     * @return el postprocesador que hace la comprobación
     */
    static BeanPostProcessor postprocesador(Environment entorno) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String nombre) {
                if (!(bean instanceof DataSource)) return bean;
                comprobar(entorno.getProperty("spring.datasource.url"), "spring.datasource.url");
                comprobar(entorno.getProperty("spring.flyway.url"), "spring.flyway.url");
                if (bean instanceof HikariDataSource hikari) {
                    comprobar(hikari.getJdbcUrl(), "el DataSource «" + nombre + "»");
                } else {
                    throw rechazo("el DataSource «" + nombre + "»",
                            "es un " + bean.getClass().getName() + " y la barrera solo sabe leer Hikari");
                }
                return bean;
            }
        };
    }

    /** Registrada en META-INF/spring.factories: se aplica a cada contexto de test. */
    public static final class Fabrica implements ContextCustomizerFactory {
        @Override
        public ContextCustomizer createContextCustomizer(Class<?> testClass,
                                                         List<ContextConfigurationAttributes> configAttributes) {
            return new Customizer();
        }
    }

    /** Registrado en META-INF/spring.factories: se aplica a cada SpringApplication del classpath de test. */
    public static final class Inicializador implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext contexto) {
            contexto.getBeanFactory().addBeanPostProcessor(postprocesador(contexto.getEnvironment()));
        }
    }

    /** Sin estado: todos son iguales, para no romper la caché de contextos. */
    static final class Customizer implements ContextCustomizer {
        @Override
        public void customizeContext(ConfigurableApplicationContext contexto, MergedContextConfiguration config) {
            contexto.getBeanFactory().addBeanPostProcessor(postprocesador(contexto.getEnvironment()));
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Customizer;
        }

        @Override
        public int hashCode() {
            return Customizer.class.hashCode();
        }
    }
}
