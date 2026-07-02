package com.gymprofit.api.service.alimento;

import com.gymprofit.api.dto.entity.alimento.AlimentoCreateDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoPatchDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.CreateEntityException;
import com.gymprofit.api.exceptions.DeleteEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UpdateEntityException;
import com.gymprofit.api.mappers.AlimentoMapper;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// ============================================================
// AlimentoService — lógica de negocio de los alimentos del sistema
// Implementa el CRUD y las búsquedas/filtros de los alimentos usados en el
// módulo de nutrición de GymProFit (creación, baja lógica, activación,
// borrado permanente, patch parcial y consultas por nombre/categoría/calorías).
// ============================================================
@Service
@AllArgsConstructor
public class AlimentoService implements IAlimentoService {

    private final IAlimentoRepository alimentoRepository;
    private final AlimentoMapper alimentoMapper;
    private final IUsuarioRepository usuarioRepository;
    private final Logger logger = LoggerFactory.getLogger(AlimentoService.class);

    // Devuelve todos los alimentos existentes (activos e inactivos).
    @Override
    public List<AlimentoDTO> findAll() {
        logger.info("Buscando todos los alimentos");

        List<Alimento> alimentos = (List<Alimento>) alimentoRepository.findAll();

        return alimentoMapper.toDTOList(alimentos);
    }

    // Busca un alimento por id o lanza NotFoundEntityException si no existe.
    @Override
    public AlimentoDTO findById(Integer id) {
        logger.info("Buscando alimento por id: {}", id);

        Alimento alimento = alimentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El alimento con id " + id + " no existe"));

        return alimentoMapper.toDTO(alimento);
    }

    // Crea un alimento nuevo, activo por defecto; si trae usuarioId lo asocia
    // al usuario creador (alimento personalizado, no global).
    @Override
    public AlimentoDTO save(AlimentoCreateDTO alimentoCreateDTO) {
        logger.info("Creando nuevo alimento: {}", alimentoCreateDTO.getNombre());

        try {
            Alimento alimento = alimentoMapper.toEntity(alimentoCreateDTO);
            alimento.setActivo(true);

            if (alimentoCreateDTO.getUsuarioId() != null) {
                Usuario usuario = usuarioRepository.findById(alimentoCreateDTO.getUsuarioId())
                        .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + alimentoCreateDTO.getUsuarioId() + " no existe"));
                alimento.setUsuario(usuario);
            }

            Alimento alimentoGuardado = alimentoRepository.save(alimento);

            return alimentoMapper.toDTO(alimentoGuardado);
        } catch (Exception ex) {
            throw new CreateEntityException(Alimento.class.getSimpleName(), alimentoCreateDTO, ex);
        }
    }

    // Baja lógica: desactiva el alimento (activo = false) sin borrarlo de la BD.
    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Desactivando alimento con id: {}", id);

        Alimento alimento = alimentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El alimento con id " + id + " no existe"));

        try {
            alimento.setActivo(false);
            alimentoRepository.save(alimento);

            logger.info("Alimento con id {} desactivado correctamente", id);
        } catch (Exception ex) {
            throw new DeleteEntityException(Alimento.class.getSimpleName(), id, ex);
        }
    }

    // Reactiva un alimento previamente desactivado.
    @Transactional
    @Override
    public void activateById(Integer id) {
        logger.info("Activando alimento con id: {}", id);

        Alimento alimento = alimentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El alimento con id " + id + " no existe"));

        try {
            alimento.setActivo(true);
            alimentoRepository.save(alimento);

            logger.info("Alimento con id {} activado correctamente", id);
        } catch (Exception ex) {
            throw new UpdateEntityException(Alimento.class.getSimpleName(), id, ex);
        }
    }

    // Borrado físico definitivo del alimento en la base de datos.
    @Transactional
    @Override
    public void permanentDeleteById(Integer id) {
        logger.info("Eliminando permanentemente alimento con id: {}", id);

        Alimento alimento = alimentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El alimento con id " + id + " no existe"));

        try {
            alimentoRepository.delete(alimento);

            logger.info("Alimento con id {} eliminado permanentemente", id);
        } catch (Exception ex) {
            throw new DeleteEntityException(Alimento.class.getSimpleName(), id, ex);
        }
    }

    // Sustituye todos los campos editables del alimento por los del DTO recibido.
    @Override
    public AlimentoDTO modify(AlimentoDTO alimentoDTO) {
        logger.info("Modificando alimento con id: {}", alimentoDTO.getId());

        Alimento alimento = alimentoRepository.findById(alimentoDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("El alimento con id " + alimentoDTO.getId() + " no existe"));

        try {
            // Mapear los cambios del DTO a la entidad
            alimento.setNombre(alimentoDTO.getNombre());
            alimento.setCategoria(alimentoDTO.getCategoria());
            alimento.setCalorias(alimentoDTO.getCalorias());
            alimento.setProteinas(alimentoDTO.getProteinas());
            alimento.setCarbohidratos(alimentoDTO.getCarbohidratos());
            alimento.setGrasas(alimentoDTO.getGrasas());
            alimento.setFibra(alimentoDTO.getFibra());
            alimento.setActivo(alimentoDTO.getActivo());  // ← Permitir modificar estado

            Alimento alimentoActualizado = alimentoRepository.save(alimento);

            return alimentoMapper.toDTO(alimentoActualizado);
        } catch (Exception ex) {
            throw new UpdateEntityException(Alimento.class.getSimpleName(), alimentoDTO, ex);
        }
    }

    // Búsqueda de alimentos cuyo nombre contiene el texto dado.
    @Override
    public List<AlimentoDTO> findByNombre(String nombre) {
        logger.info("Buscando alimentos por nombre: {}", nombre);

        List<Alimento> alimentos = alimentoRepository.findByNombreContainingIgnoreCase(nombre);

        return alimentoMapper.toDTOList(alimentos);
    }

    // Alimentos filtrados por categoría exacta.
    @Override
    public List<AlimentoDTO> findByCategoria(String categoria) {
        logger.info("Buscando alimentos por categoria: {}", categoria);

        List<Alimento> alimentos = alimentoRepository.findByCategoria(categoria);

        return alimentoMapper.toDTOList(alimentos);
    }

    // Devuelve únicamente los alimentos activos.
    @Override
    public List<AlimentoDTO> findActivos() {
        logger.info("Buscando alimentos activos");

        List<Alimento> alimentos = alimentoRepository.findByActivoTrue();

        return alimentoMapper.toDTOList(alimentos);
    }

    // Alimentos cuyas calorías están dentro del rango [min, max].
    @Override
    public List<AlimentoDTO> findByCaloriasBetween(Integer min, Integer max) {
        logger.info("Buscando alimentos con calorias entre {} y {}", min, max);

        List<Alimento> alimentos = alimentoRepository.findByCaloriasBetween(min, max);

        return alimentoMapper.toDTOList(alimentos);
    }

    // Número total de alimentos activos.
    @Override
    public Long countActivos() {
        logger.info("Contando alimentos activos");

        return alimentoRepository.countByActivoTrue();
    }

    // Número de alimentos que pertenecen a una categoría.
    @Override
    public Long countByCategoria(String categoria) {
        logger.info("Contando alimmentos por categoría: {}", categoria);

        return alimentoRepository.countByCategoria(categoria);
    }

    // Alimentos personalizados creados por un usuario concreto.
    @Override
    public List<AlimentoDTO> findByUsuarioId(Integer usuarioId) {
        logger.info("Buscando alimentos del usuario con id: {}", usuarioId);

        List<Alimento> alimentos = alimentoRepository.findByUsuarioId(usuarioId);

        return alimentoMapper.toDTOList(alimentos);
    }

    // Actualización parcial: solo modifica los campos no nulos del patchDTO.
    @Transactional
    @Override
    public AlimentoDTO patch(Integer id, AlimentoPatchDTO patchDTO) {
        logger.info("Aplicando patch a alimento con id: {}", id);

        Alimento alimento = alimentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El alimento con id " + id + " no existe"));

        try {
            if (patchDTO.getNombre() != null) alimento.setNombre(patchDTO.getNombre());
            if (patchDTO.getCategoria() != null) alimento.setCategoria(patchDTO.getCategoria());
            if (patchDTO.getCalorias() != null) alimento.setCalorias(patchDTO.getCalorias());
            if (patchDTO.getProteinas() != null) alimento.setProteinas(patchDTO.getProteinas());
            if (patchDTO.getCarbohidratos() != null) alimento.setCarbohidratos(patchDTO.getCarbohidratos());
            if (patchDTO.getGrasas() != null) alimento.setGrasas(patchDTO.getGrasas());
            if (patchDTO.getFibra() != null) alimento.setFibra(patchDTO.getFibra());
            if (patchDTO.getPorcionGramos() != null) alimento.setPorcionGramos(patchDTO.getPorcionGramos());
            if (patchDTO.getDescripcion() != null) alimento.setDescripcion(patchDTO.getDescripcion());
            if (patchDTO.getActivo() != null) alimento.setActivo(patchDTO.getActivo());

            return alimentoMapper.toDTO(alimentoRepository.save(alimento));
        } catch (Exception e) {
            throw new UpdateEntityException(Alimento.class.getSimpleName(), id, e);
        }
    }
}