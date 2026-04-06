package com.gymprofit.api.service;

import com.gymprofit.api.dto.entity.ejercicio.EjercicioCreateDTO;
import com.gymprofit.api.dto.entity.ejercicio.EjercicioDTO;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.enums.Dificultad;
import com.gymprofit.api.enums.GrupoMuscular;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.mappers.EjercicioMapper;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.service.ejercicio.EjercicioService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del EjercicioService")
class EjercicioServiceTest {

    @Mock
    private IEjercicioRepository ejercicioRepository;

    @Mock
    private EjercicioMapper ejercicioMapper;

    @InjectMocks
    private EjercicioService ejercicioService;

    private Ejercicio ejercicio;
    private EjercicioDTO ejercicioDTO;
    private EjercicioCreateDTO ejercicioCreateDTO;

    @BeforeEach
    void setup() {
        ejercicio = new Ejercicio();
        ejercicio.setId(1);
        ejercicio.setNombre("Press de Banca");
        ejercicio.setGrupoMuscular(GrupoMuscular.PECHO);
        ejercicio.setDificultad(Dificultad.INTERMEDIO);
        ejercicio.setActivo(true);

        ejercicioDTO = new EjercicioDTO();
        ejercicioDTO.setId(1);
        ejercicioDTO.setNombre("Press de Banca");
        ejercicioDTO.setGrupoMuscular("PECHO");
        ejercicioDTO.setActivo(true);

        ejercicioCreateDTO = new EjercicioCreateDTO();
        ejercicioCreateDTO.setNombre("Press de Banca");
        ejercicioCreateDTO.setGrupoMuscular("PECHO");
        ejercicioCreateDTO.setDescripcion("INTERMEDIO");
    }

    @Test
    @DisplayName("findAll devuelve lista de ejercicios")
    void findAll_devuelve_lista() {
        when(ejercicioRepository.findAll()).thenReturn(List.of(ejercicio));
        when(ejercicioMapper.toDTOList(any())).thenReturn(List.of(ejercicioDTO));

        List<EjercicioDTO> result = ejercicioService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(ejercicioRepository).findAll();
    }

    @Test
    @DisplayName("findById con id existente devuelve EjercicioDTO")
    void findById_existente_devuelve_dto() {
        when(ejercicioRepository.findById(1)).thenReturn(Optional.of(ejercicio));
        when(ejercicioMapper.toDTO(ejercicio)).thenReturn(ejercicioDTO);

        EjercicioDTO result = ejercicioService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Press de Banca", result.getNombre());
    }

    @Test
    @DisplayName("findById don id inexistente lanza NotFoundEntityException")
    void findById_inexistente_lanza_excepcion() {
        when(ejercicioRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> ejercicioService.findById(99));
    }

    @Test
    @DisplayName("save correcto guarda el ejercicio con activo=true")
    void save_correcto_guarda_ejercicio() {
        when(ejercicioMapper.toEntity(ejercicioCreateDTO)).thenReturn(ejercicio);
        when(ejercicioRepository.save(any())).thenReturn(ejercicio);
        when(ejercicioMapper.toDTO(ejercicio)).thenReturn(ejercicioDTO);

        EjercicioDTO result = ejercicioService.save(ejercicioCreateDTO);

        assertNotNull(result);
        assertTrue(ejercicio.getActivo());
        verify(ejercicioRepository).save(any());
    }

    @Test
    @DisplayName("deleteById desactiva el ejercicio correctamente")
    void deleteById_desactiva_ejercicio() {
        when(ejercicioRepository.findById(1)).thenReturn(Optional.of(ejercicio));

        assertDoesNotThrow(() -> ejercicioService.deleteById(1));

        assertFalse(ejercicio.getActivo());
        verify(ejercicioRepository).save(ejercicio);
    }

    @Test
    @DisplayName("deleteById con id inexistente lanza NotFoundEntityException")
    void deleteById_inexistente_lanza_excepcion() {
        when(ejercicioRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> ejercicioService.deleteById(99));
    }

    @Test
    @DisplayName("activateById activa el ejercicio correctamente")
    void activateById_activa_ejercicio() {
        ejercicio.setActivo(false);
        when(ejercicioRepository.findById(1)).thenReturn(Optional.of(ejercicio));

        assertDoesNotThrow(() -> ejercicioService.activateById(1));

        assertTrue(ejercicio.getActivo());
        verify(ejercicioRepository).save(ejercicio);
    }

    @Test
    @DisplayName("permanentDeleteById elimina el ejercicio permanentemente")
    void permanentDeleteById_elimina_ejercicio() {
        when(ejercicioRepository.findById(1)).thenReturn(Optional.of(ejercicio));

        assertDoesNotThrow(() -> ejercicioService.permanentDeleteById(1));

        verify(ejercicioRepository).delete(ejercicio);
    }

    @Test
    @DisplayName("findByGrupoMuscular devuelve ejercicios del grupo indicado")
    void findByGrupoMuscular_devuelve_lista() {
        when(ejercicioRepository.findByGrupoMuscular(any())).thenReturn(List.of(ejercicio));
        when(ejercicioMapper.toDTOList(any())).thenReturn(List.of(ejercicioDTO));

        List<EjercicioDTO> result = ejercicioService.findByGrupoMuscular("PECHO");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findByDificultad devuelve ejercicios de la dificultad indicada")
    void findByDificultad_devuelve_lista() {
        when(ejercicioRepository.findByDificultad(any())).thenReturn(List.of(ejercicio));
        when(ejercicioMapper.toDTOList(any())).thenReturn(List.of(ejercicioDTO));

        List<EjercicioDTO> result = ejercicioService.findByDificultad("INTERMEDIO");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findByNombre devuelve ejercicios que contienen el nombre")
    void findByNombre_devuelve_lista() {
        when(ejercicioRepository.findByNombreContainingIgnoreCase("press")).thenReturn(List.of(ejercicio));
        when(ejercicioMapper.toDTOList(any())).thenReturn(List.of(ejercicioDTO));

        List<EjercicioDTO> result = ejercicioService.findByNombre("press");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findActivos devuelve solo ejercicios activos")
    void findActivos_devuelve_activos() {
        when(ejercicioRepository.findByActivoTrue()).thenReturn(List.of(ejercicio));
        when(ejercicioMapper.toDTOList(any())).thenReturn(List.of(ejercicioDTO));

        List<EjercicioDTO> result = ejercicioService.findActivos();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(ejercicioRepository).findByActivoTrue();
    }
}
