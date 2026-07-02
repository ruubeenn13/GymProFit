package com.gymprofit.api.service;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalCreateDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalDTO;
import com.gymprofit.api.entity.ObjetivoPersonal;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.TipoObjetivo;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.ObjetivoAlreadyCompletedException;
import com.gymprofit.api.mappers.ObjetivoPersonalMapper;
import com.gymprofit.api.repository.jpa.IObjetivoPersonalRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.logro.ILogroService;
import com.gymprofit.api.service.objetivopersonal.ObjetivoPersonalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// ============================================================
// ObjetivoPersonalServiceTest — pruebas unitarias del ObjetivoPersonalService
// Verifica CRUD y el flujo de completar objetivos (incluyendo el
// disparo de evaluación de logros al completar), con repositorios simulados.
// ============================================================
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del ObjetivoPersonalService")
class ObjetivoPersonalServiceTest {

    @Mock private IObjetivoPersonalRepository objetivoPersonalRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private ObjetivoPersonalMapper objetivoPersonalMapper;
    // Servicio de logros simulado, para verificar que se evalúan logros al completar objetivo
    @Mock private ILogroService logroService;
    // Mock de SecurityUtils: checkOwnership/requireAdmin quedan como no-op en las lecturas
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private ObjetivoPersonalService objetivoPersonalService;

    private ObjetivoPersonal objetivo;
    private ObjetivoPersonalDTO objetivoDTO;
    private ObjetivoPersonalCreateDTO createDTO;
    private Usuario usuario;

    // Prepara entidad, DTO y DTO de creación de prueba antes de cada test
    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1);
        usuario.setUsername("testuser");

        objetivo = new ObjetivoPersonal();
        objetivo.setId(1);
        objetivo.setUsuario(usuario);
        objetivo.setTipoObjetivo(TipoObjetivo.PERDER_PESO);
        objetivo.setCompletado(false);
        objetivo.setFechaInicio(LocalDate.now());

        objetivoDTO = new ObjetivoPersonalDTO();
        objetivoDTO.setId(1);
        objetivoDTO.setUsuarioId(1);
        objetivoDTO.setTipoObjetivo(TipoObjetivo.PERDER_PESO);
        objetivoDTO.setCompletado(false);

        createDTO = new ObjetivoPersonalCreateDTO();
        createDTO.setUsuarioId(1);
        createDTO.setTipoObjetivo(TipoObjetivo.PERDER_PESO);
    }

    // Comprueba que findAll devuelve todos los objetivos mapeados a DTO
    @Test
    @DisplayName("findAll devuelve lista de objetivos")
    void findAll_devuelve_lista() {
        when(objetivoPersonalRepository.findAll()).thenReturn(List.of(objetivo));
        when(objetivoPersonalMapper.toDTOList(any())).thenReturn(List.of(objetivoDTO));

        List<ObjetivoPersonalDTO> result = objetivoPersonalService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Comprueba que findById devuelve el DTO correcto cuando el objetivo existe
    @Test
    @DisplayName("findById existente devuelve DTO")
    void findById_existente_devuelve_dto() {
        when(objetivoPersonalRepository.findById(1)).thenReturn(Optional.of(objetivo));
        when(objetivoPersonalMapper.toDTO(objetivo)).thenReturn(objetivoDTO);

        ObjetivoPersonalDTO result = objetivoPersonalService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    // Comprueba que findById lanza excepción si el objetivo no existe
    @Test
    @DisplayName("findById inexistente lanza NotFoundEntityException")
    void findById_inexistente_lanza_excepcion() {
        when(objetivoPersonalRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> objetivoPersonalService.findById(99));
    }

    // Comprueba que al crear un objetivo se guarda con completado=false por defecto
    @Test
    @DisplayName("save correcto guarda el objetivo con completado=false")
    void save_correcto_guarda_objetivo() {
        // Como ADMIN, el service respeta el usuarioId del DTO (1) sin sobrescribirlo
        when(securityUtils.isAdmin()).thenReturn(true);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(objetivoPersonalMapper.toEntity(createDTO)).thenReturn(objetivo);
        when(objetivoPersonalRepository.save(any())).thenReturn(objetivo);
        when(objetivoPersonalRepository.findById(any())).thenReturn(Optional.of(objetivo));
        when(objetivoPersonalMapper.toDTO(objetivo)).thenReturn(objetivoDTO);

        ObjetivoPersonalDTO result = objetivoPersonalService.save(createDTO);

        assertNotNull(result);
        assertFalse(objetivo.getCompletado());
        verify(objetivoPersonalRepository).save(any());
    }

    // Comprueba que no se puede crear un objetivo para un usuario inexistente
    @Test
    @DisplayName("save con usuario inexistente lanza NotFoundEntityException")
    void save_usuario_inexistente_lanza_excepcion() {
        // Como ADMIN, el service busca el usuarioId del DTO (1) que no existe
        when(securityUtils.isAdmin()).thenReturn(true);
        when(usuarioRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> objetivoPersonalService.save(createDTO));
        verify(objetivoPersonalRepository, never()).save(any());
    }

    // Comprueba que deleteById elimina físicamente el objetivo existente
    @Test
    @DisplayName("deleteById elimina el objetivo correctamente")
    void deleteById_elimina_objetivo() {
        when(objetivoPersonalRepository.findById(1)).thenReturn(Optional.of(objetivo));

        assertDoesNotThrow(() -> objetivoPersonalService.deleteById(1));
        verify(objetivoPersonalRepository).delete(objetivo);
    }

    // Comprueba que deleteById lanza excepción si el objetivo no existe
    @Test
    @DisplayName("deleteById inexistente lanza NotFoundEntityException")
    void deleteById_inexistente_lanza_excepcion() {
        when(objetivoPersonalRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> objetivoPersonalService.deleteById(99));
    }

    // Comprueba que completar marca el objetivo, fija fecha de completado
    // y dispara la evaluación de logros del usuario
    @Test
    @DisplayName("completar marca el objetivo como completado")
    void completar_marca_completado() {
        when(objetivoPersonalRepository.findById(1)).thenReturn(Optional.of(objetivo));
        when(objetivoPersonalRepository.save(any())).thenReturn(objetivo);
        when(objetivoPersonalMapper.toDTO(objetivo)).thenReturn(objetivoDTO);
        when(logroService.evaluarLogros(any())).thenReturn(java.util.Collections.emptyList());

        objetivoPersonalService.completar(1);

        assertTrue(objetivo.getCompletado());
        assertNotNull(objetivo.getFechaCompletado());
        verify(logroService).evaluarLogros(1);
    }

    // Comprueba que no se puede completar dos veces el mismo objetivo
    @Test
    @DisplayName("completar objetivo ya completado lanza ObjetivoAlreadyCompletedException")
    void completar_ya_completado_lanza_excepcion() {
        objetivo.setCompletado(true);
        when(objetivoPersonalRepository.findById(1)).thenReturn(Optional.of(objetivo));

        assertThrows(ObjetivoAlreadyCompletedException.class, () -> objetivoPersonalService.completar(1));
    }

    // Comprueba que se listan los objetivos asociados a un usuario concreto
    @Test
    @DisplayName("findByUsuarioId devuelve objetivos del usuario")
    void findByUsuarioId_devuelve_lista() {
        when(objetivoPersonalRepository.findByUsuarioId(1)).thenReturn(List.of(objetivo));
        when(objetivoPersonalMapper.toDTOList(any())).thenReturn(List.of(objetivoDTO));

        List<ObjetivoPersonalDTO> result = objetivoPersonalService.findByUsuarioId(1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Comprueba que countByUsuarioId devuelve el número total de objetivos del usuario
    @Test
    @DisplayName("countByUsuarioId devuelve el total")
    void countByUsuarioId_devuelve_total() {
        when(objetivoPersonalRepository.countByUsuarioId(1)).thenReturn(3L);

        Long result = objetivoPersonalService.countByUsuarioId(1);

        assertEquals(3L, result);
    }
}
