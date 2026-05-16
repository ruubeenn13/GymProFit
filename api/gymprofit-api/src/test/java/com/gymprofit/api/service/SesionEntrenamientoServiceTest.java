package com.gymprofit.api.service;

import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoDTO;
import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.SesionEntrenamiento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.mappers.SesionEntrenamientoMapper;
import com.gymprofit.api.repository.jpa.IRutinaRepository;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.logro.ILogroService;
import com.gymprofit.api.service.sesionentrenamiento.SesionEntrenamientoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del SesionEntrenamientoService")
class SesionEntrenamientoServiceTest {

    @Mock
    private ISesionEntrenamientoRepository sesionEntrenamientoRepository;

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private IRutinaRepository rutinaRepository;

    @Mock
    private SesionEntrenamientoMapper sesionEntrenamientoMapper;

    @Mock
    private ILogroService logroService;

    @InjectMocks
    private SesionEntrenamientoService sesionEntrenamientoService;

    private SesionEntrenamiento sesionEntrenamiento;
    private SesionEntrenamientoDTO sesionEntrenamientoDTO;
    private SesionEntrenamientoCreateDTO sesionEntrenamientoCreateDTO;
    private Usuario usuario;
    private Rutina rutina;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1);
        usuario.setUsername("testuser");

        rutina = new Rutina();
        rutina.setId(1);
        rutina.setNombre("Rutina Pecho");

        sesionEntrenamiento = new SesionEntrenamiento();
        sesionEntrenamiento.setId(1);
        sesionEntrenamiento.setUsuario(usuario);
        sesionEntrenamiento.setRutina(rutina);
        sesionEntrenamiento.setFechaInicio(LocalDateTime.now());
        sesionEntrenamiento.setCompletada(false);

        sesionEntrenamientoDTO = new SesionEntrenamientoDTO();
        sesionEntrenamientoDTO.setId(1);
        sesionEntrenamientoDTO.setUsuarioId(1);
        sesionEntrenamientoDTO.setRutinaId(1);
        sesionEntrenamientoDTO.setCompletada(false);

        sesionEntrenamientoCreateDTO = new SesionEntrenamientoCreateDTO();
        sesionEntrenamientoCreateDTO.setUsuarioId(1);
        sesionEntrenamientoCreateDTO.setRutinaId(1);
        sesionEntrenamientoCreateDTO.setFechaInicio(LocalDateTime.now());
    }

    @Test
    @DisplayName("findAll devuelve lista de sesiones")
    void findAll_devuelve_lista() {
        when(sesionEntrenamientoRepository.findAll()).thenReturn(List.of(sesionEntrenamiento));
        when(sesionEntrenamientoMapper.toDTOList(any())).thenReturn(List.of(sesionEntrenamientoDTO));

        List<SesionEntrenamientoDTO> result = sesionEntrenamientoService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(sesionEntrenamientoRepository).findAll();
    }

    @Test
    @DisplayName("findById con id existente devuelve SesionEntrenamientoDTO")
    void findById_existente_devuelve_dto() {
        when(sesionEntrenamientoRepository.findById(1)).thenReturn(Optional.of(sesionEntrenamiento));
        when(sesionEntrenamientoMapper.toDTO(sesionEntrenamiento)).thenReturn(sesionEntrenamientoDTO);

        SesionEntrenamientoDTO result = sesionEntrenamientoService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    @DisplayName("findById con id inexistente lanza NotFoundEntityException")
    void findById_inexistente_lanza_excepcion() {
        when(sesionEntrenamientoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> sesionEntrenamientoService.findById(99));
    }

    @Test
    @DisplayName("save correcto guarda la sesión con completada=false")
    void save_correcto_guarda_sesion() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rutinaRepository.findById(1)).thenReturn(Optional.of(rutina));
        when(sesionEntrenamientoMapper.toEntity(sesionEntrenamientoCreateDTO)).thenReturn(sesionEntrenamiento);
        when(sesionEntrenamientoRepository.save(any())).thenReturn(sesionEntrenamiento);
        when(sesionEntrenamientoMapper.toDTO(sesionEntrenamiento)).thenReturn(sesionEntrenamientoDTO);

        SesionEntrenamientoDTO result = sesionEntrenamientoService.save(sesionEntrenamientoCreateDTO);

        assertNotNull(result);
        assertFalse(sesionEntrenamiento.getCompletada());
        verify(sesionEntrenamientoRepository).save(any());
    }

    @Test
    @DisplayName("save con usuario inexistente lanza NotFoundEntityException")
    void save_usuario_inexistente_lanza_excepcion() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> sesionEntrenamientoService.save(sesionEntrenamientoCreateDTO));

        verify(sesionEntrenamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteById elimina la sesión correctamente")
    void deleteById_elimina_sesion() {
        when(sesionEntrenamientoRepository.findById(1)).thenReturn(Optional.of(sesionEntrenamiento));

        assertDoesNotThrow(() -> sesionEntrenamientoService.deleteById(1));

        verify(sesionEntrenamientoRepository).delete(sesionEntrenamiento);
    }

    @Test
    @DisplayName("deleteById con id inexistente lanza NotFoundEntityException")
    void deleteById_inexistente_lanza_excepcion() {
        when(sesionEntrenamientoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> sesionEntrenamientoService.deleteById(99));
    }

    @Test
    @DisplayName("completarSesion marca la sesión como completada")
    void completarSesion_marca_completada() {
        when(sesionEntrenamientoRepository.findById(1)).thenReturn(Optional.of(sesionEntrenamiento));
        when(sesionEntrenamientoRepository.save(any())).thenReturn(sesionEntrenamiento);
        when(sesionEntrenamientoMapper.toDTO(sesionEntrenamiento)).thenReturn(sesionEntrenamientoDTO);

        sesionEntrenamientoService.completarSesion(1, 400, "Sesión completada");

        assertTrue(sesionEntrenamiento.getCompletada());
        assertNotNull(sesionEntrenamiento.getFechaFin());
        assertEquals(400, sesionEntrenamiento.getCaloriasQuemadas());
        assertEquals("Sesión completada", sesionEntrenamiento.getNotas());
    }

    @Test
    @DisplayName("completarSesion con id inexistente lanza NotFoundEntityException")
    void completarSesion_inexistente_lanza_excepcion() {
        when(sesionEntrenamientoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> sesionEntrenamientoService.completarSesion(99, null, null));
    }

    @Test
    @DisplayName("findByUsuarioId devuelve sesiones del usuario")
    void findByUsuarioId_devuelve_lista() {
        when(sesionEntrenamientoRepository.findByUsuarioId(1)).thenReturn(List.of(sesionEntrenamiento));
        when(sesionEntrenamientoMapper.toDTOList(any())).thenReturn(List.of(sesionEntrenamientoDTO));

        List<SesionEntrenamientoDTO> result = sesionEntrenamientoService.findByUsuarioId(1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findCompletadas devuelve solo sesiones completadas")
    void findCompletadas_devuelve_completadas() {
        when(sesionEntrenamientoRepository.findByCompletadaTrue()).thenReturn(List.of(sesionEntrenamiento));
        when(sesionEntrenamientoMapper.toDTOList(any())).thenReturn(List.of(sesionEntrenamientoDTO));

        List<SesionEntrenamientoDTO> result = sesionEntrenamientoService.findCompletadas();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(sesionEntrenamientoRepository).findByCompletadaTrue();
    }

    @Test
    @DisplayName("findPendientes devuelve solo sesiones pendientes")
    void findPendientes_devuelve_pendientes() {
        when(sesionEntrenamientoRepository.findByCompletadaFalse()).thenReturn(List.of(sesionEntrenamiento));
        when(sesionEntrenamientoMapper.toDTOList(any())).thenReturn(List.of(sesionEntrenamientoDTO));

        List<SesionEntrenamientoDTO> result = sesionEntrenamientoService.findPendientes();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(sesionEntrenamientoRepository).findByCompletadaFalse();
    }

    @Test
    @DisplayName("countByUsuarioId devuelve el total de sesiones del usuario")
    void countByUsuarioId_devuelve_total() {
        when(sesionEntrenamientoRepository.countByUsuarioId(1)).thenReturn(5L);

        Long result = sesionEntrenamientoService.countByUsuarioId(1);

        assertEquals(5L, result);
        verify(sesionEntrenamientoRepository).countByUsuarioId(1);
    }
}
