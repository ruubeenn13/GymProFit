package com.gymprofit.api.pruebas;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// ============================================================
// ContadorSentencias — cuenta las sentencias SQL que llegan a la base (GP-168)
// Envuelve el DataSource del contexto de test: cada prepareStatement, prepareCall o
// createStatement de una conexión cuenta como una sentencia, venga de Hibernate o de
// JdbcTemplate. Sin dependencias: un proxy dinámico de java.sql.
// Se importa con @Import(ContadorSentencias.class) y se lee con empezar()/sentencias().
// ============================================================
@TestConfiguration
public class ContadorSentencias {

    private static final List<String> SENTENCIAS = Collections.synchronizedList(new ArrayList<>());
    private static volatile boolean contando;

    /** Pone el contador a cero y empieza a contar. */
    public static void empezar() {
        SENTENCIAS.clear();
        contando = true;
    }

    /** Deja de contar y devuelve las sentencias vistas desde empezar(). */
    public static List<String> sentencias() {
        contando = false;
        synchronized (SENTENCIAS) {
            return List.copyOf(SENTENCIAS);
        }
    }

    @Bean
    static BeanPostProcessor envolverDataSource() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String nombre) {
                if (!(bean instanceof DataSource ds) || bean instanceof Proxy) return bean;
                return Proxy.newProxyInstance(DataSource.class.getClassLoader(), new Class<?>[]{DataSource.class},
                        manejador(ds, (proxy, m, args) -> {
                            Object r = m.invoke(ds, args);
                            return r instanceof Connection c ? conexion(c) : r;
                        }));
            }
        };
    }

    private static Connection conexion(Connection real) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                manejador(real, (proxy, m, args) -> {
                    String n = m.getName();
                    if (contando && (n.equals("prepareStatement") || n.equals("prepareCall") || n.equals("createStatement"))) {
                        SENTENCIAS.add(args != null && args.length > 0 && args[0] instanceof String s ? s : n);
                    }
                    return m.invoke(real, args);
                }));
    }

    // Desenvuelve InvocationTargetException para que el llamante vea la SQLException original.
    private static InvocationHandler manejador(Object real, InvocationHandler h) {
        return (proxy, m, args) -> {
            if (m.getName().equals("unwrap") && args != null && args[0] instanceof Class<?> k && k.isInstance(real)) {
                return real;
            }
            try {
                return h.invoke(proxy, m, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        };
    }
}
