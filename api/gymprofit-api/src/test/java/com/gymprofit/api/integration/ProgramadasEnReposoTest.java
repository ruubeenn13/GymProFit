package com.gymprofit.api.integration;

import com.gymprofit.api.dto.entity.notificacion.NotificacionCreateDTO;
import com.gymprofit.api.entity.Role;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.enums.TipoNotificacion;
import com.gymprofit.api.repository.jpa.INotificacionRepository;
import com.gymprofit.api.repository.jpa.IRoleRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.notificacion.INotificacionService;
import com.gymprofit.api.service.notificacion.VigiaProgramadas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ============================================================
// ProgramadasEnReposoTest — una notificación programada creada por la API sigue saliendo
// en su minuto aunque el barrido ya no vaya a la base cada minuto (GP-283).
//
// Sin @Transactional a propósito: el aviso a ProximaProgramada se da al confirmar la
// transacción de NotificacionService.save, y un test transaccional no confirma nunca. Por
// eso borra a mano lo que crea.
// ============================================================
@SpringBootTest
@DisplayName("Programadas en reposo — la creada por la API sale sin esperar a la relectura")
class ProgramadasEnReposoTest {

    private static final String USUARIO = "__gp283_programada__";

    @Autowired private INotificacionService notificacionService;
    @Autowired private INotificacionRepository notificacionRepository;
    @Autowired private IUsuarioRepository usuarioRepository;
    @Autowired private IRoleRepository roleRepository;
    @Autowired private VigiaProgramadas vigia;

    private Usuario usuario;

    @AfterEach
    void limpiar() {
        if (usuario != null) {
            notificacionRepository.deleteAll(notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuario.getId()));
            usuarioRepository.deleteById(usuario.getId());
        }
    }

    @Test
    @DisplayName("creada con fecha vencida → el siguiente minuto la manda")
    void creadaPorLaApiSale() {
        usuario = crearUsuario();
        vigia.comprobar(); // relectura hecha: durante 15 min no vuelve a leer por su cuenta

        Integer id = comoUsuario(() -> {
            NotificacionCreateDTO dto = new NotificacionCreateDTO();
            dto.setUsuarioId(usuario.getId());
            dto.setTitulo("Programada GP-283");
            dto.setMensaje("Debe salir en su minuto");
            dto.setTipo(TipoNotificacion.RECORDATORIO.name());
            dto.setFechaProgramada(LocalDateTime.now().minusSeconds(5));
            return notificacionService.save(dto).getId();
        });

        vigia.comprobar();

        assertThat(notificacionRepository.findById(id).orElseThrow().getPushEnviada())
                .as("la push de la programada vencida se ha mandado")
                .isTrue();
    }

    private Usuario crearUsuario() {
        List<Role> roles = roleRepository.findByNombreIn(List.of(RoleType.USER.getValue()));
        Usuario u = new Usuario();
        u.setUsername(USUARIO);
        u.setPassword("sin-uso");
        u.setEmail(USUARIO + "@test.local");
        u.setFechaRegistro(LocalDateTime.now());
        u.setActivo(true);
        u.setRoles(roles);
        return usuarioRepository.save(u);
    }

    private <T> T comoUsuario(java.util.function.Supplier<T> bloque) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
        try {
            return bloque.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
