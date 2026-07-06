package com.gymprofit.api.integration;

import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.entity.Role;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.Dificultad;
import com.gymprofit.api.enums.GrupoMuscular;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.repository.jpa.IRoleRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// AbstractOwnershipTest — base de los tests e2e de ownership (IDOR).
// Los controller-tests normales mockean el service, por lo que checkOwnership
// (SecurityUtils) nunca se ejecuta y el 403 IDOR real nunca se produce. Esta base
// levanta el contexto COMPLETO (sin mocks de service), siembra dos usuarios USER
// reales (owner + atacante) y expone helpers para:
//   - sembrar un recurso como su dueño real (runAs), fijando el SecurityContext;
//   - autenticar el request como el atacante con @WithUserDetails(setupBefore=TEST_EXECUTION)
//     en cada test, de modo que SecurityUtils.checkOwnership se ejecute de verdad y
//     ControllerExceptionHandler traduzca UnauthorizedException → 403.
// @Transactional: toda la siembra + el request se revierten al acabar cada test
// (no ensucia la BD dev/ci). Las subclases añaden la creación del recurso concreto.
// ============================================================
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class AbstractOwnershipTest {

    // Usernames fijos de los dos usuarios de prueba (rollback los libera tras cada test).
    protected static final String OWNER = "__idor_owner__";
    protected static final String ATTACKER = "__idor_attacker__";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IEjercicioRepository ejercicioRepository;

    @Autowired
    private IAlimentoRepository alimentoRepository;

    // Entidades sembradas, disponibles para las subclases.
    protected Usuario owner;
    protected Usuario attacker;

    // Crea los dos usuarios USER reales antes que la siembra de recursos de la subclase
    // (el @BeforeEach de la superclase corre primero). @WithUserDetails resuelve el
    // principal más tarde (setupBefore=TEST_EXECUTION), cuando ya existen en BD.
    @BeforeEach
    void baseSetup() {
        owner = crearUsuario(OWNER, RoleType.USER);
        attacker = crearUsuario(ATTACKER, RoleType.USER);
    }

    // Persiste un Usuario mínimo válido (mismo patrón que AuthService.register).
    protected Usuario crearUsuario(String username, RoleType tipo) {
        List<Role> roles = roleRepository.findByNombreIn(List.of(tipo.getValue()));
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("Test1234"));
        u.setEmail(username + "@test.local");
        u.setFechaRegistro(LocalDateTime.now());
        u.setActivo(true);
        u.setRoles(roles);
        return usuarioRepository.save(u);
    }

    // Crea un ejercicio del catálogo global. En CI la BD efímera solo trae el seed de
    // Flyway (roles + guest), NO el catálogo, así que hay que sembrarlo aquí (no valía
    // asumir que existía). Enums por values()[0] para no depender de constantes concretas.
    protected Ejercicio crearEjercicioCatalogo() {
        Ejercicio e = new Ejercicio();
        e.setNombre("Ejercicio IDOR test");
        e.setGrupoMuscular(GrupoMuscular.values()[0]);
        e.setDificultad(Dificultad.values()[0]);
        e.setCaloriasQuemadas(10);
        e.setActivo(true);
        return ejercicioRepository.save(e);
    }

    // Crea un alimento del catálogo global (mismo motivo: CI no lo trae sembrado).
    protected Alimento crearAlimentoCatalogo() {
        Alimento a = new Alimento();
        a.setNombre("Alimento IDOR test");
        a.setCalorias(100);
        a.setActivo(true);
        return alimentoRepository.save(a);
    }

    // Ejecuta un bloque con el SecurityContext fijado a un usuario, para sembrar
    // recursos "como su dueño" (los services fuerzan usuarioId = usuario autenticado).
    // Limpia el contexto al terminar; @WithUserDetails lo repuebla antes del test.
    protected void runAs(Usuario usuario, Runnable bloque) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            bloque.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
