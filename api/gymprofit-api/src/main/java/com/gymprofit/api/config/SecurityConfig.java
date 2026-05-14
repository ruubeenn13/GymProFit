package com.gymprofit.api.config;

import com.gymprofit.api.config.security.JwtAccessDenied;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static jakarta.servlet.DispatcherType.ERROR;
import static jakarta.servlet.DispatcherType.FORWARD;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final IUsuarioService usuarioService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtEntryPoint jwtEntryPoint;
    private final JwtAccessDenied jwtAccessDenied;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

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

                                // Swagger público
                                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()

                                // GUEST: solo GET en endpoints públicos
                                .requestMatchers(HttpMethod.GET, "/ejercicios/**").hasAnyRole(RoleType.GUEST.name(), RoleType.USER.name(), RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.GET, "/rutinas/**").hasAnyRole(RoleType.GUEST.name(), RoleType.USER.name(), RoleType.ADMIN.name())
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

                                // ADMIN: gestión completa de ejercicios
                                .requestMatchers(HttpMethod.POST, "/ejercicios/**").hasRole(RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.PUT, "/ejercicios/**").hasRole(RoleType.ADMIN.name())
                                .requestMatchers(HttpMethod.DELETE, "/ejercicios/**").hasRole(RoleType.ADMIN.name())

                                // ADMIN: gestión completa de usuarios
                                .requestMatchers("/usuarios/**").hasRole(RoleType.ADMIN.name())

                                .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider());

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}