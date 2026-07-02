package com.gymprofit.api.service.ejercicio;

import com.gymprofit.api.dto.entity.ejercicio.EjercicioCreateDTO;
import com.gymprofit.api.dto.entity.ejercicio.EjercicioDTO;
import com.gymprofit.api.dto.entity.ejercicio.EjercicioPatchDTO;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.enums.Dificultad;
import com.gymprofit.api.enums.GrupoMuscular;
import com.gymprofit.api.exceptions.CreateEntityException;
import com.gymprofit.api.exceptions.DeleteEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UpdateEntityException;
import com.gymprofit.api.mappers.EjercicioMapper;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// ============================================================
// EjercicioService — servicio del catálogo de ejercicios
// Gestiona el CRUD del catálogo global de ejercicios (nombre, grupo
// muscular, dificultad, instrucciones, etc.) usado para componer rutinas.
// No aplica control de propiedad porque el catálogo es compartido.
// ============================================================
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class EjercicioService implements IEjercicioService {

    private final IEjercicioRepository ejercicioRepository;
    private final EjercicioMapper ejercicioMapper;
    private final Logger logger = LoggerFactory.getLogger(EjercicioService.class);

    // Devuelve todos los ejercicios del catálogo.
    @Override
    public List<EjercicioDTO> findAll() {
        logger.info("Buscando todos los ejercicios");

        List<Ejercicio> ejercicios = ejercicioRepository.findAll();

        return ejercicioMapper.toDTOList(ejercicios);
    }

    // Busca un ejercicio por su id.
    @Override
    public EjercicioDTO findById(Integer id) {
        logger.info("Buscando ejercicio por id: {}", id);

        Ejercicio ejercicio = ejercicioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio con id " + id + " no existe"));

        return ejercicioMapper.toDTO(ejercicio);
    }

    // Crea un nuevo ejercicio, activándolo por defecto.
    @Override
    @Transactional
    public EjercicioDTO save(EjercicioCreateDTO ejercicioCreateDTO) {
        logger.info("Creando nuevo ejercicio {}", ejercicioCreateDTO.getNombre());

        try {
            Ejercicio ejercicio = ejercicioMapper.toEntity(ejercicioCreateDTO);
            ejercicio.setActivo(true);

            Ejercicio ejercicioGuardado = ejercicioRepository.save(ejercicio);

            return ejercicioMapper.toDTO(ejercicioGuardado);
        } catch (Exception e) {
            throw new CreateEntityException(Ejercicio.class.getSimpleName(), ejercicioCreateDTO, e);
        }
    }

    // Borrado lógico: desactiva el ejercicio en lugar de eliminarlo.
    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Desactivando ejercicio con id: {}", id);

        Ejercicio ejercicio = ejercicioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio con id " + id + " no existe"));

        try {
            ejercicio.setActivo(false);

            ejercicioRepository.save(ejercicio);

            logger.info("Ejercicio con id {} desactivado correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(Ejercicio.class.getSimpleName(), id, e);
        }
    }

    // Reactiva un ejercicio previamente desactivado.
    @Transactional
    @Override
    public void activateById(Integer id) {
        logger.info("Activando ejercicio con id: {}", id);

        Ejercicio ejercicio = ejercicioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio con id " + id + " no existe"));

        ejercicio.setActivo(true);
        ejercicioRepository.save(ejercicio);

        logger.info("Ejercicio con id {} activado correctamente", id);
    }

    // Elimina definitivamente el ejercicio de la base de datos.
    @Transactional
    @Override
    public void permanentDeleteById(Integer id) {
        logger.info("Eliminando permanentemente ejercicio con id: {}", id);

        Ejercicio ejercicio = ejercicioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio con id " + id + " no existe"));

        try {
            ejercicioRepository.delete(ejercicio);

            logger.info("Ejercicio con id {} eliminado permanentemente", id);
        } catch (Exception ex) {
            throw new DeleteEntityException(Ejercicio.class.getSimpleName(), id, ex);
        }
    }

    // Modifica un ejercicio existente con todos sus campos.
    @Override
    @Transactional
    public EjercicioDTO modify(EjercicioDTO ejercicioDTO) {
        logger.info("Modificando ejercicio con id: {}", ejercicioDTO.getId());

        Ejercicio ejercicio = ejercicioRepository.findById(ejercicioDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio con id " + ejercicioDTO.getId() + " no existe"));

        try {
            ejercicio.setNombre(ejercicioDTO.getNombre());
            ejercicio.setDescripcion(ejercicioDTO.getDescripcion());
            ejercicio.setGrupoMuscular(GrupoMuscular.valueOf(ejercicioDTO.getGrupoMuscular()));
            ejercicio.setDificultad(Dificultad.valueOf(ejercicioDTO.getDificultad()));
            ejercicio.setImagenUrl(ejercicioDTO.getImagenUrl());
            ejercicio.setInstrucciones(ejercicioDTO.getInstrucciones());
            ejercicio.setCaloriasQuemadas(ejercicioDTO.getCaloriasQuemadas());
            ejercicio.setEquipoNecesario(ejercicioDTO.getEquipoNecesario());
            ejercicio.setActivo(ejercicioDTO.getActivo());

            Ejercicio ejercicioActualizado = ejercicioRepository.save(ejercicio);

            return ejercicioMapper.toDTO(ejercicioActualizado);
        } catch (Exception e) {
            throw new UpdateEntityException(Ejercicio.class.getSimpleName(), ejercicioDTO, e);
        }
    }

    // Busca ejercicios por grupo muscular.
    @Override
    public List<EjercicioDTO> findByGrupoMuscular(String grupoMuscular) {
        logger.info("Buscando ejercicios por grupo muscular: {}", grupoMuscular);

        GrupoMuscular grupo = GrupoMuscular.valueOf(grupoMuscular.toUpperCase());

        List<Ejercicio> ejercicios = ejercicioRepository.findByGrupoMuscular(grupo);

        return ejercicioMapper.toDTOList(ejercicios);
    }

    // Busca ejercicios por nivel de dificultad.
    @Override
    public List<EjercicioDTO> findByDificultad(String dificultad) {
        logger.info("Buscando ejercicios por dificultad: {}", dificultad);

        Dificultad dif = Dificultad.valueOf(dificultad.toUpperCase());

        List<Ejercicio> ejercicios = ejercicioRepository.findByDificultad(dif);

        return ejercicioMapper.toDTOList(ejercicios);
    }

    // Busca ejercicios cuyo nombre contenga el texto indicado (ignora mayúsculas).
    @Override
    public List<EjercicioDTO> findByNombre(String nombre) {
        logger.info("Buscando ejercicios por nombre: {}", nombre);

        List<Ejercicio> ejercicios = ejercicioRepository.findByNombreContainingIgnoreCase(nombre);

        return ejercicioMapper.toDTOList(ejercicios);
    }

    // Devuelve solo los ejercicios activos.
    @Override
    public List<EjercicioDTO> findActivos() {
        logger.info("Buscando ejercicios activos");

        List<Ejercicio> ejercicios = ejercicioRepository.findByActivoTrue();

        return ejercicioMapper.toDTOList(ejercicios);
    }

    // Aplica una actualización parcial sobre un ejercicio (solo campos no nulos).
    @Transactional
    @Override
    public EjercicioDTO patch(Integer id, EjercicioPatchDTO patchDTO) {
        logger.info("Aplicando patch a ejercicio con id: {}", id);

        Ejercicio ejercicio = ejercicioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio con id " + id + " no existe"));

        try {
            if (patchDTO.getNombre() != null) ejercicio.setNombre(patchDTO.getNombre());
            if (patchDTO.getDescripcion() != null) ejercicio.setDescripcion(patchDTO.getDescripcion());
            if (patchDTO.getGrupoMuscular() != null)
                ejercicio.setGrupoMuscular(GrupoMuscular.valueOf(patchDTO.getGrupoMuscular().toUpperCase()));
            if (patchDTO.getDificultad() != null)
                ejercicio.setDificultad(Dificultad.valueOf(patchDTO.getDificultad().toUpperCase()));
            if (patchDTO.getImagenUrl() != null) ejercicio.setImagenUrl(patchDTO.getImagenUrl());
            if (patchDTO.getInstrucciones() != null) ejercicio.setInstrucciones(patchDTO.getInstrucciones());
            if (patchDTO.getCaloriasQuemadas() != null) ejercicio.setCaloriasQuemadas(patchDTO.getCaloriasQuemadas());
            if (patchDTO.getEquipoNecesario() != null) ejercicio.setEquipoNecesario(patchDTO.getEquipoNecesario());
            if (patchDTO.getActivo() != null) ejercicio.setActivo(patchDTO.getActivo());

            return ejercicioMapper.toDTO(ejercicioRepository.save(ejercicio));
        } catch (Exception e) {
            throw new UpdateEntityException(Ejercicio.class.getSimpleName(), id, e);
        }
    }
}
