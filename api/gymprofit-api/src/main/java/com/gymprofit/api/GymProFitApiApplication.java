package com.gymprofit.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// ============================================================
// GymProFitApiApplication — punto de entrada de la API GymProFit
// Clase principal que arranca el contexto de Spring Boot y toda
// la configuración autoconfigurada (seguridad, JPA, Flyway, etc.).
// @EnableScheduling activa las tareas programadas (@Scheduled), como
// la purga periódica de refresh tokens caducados/revocados.
// ============================================================
@SpringBootApplication
@EnableScheduling
public class GymProFitApiApplication {

	// Método main: lanza la aplicación Spring Boot.
	public static void main(String[] args) {
		SpringApplication.run(GymProFitApiApplication.class, args);
	}

}
