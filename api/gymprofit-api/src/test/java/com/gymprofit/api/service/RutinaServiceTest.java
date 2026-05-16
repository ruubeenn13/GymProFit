package com.gymprofit.api.service;

import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaDTO;
import com.gymprofit.api.entity.Role;
import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.Nivel;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.mappers.RutinaMapper;
import com.gymprofit.api.repository.jpa.IRutinaRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.rutina.RutinaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del RutinaService")
class RutinaServiceTest {

    @Mock
    private IRutinaRepository rutinaRepository;

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private RutinaMapper rutinaMapper;

    @InjectMocks
    private RutinaService rutinaService;

    private Rutina rutina;
    private RutinaDTO rutinaDTO;
    private RutinaCreateDTO rutinaCreateDTO;
    private Usuario usuario;

    @BeforeEach
    void setup() {
        usuario = new Usuario();
        usuario.setId(1);
        usuario.setUsername("testuser");
        usuario.setRoles(List.of(new Role(1, RoleType.USER)));

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(usuario, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        rutina = new Rutina();
        rutina.setId(1);
        rutina.setNombre("Rutina Pecho");
        rutina.setNivel(Nivel.INTERMEDIO);
        rutina.setActiva(true);
        rutina.setUsuario(usuario);

        rutinaDTO = new RutinaDTO();
        rutinaDTO.setId(1);
        rutinaDTO.setNombre("Rutina Pecho");
        rutinaDTO.setNivel("INTERMEDIO");
        rutinaDTO.setActiva(true);

        rutinaCreateDTO = new RutinaCreateDTO();
        rutinaCreateDTO.setUsuarioId(1);
        rutinaCreateDTO.setNombre("Rutina Pecho");
        rutinaCreateDTO.setNivel("INTERMEDIO");
        rutinaCreateDTO.setEsPredefinida(false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("findAll devuelve lista de rutinas")
    void findAll_devuelve_lista() {
        when(rutinaRepository.findAll()).thenReturn(List.of(rutina));
        when(rutinaMapper.toDTOList(any())).thenReturn(List.of(rutinaDTO));

        List<RutinaDTO> result = rutinaService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(rutinaRepository).findAll();
    }

    @Test
    @DisplayName("findById con id existente devuelve RutinaDTO")
    void findById_existente_devuelve_dto() {
        when(rutinaRepository.findById(1)).thenReturn(Optional.of(rutina));
        when(rutinaMapper.toDTO(rutina)).thenReturn(rutinaDTO);

        RutinaDTO result = rutinaService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Rutina Pecho", result.getNombre());
    }

    @Test
    @DisplayName("findById con id inexistente lanza NotFoundEntityException")
    void findByID_inexistente_lanza_excepcion() {
        when(rutinaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> rutinaService.findById(99));
    }

    @Test
    @DisplayName("save correcto guarda la rutina con activa=true")
    void save_correcto_guarda_rutina() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rutinaMapper.toEntity(rutinaCreateDTO)).thenReturn(rutina);
        when(rutinaRepository.save(any())).thenReturn(rutina);
        when(rutinaMapper.toDTO(rutina)).thenReturn(rutinaDTO);

        RutinaDTO result = rutinaService.save(rutinaCreateDTO);

        assertNotNull(result);
        assertTrue(rutina.getActiva());
        verify(rutinaRepository).save(any());
    }

    @Test
    @DisplayName("save con usuario inexistente lanza NotFoundEntityException")
    void save_usuario_inexistente_lanza_excepcion() {
        when(rutinaMapper.toEntity(rutinaCreateDTO)).thenReturn(rutina);
        when(usuarioRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> rutinaService.save(rutinaCreateDTO));

        verify(rutinaRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteById desactiva la rutina correctamente")
    void deleteById_desactiva_rutina() {
        when(rutinaRepository.findById(1)).thenReturn(Optional.of(rutina));

        assertDoesNotThrow(() -> rutinaService.deleteById(1));
        assertFalse(rutina.getActiva());
        verify(rutinaRepository).save(rutina);
    }

    @Test
    @DisplayName("deleteById con id inexistente lanza NotFounEntityException")
    void deleteById_inexistente_lanza_excepcion() {
        when(rutinaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> rutinaService.deleteById(99));
    }

    @Test
    @DisplayName("activateById activa la rutina correctamente")
    void activateById_activa_rutina() {
        rutina.setActiva(false);
        when(rutinaRepository.findById(1)).thenReturn(Optional.of(rutina));

        assertDoesNotThrow(() -> rutinaService.activateById(1));

        assertTrue(rutina.getActiva());
        verify(rutinaRepository).save(rutina);
    }

    @Test
    @DisplayName("permanentDeleteById elimina la rutina permanentemente")
    void permanentDeleteById_elimina_rutina() {
        when(rutinaRepository.findById(1)).thenReturn(Optional.of(rutina));

        assertDoesNotThrow(() -> rutinaService.permanentDeleteById(1));

        verify(rutinaRepository).delete(rutina);
    }

    @Test
    @DisplayName("findByUsuarioId devuelve rutinas del usuario")
    void findByUsuarioId_devuelve_lista() {
        when(rutinaRepository.findByUsuarioId(1)).thenReturn(List.of(rutina));
        when(rutinaMapper.toDTOList(any())).thenReturn(List.of(rutinaDTO));

        List<RutinaDTO> result = rutinaService.findByUsuarioId(1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findByNivel devuelve rutinas del nivel indicado")
    void findByNivel_devuelve_lista() {
        when(rutinaRepository.findByNivel(Nivel.INTERMEDIO)).thenReturn(List.of(rutina));
        when(rutinaMapper.toDTOList(any())).thenReturn(List.of(rutinaDTO));

        List<RutinaDTO> result = rutinaService.findByNivel("INTERMEDIO");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findActivas devuelve solo rutinas activas")
    void findActivas_devuelve_activas() {
        when(rutinaRepository.findByActivaTrue()).thenReturn(List.of(rutina));
        when(rutinaMapper.toDTOList(any())).thenReturn(List.of(rutinaDTO));

        List<RutinaDTO> result = rutinaService.findActivas();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(rutinaRepository).findByActivaTrue();
    }

    @Test
    @DisplayName("findPredefinidas devuelve solo rutinas predefinidas")
    void findPredefinidas_devuelve_predefinidas() {
        when(rutinaRepository.findByEsPredefinidaTrue()).thenReturn(List.of(rutina));
        when(rutinaMapper.toDTOList(any())).thenReturn(List.of(rutinaDTO));

        List<RutinaDTO> result = rutinaService.findPredefinidas();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(rutinaRepository).findByEsPredefinidaTrue();
    }
}
