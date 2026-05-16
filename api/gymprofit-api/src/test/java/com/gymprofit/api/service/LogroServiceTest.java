package com.gymprofit.api.service;

import com.gymprofit.api.dto.entity.logro.LogroCreateDTO;
import com.gymprofit.api.dto.entity.logro.LogroDTO;
import com.gymprofit.api.dto.entity.logro.UsuarioLogroDTO;
import com.gymprofit.api.entity.Logro;
import com.gymprofit.api.enums.TipoLogro;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.mappers.LogroMapper;
import com.gymprofit.api.repository.jpa.IEjercicioRealizadoRepository;
import com.gymprofit.api.repository.jpa.ILogroRepository;
import com.gymprofit.api.repository.jpa.IObjetivoPersonalRepository;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioLogroRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.logro.LogroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del LogroService")
class LogroServiceTest {

    @Mock private ILogroRepository logroRepository;
    @Mock private IUsuarioLogroRepository usuarioLogroRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private ISesionEntrenamientoRepository sesionRepository;
    @Mock private IEjercicioRealizadoRepository ejercicioRealizadoRepository;
    @Mock private IObjetivoPersonalRepository objetivoPersonalRepository;
    @Mock private LogroMapper logroMapper;

    @InjectMocks
    private LogroService logroService;

    private Logro logro;
    private LogroDTO logroDTO;
    private LogroCreateDTO logroCreateDTO;

    @BeforeEach
    void setUp() {
        logro = new Logro();
        logro.setId(1);
        logro.setNombre("Primera Sesión");
        logro.setDescripcion("Completa tu primera sesión");
        logro.setTipo(TipoLogro.PRIMERA_SESION);

        logroDTO = new LogroDTO();
        logroDTO.setId(1);
        logroDTO.setNombre("Primera Sesión");
        logroDTO.setTipo(TipoLogro.PRIMERA_SESION);

        logroCreateDTO = new LogroCreateDTO();
        logroCreateDTO.setNombre("Primera Sesión");
        logroCreateDTO.setDescripcion("Completa tu primera sesión");
        logroCreateDTO.setTipo("PRIMERA_SESION");
    }

    @Test
    @DisplayName("findAll devuelve lista de logros")
    void findAll_devuelve_lista() {
        when(logroRepository.findAll()).thenReturn(List.of(logro));
        when(logroMapper.toDTOList(any())).thenReturn(List.of(logroDTO));

        List<LogroDTO> result = logroService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(logroRepository).findAll();
    }

    @Test
    @DisplayName("findByUsuarioId devuelve logros del usuario")
    void findByUsuarioId_devuelve_lista() {
        when(usuarioRepository.existsById(1)).thenReturn(true);
        when(usuarioLogroRepository.findByUsuarioId(1)).thenReturn(List.of());
        when(logroMapper.toUsuarioLogroDTOList(any())).thenReturn(List.of());

        List<UsuarioLogroDTO> result = logroService.findByUsuarioId(1);

        assertNotNull(result);
        verify(usuarioLogroRepository).findByUsuarioId(1);
    }

    @Test
    @DisplayName("findByUsuarioId con usuario inexistente lanza NotFoundEntityException")
    void findByUsuarioId_inexistente_lanza_excepcion() {
        when(usuarioRepository.existsById(99)).thenReturn(false);

        assertThrows(NotFoundEntityException.class, () -> logroService.findByUsuarioId(99));
    }

    @Test
    @DisplayName("save crea logro correctamente")
    void save_crea_logro() {
        when(logroRepository.save(any())).thenReturn(logro);
        when(logroMapper.toDTO(logro)).thenReturn(logroDTO);

        LogroDTO result = logroService.save(logroCreateDTO);

        assertNotNull(result);
        assertEquals("Primera Sesión", result.getNombre());
        verify(logroRepository).save(any());
    }

    @Test
    @DisplayName("save con tipo inválido lanza InvalidDataException")
    void save_tipo_invalido_lanza_excepcion() {
        logroCreateDTO.setTipo("INVALIDO");

        assertThrows(InvalidDataException.class, () -> logroService.save(logroCreateDTO));
        verify(logroRepository, never()).save(any());
    }

    @Test
    @DisplayName("update logro existente actualiza correctamente")
    void update_existente_actualiza() {
        when(logroRepository.findById(1)).thenReturn(Optional.of(logro));
        when(logroRepository.save(any())).thenReturn(logro);
        when(logroMapper.toDTO(logro)).thenReturn(logroDTO);

        LogroDTO result = logroService.update(1, logroCreateDTO);

        assertNotNull(result);
        verify(logroRepository).save(any());
    }

    @Test
    @DisplayName("update logro inexistente lanza NotFoundEntityException")
    void update_inexistente_lanza_excepcion() {
        when(logroRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> logroService.update(99, logroCreateDTO));
        verify(logroRepository, never()).save(any());
    }
}
