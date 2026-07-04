package com.gymprofit.api.config;

import com.gymprofit.api.config.security.JwtAccessDenied;
import com.gymprofit.api.config.security.AuthRateLimitFilter;
import com.gymprofit.api.config.security.JwtAuthenticationFilter;
import com.gymprofit.api.config.security.JwtEntryPoint;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.service.usuario.IUsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static jakarta.servlet.DispatcherType.ERROR;
import static jakarta.servlet.DispatcherType.FORWARD;

// ============================================================
// SecurityConfig — configuración central de Spring Security de la API
// Define el filtro de seguridad (JWT stateless, sin sesiones), CORS,
// el proveedor de autenticación y las reglas de autorización por rol
// (GUEST/USER/ADMIN) para cada endpoint de GymProFit.
// ============================================================
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final IUsuarioService usuarioService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtEntryPoint jwtEntryPoint;
    private final JwtAccessDenied jwtAccessDenied;
    // Limitador de peticiones para /auth/** (anti brute-force/spam), se ejecuta antes del filtro JWT.
    private final AuthRateLimitFilter authRateLimitFilter;

    // Orígenes permitidos por CORS (lista blanca, separada por comas), configurable por entorno.
    // Por defecto solo orígenes de desarrollo local; en producción se define el dominio web real.
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    // Bean del codificador de contraseñas usado al registrar y autenticar usuarios.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Expone el AuthenticationManager de Spring Security para usarlo en el login (AuthService).
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Proveedor de autenticación DAO: usa IUsuarioService para cargar el usuario y el encoder para comparar contraseñas.
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Configuración CORS: lista blanca de orígenes (no wildcard) y los métodos/cabeceras necesarios.
    // El cliente Android nativo no usa CORS (es cosa de navegadores); esto protege a un futuro frontend web.
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split("\\s*,\\s*")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Cadena de filtros HTTP: desactiva CSRF (API stateless con JWT), configura manejo de
    // excepciones de autenticación/autorización, sesiones sin estado y las reglas de acceso
    // por endpoint y rol; añade el filtro JWT antes del filtro estándar de usuario/contraseña.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtEntryPoint)
                        .accessDeniedHandler(jwtAccessDenied))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth.dispatcherTypeMatchers(FORWARD, ERROR).permitAll()

                                // Endpoints públicos de autenticación
                                // Sin /api/ porque Spring Security evalúa sin el context-path
                                .requestMatchers("/auth/**").permitAll()

                                // Health-check público para la PaaS (Render/Koyeb).
                                // Sin context-path porque Spring Security evalúa sin /api.
                                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                                // Swagger público
                                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()

                                // GUEST: solo GET en endpoints públicos
                                .requestMatchers(HttpMethod.GET, "/ejercicios/**").hasAnyRole(RoleType.GUEST.name(), RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/rutinas/**").hasAnyRole(RoleType.GUEST.name(), RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/alimentos/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.DELETE, "/alimentos/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.PATCH, "/alimentos/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.PUT, "/alimentos/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/alimentos/**").hasAnyRole(RoleType.GUEST.name(), RoleType.USER.name(), RoleType.ADMIN.name())

                                // JOOQ ejercicios - accesibles por todos los roles
                                .requestMatchers(HttpMethod.GET, "/jooq/ejercicios/**").hasAnyRole(RoleType.GUEST.name(), RoleType.USER.name(), RoleType.ADMIN.name())

                                // JOOQ usuarios - solo ADMIN
                                .requestMatchers("/jooq/usuarios/**").hasRole(RoleType.ADMIN.name())

                                // USER: puede consultar y actualizar su propio perfil
                                // IMPORTANTE: estas reglas van ANTES de la regla general de /usuarios/**
                                .requestMatchers(HttpMethod.GET, "/usuarios/username/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/usuarios/{id}").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.PUT, "/usuarios/{id}").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.PATCH, "/usuarios/{id}").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/usuarios/{id}/foto").hasAnyRole(RoleType.GUEST.name(), RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.POST, "/usuarios/{id}/foto").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/usuarios/{id}/estadisticas").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())

                                // USER: gestión de sus propios datos
                                .requestMatchers("/sesiones/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers("/ejercicios-realizados/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers("/mediciones-corporales/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers("/objetivos-personales/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers("/progreso-ejercicios/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers("/notificaciones/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers("/comidas/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers("/alimentos-comida/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers("/rutinas-ejercicios/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())

                                // USER: puede gestionar sus propias rutinas (validación de propiedad en RutinaService)
                                .requestMatchers(HttpMethod.POST, "/rutinas/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.PUT, "/rutinas/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.DELETE, "/rutinas/**").hasAnyRole(RoleType.USER.name(), RoleType.ADMIN.name())

                                // GUEST/USER/ADMIN: consulta de logros
                                .requestMatchers(HttpMethod.GET, "/logros/**").hasAnyRole(RoleType.GUEST.name(), RoleType.USER.name(), RoleType.ADMIN.name())

                                // ADMIN: gestión de logros
                                .requestMatchers(HttpMethod.POST, "/logros/**").hasRole(RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.PUT, "/logros/**").hasRole(RoleType.ADMIN.name())

                                // ADMIN: gestión completa de ejercicios
                                .requestMatchers(HttpMethod.POST, "/ejercicios/**").hasRole(RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.PUT, "/ejercicios/**").hasRole(RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.DELETE, "/ejercicios/**").hasRole(RoleType.ADMIN.name())

                                // ADMIN: gestión completa de usuarios
                                .requestMatchers("/usuarios/**").hasRole(RoleType.ADMIN.name())

                                // ADMIN: panel de administración
                                .requestMatchers("/admin/**").hasRole(RoleType.ADMIN.name())

                                .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider());

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        // El limitador va antes del filtro JWT: frena el spam/fuerza bruta en /auth/** antes de tocar auth.
        http.addFilterBefore(authRateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}