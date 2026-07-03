package com.gymprofit.api.service;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalCreateDTO;
import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalDTO;
import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalPatchDTO;
import com.gymprofit.api.entity.MedicionCorporal;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.mappers.MedicionCorporalMapper;
import com.gymprofit.api.repository.jpa.IMedicionCorporalRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.medicioncorporal.MedicionCorporalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ============================================================
// MedicionCorporalServiceTest — tests unitarios del MedicionCorporalService
// Verifica el CRUD de mediciones corporales con control de propiedad
// (ownership), el cálculo automático del IMC en alta/modificación/patch y
// las distintas consultas por usuario (todas, ordenadas, rango de fechas, últimas).
// ============================================================
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del MedicionCorporalService")
class MedicionCorporalServiceTest {

    @Mock
    private IMedicionCorporalRepository medicionCorporalRepository;

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private MedicionCorporalMapper medicionCorporalMapper;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private MedicionCorporalService medicionCorporalService;

    private Usuario usuario;
    private MedicionCorporal medicion;
    private MedicionCorporalDTO medicionDTO;
    private MedicionCorporalCreateDTO medicionCreateDTO;

    // Inicializa usuario, medición y DTOs de prueba (con peso y altura para el cálculo de IMC)
    @BeforeEach
    void setup() {
        usuario = new Usuario();
        usuario.setId(1);

        medicion = new MedicionCorporal();
        medicion.setId(1);
        medicion.setUsuario(usuario);
        medicion.setPeso(new BigDecimal("70.00"));
        medicion.setAltura(new BigDecimal("1.75"));
        medicion.setFecha(LocalDateTime.now());

        medicionDTO = new MedicionCorporalDTO();
        medicionDTO.setId(1);
        medicionDTO.setUsuarioId(1);
        medicionDTO.setPeso(new BigDecimal("70.00"));
        medicionDTO.setAltura(new BigDecimal("1.75"));

        medicionCreateDTO = new MedicionCorporalCreateDTO();
        medicionCreateDTO.setUsuarioId(1);
        medicionCreateDTO.setPeso(new BigDecimal("70.00"));
        medicionCreateDTO.setAltura(new BigDecimal("1.75"));
    }

    // Comprueba que findAll (solo ADMIN) mapea y devuelve todas las mediciones
    @Test
    @DisplayName("findAll devuelve lista de mediciones corporales")
    void findAll_devuelve_lista() {
        when(medicionCorporalRepository.findAll()).thenReturn(List.of(medicion));
        when(medicionCorporalMapper.toDTOList(any())).thenReturn(List.of(medicionDTO));

        List<MedicionCorporalDTO> result = medicionCorporalService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(securityUtils).requireAdmin();
    }

    // Comprueba que findById devuelve el DTO cuando la medición existe y el usuario es propietario
    @Test
    @DisplayName("findById con id existente devuelve MedicionCorporalDTO")
    void findById_existente_devuelve_dto() {
        when(medicionCorporalRepository.findById(1)).thenReturn(Optional.of(medicion));
        when(medicionCorporalMapper.toDTO(medicion)).thenReturn(medicionDTO);

        MedicionCorporalDTO result = medicionCorporalService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(securityUtils).checkOwnership(usuario.getId());
    }

    // Comprueba que findById lanza NotFoundEntityException si la medición no existe
    @Test
    @DisplayName("findById con id inexistente lanza NotFoundEntityException")
    void findById_inexistente_lanza_excepcion() {
        when(medicionCorporalRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> medicionCorporalService.findById(99));
    }

    // Comprueba que save crea la medición, calcula el IMC y la asocia al usuario autenticado
    @Test
    @DisplayName("save crea la medición y calcula el IMC")
    void save_crea_medicion() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(medicionCorporalMapper.toEntity(medicionCreateDTO)).thenReturn(medicion);
        when(medicionCorporalRepository.findByUsuarioIdOrderByFechaDesc(1)).thenReturn(List.of(medicion));
        when(medicionCorporalMapper.toDTO(medicion)).thenReturn(medicionDTO);

        MedicionCorporalDTO result = medicionCorporalService.save(medicionCreateDTO);

        assertNotNull(result);
        assertNotNull(medicion.getImc());
        verify(medicionCorporalRepository).save(medicion);
    }

    // Comprueba que save lanza NotFoundEntityException si el usuario no existe
    @Test
    @DisplayName("save con usuario inexistente lanza NotFoundEntityException")
    void save_usuario_inexistente_lanza_excepcion() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(usuarioRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> medicionCorporalService.save(medicionCreateDTO));
    }

    // Comprueba que modify actualiza la medición, recalcula el IMC y devuelve el DTO
    @Test
    @DisplayName("modify actualiza la medición corporal")
    void modify_actualiza_medicion() {
        when(medicionCorporalRepository.findById(1)).thenReturn(Optional.of(medicion));
        when(medicionCorporalRepository.save(any())).thenReturn(medicion);
        when(medicionCorporalMapper.toDTO(medicion)).thenReturn(medicionDTO);

        MedicionCorporalDTO result = medicionCorporalService.modify(medicionDTO);

        assertNotNull(result);
        assertNotNull(medicion.getImc());
        verify(medicionCorporalRepository).save(medicion);
    }

    // Comprueba que deleteById elimina la medición tras verificar su propiedad
    @Test
    @DisplayName("deleteById elimina la medición corporal")
    void deleteById_elimina_medicion() {
        when(medicionCorporalRepository.findById(1)).thenReturn(Optional.of(medicion));

        assertDoesNotThrow(() -> medicionCorporalService.deleteById(1));

        verify(medicionCorporalRepository).delete(medicion);
    }

    // Comprueba que deleteById lanza NotFoundEntityException si la medición no existe
    @Test
    @DisplayName("deleteById con id inexistente lanza NotFoundEntityException")
    void deleteById_inexistente_lanza_excepcion() {
        when(medicionCorporalRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> medicionCorporalService.deleteById(99));
    }

    // Comprueba que findByUsuarioId verifica propiedad y devuelve las mediciones del usuario
    @Test
    @DisplayName("findByUsuarioId devuelve las mediciones del usuario")
    void findByUsuarioId_devuelve_lista() {
        when(medicionCorporalRepository.findByUsuarioId(1)).thenReturn(List.of(medicion));
        when(medicionCorporalMapper.toDTOList(any())).thenReturn(List.of(medicionDTO));

        List<MedicionCorporalDTO> result = medicionCorporalService.findByUsuarioId(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(securityUtils).checkOwnership(1);
    }

    // Comprueba que findByUsuarioIdOrdenadas devuelve las mediciones ordenadas por fecha descendente
    @Test
    @DisplayName("findByUsuarioIdOrdenadas devuelve las mediciones ordenadas por fecha")
    void findByUsuarioIdOrdenadas_devuelve_lista() {
        when(medicionCorporalRepository.findByUsuarioIdOrderByFechaDesc(1)).thenReturn(List.of(medicion));
        when(medicionCorporalMapper.toDTOList(any())).thenReturn(List.of(medicionDTO));

        List<MedicionCorporalDTO> result = medicionCorporalService.findByUsuarioIdOrdenadas(1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Comprueba que findByUsuarioIdAndFechaBetween devuelve las mediciones dentro del rango de fechas
    @Test
    @DisplayName("findByUsuarioIdAndFechaBetween devuelve las mediciones del rango")
    void findByUsuarioIdAndFechaBetween_devuelve_lista() {
        LocalDateTime inicio = LocalDateTime.now().minusDays(7);
        LocalDateTime fin = LocalDateTime.now();
        when(medicionCorporalRepository.findByUsuarioIdAndFechaBetween(1, inicio, fin)).thenReturn(List.of(medicion));
        when(medicionCorporalMapper.toDTOList(any())).thenReturn(List.of(medicionDTO));

        List<MedicionCorporalDTO> result = medicionCorporalService.findByUsuarioIdAndFechaBetween(1, inicio, fin);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Comprueba que getUltimasMediciones devuelve las mediciones más recientes del usuario
    @Test
    @DisplayName("getUltimasMediciones devuelve las mediciones más recientes")
    void getUltimasMediciones_devuelve_lista() {
        when(medicionCorporalRepository.getUltimasMediciones(1)).thenReturn(List.of(medicion));
        when(medicionCorporalMapper.toDTOList(any())).thenReturn(List.of(medicionDTO));

        List<MedicionCorporalDTO> result = medicionCorporalService.getUltimasMediciones(1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Comprueba que patch modifica solo los campos no nulos y recalcula el IMC al cambiar el peso
    @Test
    @DisplayName("patch aplica los campos no nulos y recalcula el IMC")
    void patch_aplica_campos_no_nulos() {
        MedicionCorporalPatchDTO patchDTO = new MedicionCorporalPatchDTO();
        patchDTO.setPeso(new BigDecimal("72.00"));

        when(medicionCorporalRepository.findById(1)).thenReturn(Optional.of(medicion));
        when(medicionCorporalRepository.save(any())).thenReturn(medicion);
        when(medicionCorporalMapper.toDTO(medicion)).thenReturn(medicionDTO);

        MedicionCorporalDTO result = medicionCorporalService.patch(1, patchDTO);

        assertNotNull(result);
        assertEquals(new BigDecimal("72.00"), medicion.getPeso());
        assertNotNull(medicion.getImc());
    }
}
