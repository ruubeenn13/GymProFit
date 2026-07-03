package com.gymprofit.api.service;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaCreateDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaPatchDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoComida;
import com.gymprofit.api.entity.Comida;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.DuplicateEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.mappers.AlimentoComidaMapper;
import com.gymprofit.api.repository.jpa.IAlimentoComidaRepository;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IComidaRepository;
import com.gymprofit.api.service.alimentocomida.AlimentoComidaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ============================================================
// AlimentoComidaServiceTest — tests unitarios del AlimentoComidaService
// Verifica la gestión de las líneas alimento-comida: alta (con cálculo de
// calorías y control de duplicados), consulta con control de propiedad,
// modificación, patch, borrado y los conteos/búsquedas por comida/alimento.
// ============================================================
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del AlimentoComidaService")
class AlimentoComidaServiceTest {

    @Mock
    private IAlimentoComidaRepository alimentoComidaRepository;

    @Mock
    private IComidaRepository comidaRepository;

    @Mock
    private IAlimentoRepository alimentoRepository;

    @Mock
    private AlimentoComidaMapper alimentoComidaMapper;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AlimentoComidaService alimentoComidaService;

    private Usuario usuario;
    private Comida comida;
    private Alimento alimento;
    private AlimentoComida alimentoComida;
    private AlimentoComidaDTO alimentoComidaDTO;
    private AlimentoComidaCreateDTO alimentoComidaCreateDTO;

    // Inicializa usuario, comida, alimento, la línea alimento-comida y sus DTOs de prueba
    @BeforeEach
    void setup() {
        usuario = new Usuario();
        usuario.setId(1);

        comida = new Comida();
        comida.setId(10);
        comida.setUsuario(usuario);

        alimento = new Alimento();
        alimento.setId(20);
        alimento.setCalorias(200);

        alimentoComida = new AlimentoComida();
        alimentoComida.setId(1);
        alimentoComida.setCantidadGramos(new BigDecimal("100"));
        alimentoComida.setCaloriasTotales(200);
        alimentoComida.setComida(comida);
        alimentoComida.setAlimento(alimento);

        alimentoComidaDTO = new AlimentoComidaDTO();
        alimentoComidaDTO.setId(1);
        alimentoComidaDTO.setComidaId(10);
        alimentoComidaDTO.setAlimentoId(20);
        alimentoComidaDTO.setCantidadGramos(new BigDecimal("100"));

        alimentoComidaCreateDTO = new AlimentoComidaCreateDTO();
        alimentoComidaCreateDTO.setComidaId(10);
        alimentoComidaCreateDTO.setAlimentoId(20);
        alimentoComidaCreateDTO.setCantidadGramos(new BigDecimal("100"));
    }

    // Comprueba que findAll (solo ADMIN) mapea y devuelve todas las líneas alimento-comida
    @Test
    @DisplayName("findAll devuelve lista de alimentos-comida")
    void findAll_devuelve_lista() {
        when(alimentoComidaRepository.findAll()).thenReturn(List.of(alimentoComida));
        when(alimentoComidaMapper.toDTOList(any())).thenReturn(List.of(alimentoComidaDTO));

        List<AlimentoComidaDTO> result = alimentoComidaService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(securityUtils).requireAdmin();
    }

    // Comprueba que findById devuelve el DTO cuando la línea existe y el usuario es propietario
    @Test
    @DisplayName("findById con id existente devuelve AlimentoComidaDTO")
    void findById_existente_devuelve_dto() {
        when(alimentoComidaRepository.findById(1)).thenReturn(Optional.of(alimentoComida));
        when(alimentoComidaMapper.toDTO(alimentoComida)).thenReturn(alimentoComidaDTO);

        AlimentoComidaDTO result = alimentoComidaService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(securityUtils).checkOwnership(usuario.getId());
    }

    // Comprueba que findById lanza NotFoundEntityException si la línea no existe
    @Test
    @DisplayName("findById con id inexistente lanza NotFoundEntityException")
    void findById_inexistente_lanza_excepcion() {
        when(alimentoComidaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> alimentoComidaService.findById(99));
    }

    // Comprueba que save crea la línea, calcula calorías y recalcula los totales de la comida
    @Test
    @DisplayName("save crea la línea y calcula las calorías totales")
    void save_crea_linea() {
        when(comidaRepository.findById(10)).thenReturn(Optional.of(comida));
        when(alimentoRepository.findById(20)).thenReturn(Optional.of(alimento));
        when(alimentoComidaRepository.existsByComidaIdAndAlimentoId(10, 20)).thenReturn(false);
        when(alimentoComidaMapper.toEntity(alimentoComidaCreateDTO)).thenReturn(alimentoComida);
        when(alimentoComidaRepository.save(any())).thenReturn(alimentoComida);
        when(alimentoComidaRepository.findByComidaId(10)).thenReturn(List.of(alimentoComida));
        when(alimentoComidaMapper.toDTO(alimentoComida)).thenReturn(alimentoComidaDTO);

        AlimentoComidaDTO result = alimentoComidaService.save(alimentoComidaCreateDTO);

        assertNotNull(result);
        // 200 kcal/100g * 100g = 200 kcal
        assertEquals(200, alimentoComida.getCaloriasTotales());
        verify(alimentoComidaRepository).save(any());
    }

    // Comprueba que save lanza DuplicateEntityException si el alimento ya está en la comida
    @Test
    @DisplayName("save con relación duplicada lanza DuplicateEntityException")
    void save_duplicado_lanza_excepcion() {
        when(comidaRepository.findById(10)).thenReturn(Optional.of(comida));
        when(alimentoRepository.findById(20)).thenReturn(Optional.of(alimento));
        when(alimentoComidaRepository.existsByComidaIdAndAlimentoId(10, 20)).thenReturn(true);

        assertThrows(DuplicateEntityException.class, () -> alimentoComidaService.save(alimentoComidaCreateDTO));
    }

    // Comprueba que save lanza NotFoundEntityException si la comida indicada no existe
    @Test
    @DisplayName("save con comida inexistente lanza NotFoundEntityException")
    void save_comida_inexistente_lanza_excepcion() {
        when(comidaRepository.findById(10)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> alimentoComidaService.save(alimentoComidaCreateDTO));
    }

    // Comprueba que modify actualiza la línea, recalcula calorías y devuelve el DTO
    @Test
    @DisplayName("modify actualiza la línea alimento-comida")
    void modify_actualiza_linea() {
        when(alimentoComidaRepository.findById(1)).thenReturn(Optional.of(alimentoComida));
        when(comidaRepository.findById(10)).thenReturn(Optional.of(comida));
        when(alimentoRepository.findById(20)).thenReturn(Optional.of(alimento));
        when(alimentoComidaRepository.save(any())).thenReturn(alimentoComida);
        when(alimentoComidaRepository.findByComidaId(10)).thenReturn(List.of(alimentoComida));
        when(alimentoComidaMapper.toDTO(alimentoComida)).thenReturn(alimentoComidaDTO);

        AlimentoComidaDTO result = alimentoComidaService.modify(alimentoComidaDTO);

        assertNotNull(result);
        verify(alimentoComidaRepository).save(any());
    }

    // Comprueba que deleteById elimina la línea y recalcula los totales de la comida
    @Test
    @DisplayName("deleteById elimina la línea alimento-comida")
    void deleteById_elimina_linea() {
        when(alimentoComidaRepository.findById(1)).thenReturn(Optional.of(alimentoComida));
        when(alimentoComidaRepository.findByComidaId(10)).thenReturn(List.of());

        assertDoesNotThrow(() -> alimentoComidaService.deleteById(1));

        verify(alimentoComidaRepository).delete(alimentoComida);
    }

    // Comprueba que deleteById lanza NotFoundEntityException si la línea no existe
    @Test
    @DisplayName("deleteById con id inexistente lanza NotFoundEntityException")
    void deleteById_inexistente_lanza_excepcion() {
        when(alimentoComidaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> alimentoComidaService.deleteById(99));
    }

    // Comprueba que findByComidaId verifica propiedad y devuelve los alimentos de la comida
    @Test
    @DisplayName("findByComidaId devuelve los alimentos de la comida")
    void findByComidaId_devuelve_lista() {
        when(comidaRepository.findById(10)).thenReturn(Optional.of(comida));
        when(alimentoComidaRepository.findByComidaId(10)).thenReturn(List.of(alimentoComida));
        when(alimentoComidaMapper.toDTOList(any())).thenReturn(List.of(alimentoComidaDTO));

        List<AlimentoComidaDTO> result = alimentoComidaService.findByComidaId(10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Comprueba que findByAlimentoId (solo ADMIN) devuelve las comidas que contienen el alimento
    @Test
    @DisplayName("findByAlimentoId devuelve las comidas que contienen el alimento")
    void findByAlimentoId_devuelve_lista() {
        when(alimentoComidaRepository.findByAlimentoId(20)).thenReturn(List.of(alimentoComida));
        when(alimentoComidaMapper.toDTOList(any())).thenReturn(List.of(alimentoComidaDTO));

        List<AlimentoComidaDTO> result = alimentoComidaService.findByAlimentoId(20);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(securityUtils).requireAdmin();
    }

    // Comprueba que existsByComidaIdAndAlimentoId verifica propiedad y delega en el repositorio
    @Test
    @DisplayName("existsByComidaIdAndAlimentoId devuelve si existe la relación")
    void existsByComidaIdAndAlimentoId_devuelve_boolean() {
        when(comidaRepository.findById(10)).thenReturn(Optional.of(comida));
        when(alimentoComidaRepository.existsByComidaIdAndAlimentoId(10, 20)).thenReturn(true);

        boolean result = alimentoComidaService.existsByComidaIdAndAlimentoId(10, 20);

        assertTrue(result);
    }

    // Comprueba que countByComidaId verifica propiedad y devuelve el número de alimentos de la comida
    @Test
    @DisplayName("countByComidaId devuelve el número de alimentos de la comida")
    void countByComidaId_devuelve_total() {
        when(comidaRepository.findById(10)).thenReturn(Optional.of(comida));
        when(alimentoComidaRepository.countByComidaId(10)).thenReturn(3L);

        Long result = alimentoComidaService.countByComidaId(10);

        assertEquals(3L, result);
    }

    // Comprueba que patch recalcula las calorías al cambiar la cantidad en gramos
    @Test
    @DisplayName("patch con nueva cantidad recalcula las calorías")
    void patch_recalcula_calorias() {
        AlimentoComidaPatchDTO patchDTO = new AlimentoComidaPatchDTO();
        patchDTO.setCantidadGramos(new BigDecimal("50"));

        when(alimentoComidaRepository.findById(1)).thenReturn(Optional.of(alimentoComida));
        when(alimentoComidaRepository.save(any())).thenReturn(alimentoComida);
        when(alimentoComidaRepository.findByComidaId(10)).thenReturn(List.of(alimentoComida));
        when(alimentoComidaMapper.toDTO(alimentoComida)).thenReturn(alimentoComidaDTO);

        AlimentoComidaDTO result = alimentoComidaService.patch(1, patchDTO);

        assertNotNull(result);
        // 200 kcal/100g * 50g = 100 kcal
        assertEquals(100, alimentoComida.getCaloriasTotales());
    }
}
