package com.gymprofit.api.service;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.usuario.UsuarioCreateDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioDTO;
import com.gymprofit.api.entity.Role;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.exceptions.DuplicateEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.mappers.UsuarioMapper;
import com.gymprofit.api.repository.jpa.IRoleRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.usuario.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del UsuarioService.
 *
 * Un test unitario comprueba que una clase finciona correctamente de forma aislada,
 * sin necesidad de arrancar la base de datos ni el servidor.
 *
 * Para ello usamos dos herramientas:
 *     - JUnit 5: el framework que define y ejecuta los tests.
 *     - Mockito: nos permite crear "mocks" (objetos falsos) que simulan el
 *     comportamiento del repository y el mapper sin tocar la BD real
 */
// ============================================================
// UsuarioServiceTest — pruebas unitarias del UsuarioService
// Verifica CRUD de usuarios, validación de duplicados (username/email)
// y activación/desactivación, usando mocks del repositorio y del mapper.
// ============================================================
@ExtendWith(MockitoExtension.class) // Le decimos a JUnit que use Mockito
@DisplayName("Tests del UsuarioService") // Nombre que aparece en el panel de resultados
class UsuarioServiceTest {

    /**
     * @Mock crea un objeto falso del repository.
     * Cuando el service llame a usuarioRepository.findById(1),
     * nosotros le diremos qué debe devolver sin consultar la BD.
     */
    @Mock
    private IUsuarioRepository usuarioRepository;

    /** @Mock del repositorio de roles: lo usa cambiarRol para resolver el rol destino. */
    @Mock
    private IRoleRepository roleRepository;

    /**
     * @Mock crea un objeto falso del mapper.
     * Cuando el service llame a usuarioMapper.toDTO(usuario),
     * nosotros contorlamos qué devuelve.
     */
    @Mock
    private UsuarioMapper usuarioMapper;

    /**
     * @Mock de SecurityUtils. En las lecturas/borrados el service llama a
     * checkOwnership(id), que con el mock queda como no-op (método void).
     */
    @Mock
    private SecurityUtils securityUtils;

    /**
     * @InjectMocks crea el UsuarioService REAL pero inyectándole
     * los mocks de arriba en vez de los beans reales de Spring.
     * Así probamos la lógica del service de forma aislada.
     */
    @InjectMocks
    private UsuarioService usuarioService;

    // Objetos de prueba que reutilizamos en todos los tests.
    private Usuario usuario;
    private UsuarioDTO usuarioDTO;
    private UsuarioCreateDTO usuarioCreateDTO;

    /**
     * @BeforeEach se ejecuta ANTES de cada test.
     * Aquí preparamos los datos de prueba para que cada test
     * empiece con un estado limpio y conocido.
     */
    @BeforeEach
    void setup() {
        // Creamos un usuario de prueba simulando lo que devolvería la BD
        usuario = new Usuario();
        usuario.setId(1);
        usuario.setUsername("testuser");
        usuario.setEmail("test@gymprofit.com");
        usuario.setActivo(true);

        // Crea,ps eñ DTP que simula lo que devolvería el mapper
        usuarioDTO = new UsuarioDTO();
        usuarioDTO.setId(1);
        usuarioDTO.setUsername("testuser");
        usuarioDTO.setEmail("test@gymprofit.com");

        // Creamos el DTO de creación que simula lo que manda el cliente
        usuarioCreateDTO = new UsuarioCreateDTO();
        usuarioCreateDTO.setUsername("newuser");
        usuarioCreateDTO.setEmail("newuser@gymprofit.com");
        usuarioCreateDTO.setPassword("password123");
    }

    @Test
    @DisplayName("findAll devuelve lista de usuarios")
    void findAll_devuelve_lista() {
        // GIVEN (dado que...) - configuramos los mocks para simular la BD
        // Cuando el service llame a findAll(), el mock devuelve una lista con un usuario
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));
        // Cuando el service llame a toDTOList(), el mock devuelve una lista con un DTO
        when(usuarioMapper.toDTOList(any())).thenReturn(List.of(usuarioDTO));

        // WHEN (cuando...) - ejecutamos el método que queremos probar
        List<UsuarioDTO> result = usuarioService.findAll();

        // THEN (entonces...) - verificamos que el resultado es el esperado
        assertNotNull(result);                          // La lista no debe ser null
        assertEquals(1, result.size());        // Debe tener exactamente 1 elemento
        verify(usuarioRepository).findAll();            // Verificamos que se llamó al repository
    }

    @Test
    @DisplayName("findById con id existente devuelve UsuarioDTO")
    void findById_existente_devuelve_dto() {
        // Simulamos que el repository encuentra el usuario con id=1
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        // Simulamos que el mapper convierte el usuario a DTO
        when(usuarioMapper.toDTO(usuario)).thenReturn(usuarioDTO);

        // Ejecutamos el método
        UsuarioDTO result = usuarioService.findById(1);

        // Verificamos que el resultado tiene los datos correctos
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("findById con id inexistente lanza NotFoundEntityException")
    void findById_inexistente_lanza_excepcion() {
        // Simulamos que el repository NO encuentra ningún usuario con id=99
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        // Verificamos que el service lanza la excepción correcta
        // assertThrows comprueba que el método lanza exactamente esa excepción
        assertThrows(NotFoundEntityException.class, () -> usuarioService.findById(99));
    }

    @Test
    @DisplayName("save correcto guarda y devuelve UsuarioDTO")
    void save_correcto_guarda_usuario() {
        // Simulamos que el username y el email NO existen aún en la BD
        when(usuarioRepository.existsByUsername("newuser")).thenReturn(false);
        when(usuarioRepository.existsByEmail("newuser@gymprofit.com")).thenReturn(false);
        // Simulamos que el mapper convierte el createDTO a entidad
        when(usuarioMapper.toEntity(usuarioCreateDTO)).thenReturn(usuario);
        // Simulamos que el repository gharda y devuelve el usuario
        when(usuarioRepository.save(any())).thenReturn(usuario);
        // Simulamos que el mapper convierte la entidd guardada a DTO
        when(usuarioMapper.toDTO(usuario)).thenReturn(usuarioDTO);

        // Ejecutamos el método y verificamos que no lanza ninguna excepción
        UsuarioDTO result = usuarioService.save(usuarioCreateDTO);

        assertNotNull(result);
        // Verificamos que save() fue llamado exactamente una vez
        verify(usuarioRepository).save(any());
    }

    @Test
    @DisplayName("save con username duplicado lanza DuplicateEntityException")
    void save_username_duplicado_lanza_excepcion() {
        // Simulamos que el username YA existe en la BD
        when(usuarioRepository.existsByUsername("newuser")).thenReturn(true);

        // Verificamos que lanza la excepción de duplicado
        assertThrows(DuplicateEntityException.class, () -> usuarioService.save(usuarioCreateDTO));

        // never() verifica que save() NUNCA fue llamado (no debe guardar nada)
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("save con email duplicado lanza DuplicateEntityException")
    void save_email_duplicado_lanza_excepcion() {
        // El username no existe pero el email sí
        when(usuarioRepository.existsByUsername("newuser")).thenReturn(false);
        when(usuarioRepository.existsByEmail("newuser@gymprofit.com")).thenReturn(true);

        // Verificamos que lanza la excepción de duplicado
        assertThrows(DuplicateEntityException.class, () -> usuarioService.save(usuarioCreateDTO));

        // Verificamos que no se intentó guardar nada
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteById desactiva el usuario correctamente")
    void deleteById_desactiva_usuario() {
        // Simulamos que el usuario existe
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        // Verificamos que no lanza ninguna excepción al eliminar
        assertDoesNotThrow(() -> usuarioService.deleteById(1));

        // Verificamos que el usaurio quedó desactivado (activo = false)
        assertFalse(usuario.getActivo());
        // Verificamos que se llamó a save para persistir el cambio
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("deleteById con id inexistente landa NotFoundEntityException")
    void deleteById_inexistente_lanza_excepcion() {
        // Simulamos que el usuario NO existe
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        // Verificamos que lanza la exepción correcta
        assertThrows(NotFoundEntityException.class, () -> usuarioService.deleteById(99));
    }

    @Test
    @DisplayName("findByUsername con username existente devuelve UsuarioDTO")
    void findByUsername_existente_devuelve_dto() {
        // Simulamos que el repository encuentra el usuario por username
        when(usuarioRepository.findByUsername("testuser")).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toDTO(usuario)).thenReturn(usuarioDTO);

        UsuarioDTO result = usuarioService.findByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("findByUsername con username inexistente lanza NotFoundEntityException")
    void findByUsername_inexistente_lanza_excepcion() {
        // Simulamos que NO existe ningún usuario con ese username
        when(usuarioRepository.findByUsername("noexiste")).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> usuarioService.findByUsername("noexiste"));
    }

    @Test
    @DisplayName("activateById activa el usuario correctamente")
    void activateById_activa_usuario() {
        // El usuario empieza desactivado
        usuario.setActivo(false);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        // Ejecutaos la activación
        assertDoesNotThrow(() -> usuarioService.activateById(1));

        // Verificamos que el usuario quedó activado (activo = true)
        assertTrue(usuario.getActivo());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("findActivos deuelve solo usuarios activos")
    void findActivos_devuelve_activos() {
        // Simulamos que el repository devuelve solo usuarios activos
        when(usuarioRepository.findByActivoTrue()).thenReturn(List.of(usuario));
        when(usuarioMapper.toDTOList(any())).thenReturn(List.of(usuarioDTO));

        List<UsuarioDTO> result = usuarioService.findActivos();

        assertNotNull(result);
        assertEquals(1, result.size());
        // Verificamos que se usó el método correcto del repository
        verify(usuarioRepository).findByActivoTrue();
    }

    @Test
    @DisplayName("cambiarRol asigna el rol con una lista MUTABLE (si no, Hibernate revienta)")
    void cambiarRol_usa_lista_mutable() {
        // Regresión real: con List.of(role) el endpoint devolvía 500 en producción, porque el
        // merge de Hibernate llama a clear() sobre la colección y las de List.of son inmutables
        // (UnsupportedOperationException en CollectionType.replaceElements).
        Role rolAdmin = new Role();
        rolAdmin.setNombre(RoleType.ADMIN);
        usuario.setRoles(new ArrayList<>());
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(roleRepository.findByNombre(RoleType.ADMIN)).thenReturn(Optional.of(rolAdmin));

        usuarioService.cambiarRol(1, "admin");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        List<Role> roles = captor.getValue().getRoles();
        assertEquals(1, roles.size());
        assertEquals(RoleType.ADMIN, roles.get(0).getNombre());
        // La comprobación que importa: la colección tiene que poder vaciarse.
        assertDoesNotThrow(roles::clear);
    }
}
