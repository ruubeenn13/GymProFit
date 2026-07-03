package com.gymprofit.api.service;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.dto.entity.comida.ComidaDTO;
import com.gymprofit.api.dto.entity.comida.ComidaPatchDTO;
import com.gymprofit.api.entity.Comida;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.TipoComida;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.mappers.ComidaMapper;
import com.gymprofit.api.repository.jpa.IComidaRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.comida.ComidaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ============================================================
// ComidaServiceTest — tests unitarios del ComidaService
// Verifica el CRUD de comidas del diario nutricional con control de
// propiedad (ownership), la conversión del tipo de comida (enum), y las
// consultas/conteos por usuario, tipo y fecha (algunas restringidas a ADMIN).
// ============================================================
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del ComidaService")
class ComidaServiceTest {

    @Mock
    private IComidaRepository comidaRepository;

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private ComidaMapper comidaMapper;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ComidaService comidaService;

    private Usuario usuario;
    private Comida comida;
    private ComidaDTO comidaDTO;
    private ComidaCreateDTO comidaCreateDTO;

    // Inicializa usuario, comida y DTOs de prueba
    @BeforeEach
    void setup() {
        usuario = new Usuario();
        usuario.setId(1);

        comida = new Comida();
        comida.setId(1);
        comida.setUsuario(usuario);
        comida.setTipoComida(TipoComida.DESAYUNO);
        comida.setFecha(LocalDateTime.now());

        comidaDTO = new ComidaDTO();
        comidaDTO.setId(1);
        comidaDTO.setUsuarioId(1);
        comidaDTO.setTipoComida("DESAYUNO");
        comidaDTO.setFecha(LocalDateTime.now());

        comidaCreateDTO = new ComidaCreateDTO();
        comidaCreateDTO.setUsuarioId(1);
        comidaCreateDTO.setTipoComida("DESAYUNO");
    }

    // Comprueba que findAll (solo ADMIN) mapea y devuelve todas las comidas
    @Test
    @DisplayName("findAll devuelve lista de comidas")
    void findAll_devuelve_lista() {
        when(comidaRepository.findAll()).thenReturn(List.of(comida));
        when(comidaMapper.toDTOList(any())).thenReturn(List.of(comidaDTO));

        List<ComidaDTO> result = comidaService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(securityUtils).requireAdmin();
    }

    // Comprueba que findById devuelve el DTO cuando la comida existe y el usuario es propietario
    @Test
    @DisplayName("findById con id existente devuelve ComidaDTO")
    void findById_existente_devuelve_dto() {
        when(comidaRepository.findById(1)).thenReturn(Optional.of(comida));
        when(comidaMapper.toDTO(comida)).thenReturn(comidaDTO);

        ComidaDTO result = comidaService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(securityUtils).checkOwnership(usuario.getId());
    }

    // Comprueba que findById lanza NotFoundEntityException si la comida no existe
    @Test
    @DisplayName("findById con id inexistente lanza NotFoundEntityException")
    void findById_inexistente_lanza_excepcion() {
        when(comidaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> comidaService.findById(99));
    }

    // Comprueba que save crea la comida forzando el usuarioId al usuario autenticado (no ADMIN)
    @Test
    @DisplayName("save crea la comida para el usuario autenticado")
    void save_crea_comida() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(comidaMapper.toEntity(comidaCreateDTO)).thenReturn(comida);
        when(comidaRepository.save(any())).thenReturn(comida);
        when(comidaMapper.toDTO(comida)).thenReturn(comidaDTO);

        ComidaDTO result = comidaService.save(comidaCreateDTO);

        assertNotNull(result);
        assertEquals(usuario, comida.getUsuario());
        verify(comidaRepository).save(any());
    }

    // Comprueba que save lanza NotFoundEntityException si el usuario no existe
    @Test
    @DisplayName("save con usuario inexistente lanza NotFoundEntityException")
    void save_usuario_inexistente_lanza_excepcion() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(usuarioRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> comidaService.save(comidaCreateDTO));
    }

    // Comprueba que modify actualiza la comida existente tras verificar su propiedad
    @Test
    @DisplayName("modify actualiza la comida existente")
    void modify_actualiza_comida() {
        when(comidaRepository.findById(1)).thenReturn(Optional.of(comida));
        when(comidaRepository.save(any())).thenReturn(comida);
        when(comidaMapper.toDTO(comida)).thenReturn(comidaDTO);

        ComidaDTO result = comidaService.modify(comidaDTO);

        assertNotNull(result);
        verify(comidaRepository).save(comida);
    }

    // Comprueba que deleteById elimina la comida tras verificar su propiedad
    @Test
    @DisplayName("deleteById elimina la comida correctamente")
    void deleteById_elimina_comida() {
        when(comidaRepository.findById(1)).thenReturn(Optional.of(comida));

        assertDoesNotThrow(() -> comidaService.deleteById(1));

        verify(comidaRepository).delete(comida);
    }

    // Comprueba que deleteById lanza NotFoundEntityException si la comida no existe
    @Test
    @DisplayName("deleteById con id inexistente lanza NotFoundEntityException")
    void deleteById_inexistente_lanza_excepcion() {
        when(comidaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundEntityException.class, () -> comidaService.deleteById(99));
    }

    // Comprueba que findByUsuarioId verifica propiedad y devuelve las comidas del usuario
    @Test
    @DisplayName("findByUsuarioId devuelve las comidas del usuario")
    void findByUsuarioId_devuelve_lista() {
        when(comidaRepository.findByUsuarioId(1)).thenReturn(List.of(comida));
        when(comidaMapper.toDTOList(any())).thenReturn(List.of(comidaDTO));

        List<ComidaDTO> result = comidaService.findByUsuarioId(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(securityUtils).checkOwnership(1);
    }

    // Comprueba que findByTipoComida (solo ADMIN) devuelve las comidas del tipo indicado
    @Test
    @DisplayName("findByTipoComida devuelve las comidas del tipo indicado")
    void findByTipoComida_devuelve_lista() {
        when(comidaRepository.findByTipoComida(TipoComida.DESAYUNO)).thenReturn(List.of(comida));
        when(comidaMapper.toDTOList(any())).thenReturn(List.of(comidaDTO));

        List<ComidaDTO> result = comidaService.findByTipoComida("DESAYUNO");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(securityUtils).requireAdmin();
    }

    // Comprueba que findByUsuarioIdAndFecha verifica propiedad y filtra por el día indicado
    @Test
    @DisplayName("findByUsuarioIdAndFecha devuelve las comidas del usuario en la fecha")
    void findByUsuarioIdAndFecha_devuelve_lista() {
        when(comidaRepository.findByUsuarioIdAndFechaBetween(any(), any(), any())).thenReturn(List.of(comida));
        when(comidaMapper.toDTOList(any())).thenReturn(List.of(comidaDTO));

        List<ComidaDTO> result = comidaService.findByUsuarioIdAndFecha(1, LocalDate.now());

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(securityUtils).checkOwnership(1);
    }

    // Comprueba que countByUsuarioId verifica propiedad y devuelve el número de comidas del usuario
    @Test
    @DisplayName("countByUsuarioId devuelve el número de comidas del usuario")
    void countByUsuarioId_devuelve_total() {
        when(comidaRepository.countByUsuarioId(1)).thenReturn(4L);

        Long result = comidaService.countByUsuarioId(1);

        assertEquals(4L, result);
        verify(securityUtils).checkOwnership(1);
    }

    // Comprueba que patch modifica solo los campos no nulos de la comida
    @Test
    @DisplayName("patch aplica solo los campos no nulos")
    void patch_aplica_campos_no_nulos() {
        ComidaPatchDTO patchDTO = new ComidaPatchDTO();
        patchDTO.setNotas("Comida ligera");

        when(comidaRepository.findById(1)).thenReturn(Optional.of(comida));
        when(comidaRepository.save(any())).thenReturn(comida);
        when(comidaMapper.toDTO(comida)).thenReturn(comidaDTO);

        ComidaDTO result = comidaService.patch(1, patchDTO);

        assertNotNull(result);
        assertEquals("Comida ligera", comida.getNotas());
    }
}
