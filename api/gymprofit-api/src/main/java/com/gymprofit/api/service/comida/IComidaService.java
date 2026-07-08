package com.gymprofit.api.service.comida;

import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.dto.entity.comida.ComidaDTO;
import com.gymprofit.api.dto.entity.comida.ResumenDiarioNutricionDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// IComidaService — contrato del servicio de comidas registradas por el usuario
// Define las operaciones CRUD y de consulta sobre las comidas (desayuno,
// comida, cena, etc.) que forman el diario nutricional de cada usuario.
// ============================================================
public interface IComidaService {

    // Devuelve todas las comidas (solo ADMIN).
    List<ComidaDTO> findAll();

    // Busca una comida por su id.
    ComidaDTO findById(Integer id);
    // Crea una nueva comida.
    ComidaDTO save(ComidaCreateDTO comidaCreateDTO);
    // Modifica una comida existente.
    ComidaDTO modify(ComidaDTO comidaDTO);

    // Elimina una comida por su id.
    void deleteById(Integer id);

    // Busca todas las comidas de un usuario.
    List<ComidaDTO> findByUsuarioId(Integer usuarioId);
    // Busca comidas por tipo (desayuno, comida, cena, etc.).
    List<ComidaDTO> findByTipoComida(String tipoComida);
    // Busca comidas registradas en una fecha concreta.
    List<ComidaDTO> findByFecha(LocalDate fecha);
    // Busca comidas de un usuario en una fecha concreta.
    List<ComidaDTO> findByUsuarioIdAndFecha(Integer usuarioId, LocalDate fecha);
    // Resumen nutricional diario (kcal+macros sumados por día) de un usuario en un rango de fechas.
    List<ResumenDiarioNutricionDTO> obtenerResumenDiario(Integer usuarioId, LocalDate inicio, LocalDate fin);
    // Busca comidas de un usuario filtradas por tipo.
    List<ComidaDTO> findByUsuarioIdAndTipoComida(Integer usuarioId, String tipoComida);

    // Cuenta las comidas de un usuario.
    Long countByUsuarioId(Integer usuarioId);
    // Cuenta las comidas de un tipo determinado.
    Long countByTipoComida(String tipoComida);
    // Cuenta las comidas de un usuario filtradas por tipo.
    Long countByUsuarioIdAndTipoComida(Integer usuarioId, String tipoComida);

    // Aplica una actualización parcial (PATCH) sobre una comida.
    ComidaDTO patch(Integer id, com.gymprofit.api.dto.entity.comida.ComidaPatchDTO patchDTO);
}
