package com.gymprofit.api.service.admin;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.admin.AdminCuentaDTO;
import com.gymprofit.api.dto.admin.AdminCuentaDetalleDTO;
import com.gymprofit.api.dto.admin.BorrarCuentaAdminDTO;
import com.gymprofit.api.dto.common.PageDTO;
import com.gymprofit.api.entity.Role;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.exceptions.ConflictEntityException;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.repository.jpa.IComidaRepository;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.usuario.IBorradoCuentaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Locale;

// ============================================================
// AdminCuentaService — las cuentas en la web de administración (GP-085)
//
// Lee y borra; activar, desactivar y cambiar el rol siguen en las rutas que ya
// existían. Ninguna respuesta lleva datos de salud.
//
// El borrado a petición es el de GP-008 (BorradoCuentaService), con dos cerrojos
// que el del titular no necesita: no se borra la propia cuenta —un administrador
// que se borra a sí mismo por error deja el producto sin administración— ni otra
// cuenta ADMIN, que se baja de rol antes a propósito y a la vista.
// ============================================================
@Service
@RequiredArgsConstructor
public class AdminCuentaService implements IAdminCuentaService {

    private static final Logger logger = LoggerFactory.getLogger(AdminCuentaService.class);

    private static final int TAMANO_MAXIMO = 100;

    private final IUsuarioRepository usuarioRepository;
    private final ISesionEntrenamientoRepository sesionRepository;
    private final IComidaRepository comidaRepository;
    private final IBorradoCuentaService borradoCuentaService;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public PageDTO<AdminCuentaDTO> listar(String q, String rol, Boolean activo, int page, int size) {
        String patron = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        PageRequest pagina = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), TAMANO_MAXIMO),
                Sort.by(Sort.Order.desc("fechaRegistro"), Sort.Order.desc("id")));
        Page<Usuario> cuentas = usuarioRepository.buscarParaAdmin(patron, rol(rol), activo, pagina);
        return PageDTO.of(cuentas, cuentas.getContent().stream().map(AdminCuentaService::aDTO).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCuentaDetalleDTO detalle(Integer id) {
        Usuario usuario = buscar(id);
        return new AdminCuentaDetalleDTO(aDTO(usuario),
                sesionRepository.countByUsuarioId(id), comidaRepository.countByUsuarioId(id));
    }

    @Override
    @Transactional
    public void borrarAPeticion(Integer id, BorrarCuentaAdminDTO dto) {
        Usuario usuario = buscar(id);
        Integer adminId = securityUtils.getCurrentUserId();

        if (usuario.getId().equals(adminId)) {
            throw new ConflictEntityException("error.admin.borrarPropia");
        }
        if (RoleType.ADMIN.name().equals(rolPrincipal(usuario))) {
            throw new ConflictEntityException("error.admin.borrarAdmin");
        }
        if (!usuario.getUsername().equals(dto.getConfirmacion().trim())) {
            throw new InvalidDataException("error.admin.confirmacion");
        }

        // Solo ids y el motivo: ni el usuario ni el correo de la cuenta que se va.
        logger.info("Borrado a petición del titular: cuenta id={}, por el administrador id={}, motivo: {}",
                id, adminId, dto.getMotivo().trim());
        borradoCuentaService.borrarCuenta(id);
    }

    private Usuario buscar(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("error.usuario.noExiste", id));
    }

    private static RoleType rol(String rol) {
        if (rol == null || rol.isBlank()) return null;
        try {
            return RoleType.valueOf(rol.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("error.rol.invalido", rol);
        }
    }

    // Una cuenta tiene un rol; si tuviera varios, manda el de más permisos.
    private static String rolPrincipal(Usuario usuario) {
        return usuario.getRoles().stream()
                .map(Role::getNombre)
                .min(Comparator.comparingInt(r -> switch (r) {
                    case ADMIN -> 0;
                    case USER -> 1;
                    default -> 2;
                }))
                .map(Enum::name)
                .orElse(null);
    }

    private static AdminCuentaDTO aDTO(Usuario u) {
        return new AdminCuentaDTO(u.getId(), u.getUsername(), u.getEmail(), u.getFechaRegistro(),
                u.getUltimoAcceso(), rolPrincipal(u), u.isEnabled());
    }
}
