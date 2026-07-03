package com.gymprofit.api.service;

import com.gymprofit.api.dto.entity.alimento.AlimentoCreateDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoPatchDTO;
import com.gymprofit.api.dto.jooq.AlimentoJooqDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.mappers.AlimentoMapper;
import com.gymprofit.api.repository.jooq.alimento.IAlimentoJooqRepository;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.alimento.AlimentoService;
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

// ============================================================
// AlimentoServiceTest — tests unitarios del AlimentoService
// Verifica el CRUD de alimentos, la baja lógica/activación, el borrado
// permanente, la modificación completa y el patch parcial, así como las
// búsquedas por nombre/categoría/calorías, los conteos y la búsqueda admin (jOOQ).
// ============================================================
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del AlimentoService")
class AlimentoServiceTest {

    @Mock
    private IAlimentoRepository alimentoRepository;

    @Mock
    private IAlimentoJooqRepository alimentoJooqRepository;

    @Mock
    private AlimentoMapper alimentoMapper;

    @Mock
    private IUsuarioRepository usuarioRepository;

    @InjectMocks
    private AlimentoService alimentoService;

    private Alimento alimento;
    private AlimentoDTO alimentoDTO;
    private AlimentoCreateDTO alimentoCreateDTO;

    // Inicializa entidad y DTOs de alimento de prueba
    @BeforeEach
    void setup() {
        alimento = new Alimento();
        alimento.setId(1);
        alimento.setNombre("Pechuga de pollo");
        alimento.setCategoria("Carnes");
        alimento.setCalorias(120);
        alimento.setActivo(true);

        alimentoDTO = new AlimentoDTO();
        alimentoDTO.setId(1);
        alimentoDTO.setNombre("Pechuga de pollo");
        alimentoDTO.setCategoria("Carnes");
        alimentoDTO.setCalorias(120);
        alimentoDTO.setActivo(true);

        alimentoCreateDTO = new AlimentoCreateDTO();
        alimentoCreateDTO.setNombre("Pechuga de pollo");
        alimentoCreateDTO.setCategoria("Carnes");
        alimentoCreateDTO.setCalorias(120);
    }

    // Comprueba que findAll mapea y devuelve todos los alimentos del repositorio
    @Test
    @DisplayName("findAll devuelve lista de alimentos")
    void findAll_devuelve_lista() {
        when(alimentoRepository.findAll()).thenReturn(List.of(alimento));
        when(alimentoMapper.toDTOList(any())).thenReturn(List.of(alimentoDTO));

        List<AlimentoDTO> result = alimentoService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(alimentoRepository).findAll();
    }

    // Comprueba que findById devuelve el DTO correspondiente cuando el alimento existe
    @Test
    @DisplayName("findById con id existente devuelve AlimentoDTO")
    void findById_existente_devuelve_dto() {
        when(alimentoRepository.findById(1)).thenReturn(Optional.of(alimento));
        when(alimentoMapper.toDTO(alimento)).thenReturn(alimentoDTO);

        AlimentoDTO result = alimentoService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Pechuga de pollo", result.getNombre());
    }

    // Comprueba que findById lanza NotFoundEntityException si el alimento no existe
    @Test
    @DisplayName("findById con id inexistente lanza NotFoundEntityException")
    void findById_inexistente_lanza_excepcion() {
        when(alimentoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> alimentoService.findById(99));
    }

    // Comprueba que save persiste el alimento marcándolo como activo (sin usuario asociado)
    @Test
    @DisplayName("save correcto guarda el alimento con activo=true")
    void save_correcto_guarda_alimento() {
        when(alimentoMapper.toEntity(alimentoCreateDTO)).thenReturn(alimento);
        when(alimentoRepository.save(any())).thenReturn(alimento);
        when(alimentoMapper.toDTO(alimento)).thenReturn(alimentoDTO);

        AlimentoDTO result = alimentoService.save(alimentoCreateDTO);

        assertNotNull(result);
        assertTrue(alimento.getActivo());
        verify(alimentoRepository).save(any());
    }

    // Comprueba que save asocia el usuario propietario cuando el DTO trae usuarioId
    @Test
    @DisplayName("save con usuarioId asocia el alimento al usuario creador")
    void save_con_usuario_asocia_usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(7);
        alimentoCreateDTO.setUsuarioId(7);

        when(alimentoMapper.toEntity(alimentoCreateDTO)).thenReturn(alimento);
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));
        when(alimentoRepository.save(any())).thenReturn(alimento);
        when(alimentoMapper.toDTO(alimento)).thenReturn(alimentoDTO);

        AlimentoDTO result = alimentoService.save(alimentoCreateDTO);

        assertNotNull(result);
        assertEquals(usuario, alimento.getUsuario());
    }

    // Comprueba que deleteById realiza un borrado lógico (desactiva el alimento)
    @Test
    @DisplayName("deleteById desactiva el alimento correctamente")
    void deleteById_desactiva_alimento() {
        when(alimentoRepository.findById(1)).thenReturn(Optional.of(alimento));

        assertDoesNotThrow(() -> alimentoService.deleteById(1));

        assertFalse(alimento.getActivo());
        verify(alimentoRepository).save(alimento);
    }

    // Comprueba que deleteById lanza NotFoundEntityException si el alimento no existe
    @Test
    @DisplayName("deleteById con id inexistente lanza NotFoundEntityException")
    void deleteById_inexistente_lanza_excepcion() {
        when(alimentoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> alimentoService.deleteById(99));
    }

    // Comprueba que activateById reactiva un alimento previamente desactivado
    @Test
    @DisplayName("activateById activa el alimento correctamente")
    void activateById_activa_alimento() {
        alimento.setActivo(false);
        when(alimentoRepository.findById(1)).thenReturn(Optional.of(alimento));

        assertDoesNotThrow(() -> alimentoService.activateById(1));

        assertTrue(alimento.getActivo());
        verify(alimentoRepository).save(alimento);
    }

    // Comprueba que permanentDeleteById borra el alimento de forma física (no lógica)
    @Test
    @DisplayName("permanentDeleteById elimina el alimento permanentemente")
    void permanentDeleteById_elimina_alimento() {
        when(alimentoRepository.findById(1)).thenReturn(Optional.of(alimento));

        assertDoesNotThrow(() -> alimentoService.permanentDeleteById(1));

        verify(alimentoRepository).delete(alimento);
    }

    // Comprueba que modify actualiza el alimento existente y devuelve el DTO actualizado
    @Test
    @DisplayName("modify actualiza el alimento existente")
    void modify_actualiza_alimento() {
        when(alimentoRepository.findById(1)).thenReturn(Optional.of(alimento));
        when(alimentoRepository.save(any())).thenReturn(alimento);
        when(alimentoMapper.toDTO(alimento)).thenReturn(alimentoDTO);

        AlimentoDTO result = alimentoService.modify(alimentoDTO);

        assertNotNull(result);
        verify(alimentoRepository).save(alimento);
    }

    // Comprueba que patch modifica solo los campos no nulos del alimento
    @Test
    @DisplayName("patch aplica solo los campos no nulos")
    void patch_aplica_campos_no_nulos() {
        AlimentoPatchDTO patchDTO = new AlimentoPatchDTO();
        patchDTO.setNombre("Pavo");

        when(alimentoRepository.findById(1)).thenReturn(Optional.of(alimento));
        when(alimentoRepository.save(any())).thenReturn(alimento);
        when(alimentoMapper.toDTO(alimento)).thenReturn(alimentoDTO);

        AlimentoDTO result = alimentoService.patch(1, patchDTO);

        assertNotNull(result);
        assertEquals("Pavo", alimento.getNombre());
    }

    // Comprueba que findByNombre busca alimentos por coincidencia parcial en el nombre
    @Test
    @DisplayName("findByNombre devuelve alimentos que contienen el nombre")
    void findByNombre_devuelve_lista() {
        when(alimentoRepository.findByNombreContainingIgnoreCase("pollo")).thenReturn(List.of(alimento));
        when(alimentoMapper.toDTOList(any())).thenReturn(List.of(alimentoDTO));

        List<AlimentoDTO> result = alimentoService.findByNombre("pollo");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Comprueba que findByCategoria filtra alimentos por categoría exacta
    @Test
    @DisplayName("findByCategoria devuelve alimentos de la categoría indicada")
    void findByCategoria_devuelve_lista() {
        when(alimentoRepository.findByCategoria("Carnes")).thenReturn(List.of(alimento));
        when(alimentoMapper.toDTOList(any())).thenReturn(List.of(alimentoDTO));

        List<AlimentoDTO> result = alimentoService.findByCategoria("Carnes");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // Comprueba que findActivos devuelve únicamente los alimentos con activo=true
    @Test
    @DisplayName("findActivos devuelve solo alimentos activos")
    void findActivos_devuelve_activos() {
        when(alimentoRepository.findByActivoTrue()).thenReturn(List.of(alimento));
        when(alimentoMapper.toDTOList(any())).thenReturn(List.of(alimentoDTO));

        List<AlimentoDTO> result = alimentoService.findActivos();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(alimentoRepository).findByActivoTrue();
    }

    // Comprueba que countActivos delega en el repositorio y devuelve el número de alimentos activos
    @Test
    @DisplayName("countActivos devuelve el número de alimentos activos")
    void countActivos_devuelve_total() {
        when(alimentoRepository.countByActivoTrue()).thenReturn(5L);

        Long result = alimentoService.countActivos();

        assertEquals(5L, result);
    }

    // Comprueba que busquedaAdmin delega en el repositorio jOOQ y devuelve los resultados
    @Test
    @DisplayName("busquedaAdmin delega en el repositorio jOOQ")
    void busquedaAdmin_delega_jooq() {
        when(alimentoJooqRepository.busquedaAdmin("pollo", "Carnes", true))
                .thenReturn(List.of(new AlimentoJooqDTO()));

        List<AlimentoJooqDTO> result = alimentoService.busquedaAdmin("pollo", "Carnes", true);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(alimentoJooqRepository).busquedaAdmin("pollo", "Carnes", true);
    }
}
