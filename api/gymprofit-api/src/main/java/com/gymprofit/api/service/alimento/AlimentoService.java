package com.gymprofit.api.service.alimento;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.common.PageDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoCreateDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoPatchDTO;
import com.gymprofit.api.dto.jooq.AlimentoJooqDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.CreateEntityException;
import com.gymprofit.api.exceptions.DeleteEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UpdateEntityException;
import com.gymprofit.api.mappers.AlimentoMapper;
import com.gymprofit.api.repository.jooq.alimento.IAlimentoJooqRepository;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.externo.OpenFoodFactsClient;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@Transactional(readOnly = true)
public class AlimentoService implements IAlimentoService {

    private final IAlimentoRepository alimentoRepository;
    private final IAlimentoJooqRepository alimentoJooqRepository;
    private final AlimentoMapper alimentoMapper;
    private final IUsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;
    private final OpenFoodFactsClient openFoodFactsClient;
    private final Logger logger = LoggerFactory.getLogger(AlimentoService.class);

    // Devuelve todos los alimentos existentes (activos e inactivos).
    @Override
    public List<AlimentoDTO> findAll() {
        logger.info("Buscando todos los alimentos");

        List<Alimento> alimentos = alimentoRepository.findAll();

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
    @Transactional
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
    @Transactional
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

    // Búsqueda de alimentos para el panel admin (incluye inactivos) mediante jOOQ.
    @Override
    @Transactional(readOnly = true)
    public List<AlimentoJooqDTO> busquedaAdmin(String nombre, String categoria, Boolean activo) {
        logger.info("Búsqueda admin de alimentos");

        return alimentoJooqRepository.busquedaAdmin(nombre, categoria, activo);
    }

    // Búsqueda paginada del catálogo. Sin texto → alimentos locales (propios
    // del usuario + importados) como siempre. Con texto → búsqueda EN VIVO en
    // Open Food Facts (catálogo real de productos) anteponiendo en la primera
    // página los alimentos propios del usuario que coincidan. El usuarioId
    // sale SIEMPRE del token (nunca del cliente).
    @Override
    public PageDTO<AlimentoDTO> buscarCatalogo(String q, String categoria, int page, int size) {
        logger.info("Búsqueda paginada de alimentos: q={}, categoria={}, page={}, size={}", q, categoria, page, size);

        // Normalizar filtros: blanco → null (sin filtro)
        String texto = (q == null || q.isBlank()) ? null : q.trim();
        String cat = (categoria == null || categoria.isBlank()) ? null : categoria.trim();
        int pagina = Math.max(0, page);
        int tam = Math.min(Math.max(1, size), 100);

        Integer usuarioId = securityUtils.getCurrentUserId();

        // Sin texto: solo catálogo local (propios + importados), paginado en BD
        if (texto == null) {
            Page<Alimento> resultado = alimentoRepository.buscarCatalogo(
                    null, cat, usuarioId, PageRequest.of(pagina, tam));
            return PageDTO.of(resultado, alimentoMapper.toDTOList(resultado.getContent()));
        }

        // Con texto: Open Food Facts en vivo + propios del usuario delante (página 0)
        OpenFoodFactsClient.BusquedaExterna externa = openFoodFactsClient.buscar(texto, pagina, tam);

        List<AlimentoDTO> contenido = new java.util.ArrayList<>();
        if (pagina == 0) {
            contenido.addAll(alimentoMapper.toDTOList(alimentoRepository.buscarPropios(texto, usuarioId)));
        }
        // Dedup por barcode: si un producto OFF ya está importado en local se
        // resuelve al importar (el upsert devuelve la fila existente)
        contenido.addAll(externa.alimentos());

        long total = externa.totalElements() + (pagina == 0 ? 0 : 0);
        int totalPaginas = (int) Math.ceil((double) Math.max(total, contenido.size()) / tam);
        boolean ultima = (long) (pagina + 1) * tam >= total;

        return new PageDTO<>(contenido, pagina, tam, Math.max(total, contenido.size()),
                Math.max(totalPaginas, 1), ultima);
    }

    // Importa (o recupera si ya existe) un producto de Open Food Facts a la BD
    // local por su código de barras. Necesario porque las comidas referencian
    // alimentos por FK: el producto externo se materializa al seleccionarlo.
    @Override
    @Transactional
    public AlimentoDTO importarPorBarcode(String barcode) {
        logger.info("Importando alimento OFF por barcode: {}", barcode);

        if (barcode == null || barcode.isBlank()) {
            throw new com.gymprofit.api.exceptions.InvalidDataException("El código de barras es obligatorio");
        }
        String codigo = barcode.trim();

        // Ya importado → devolver el existente (idempotente)
        java.util.Optional<Alimento> existente = alimentoRepository.findByBarcode(codigo);
        if (existente.isPresent()) {
            Alimento alimento = existente.get();
            // Reactivar si un admin lo había desactivado y el usuario lo vuelve a elegir
            if (!Boolean.TRUE.equals(alimento.getActivo())) {
                alimento.setActivo(true);
                alimentoRepository.save(alimento);
            }
            return alimentoMapper.toDTO(alimento);
        }

        AlimentoDTO dto = openFoodFactsClient.porBarcode(codigo)
                .orElseThrow(() -> new NotFoundEntityException(
                        "No existe producto con código de barras " + codigo + " en Open Food Facts"));

        Alimento alimento = new Alimento();
        alimento.setNombre(dto.getNombre());
        alimento.setBarcode(dto.getBarcode());
        alimento.setMarca(dto.getMarca());
        alimento.setCalorias(dto.getCalorias());
        alimento.setProteinas(dto.getProteinas());
        alimento.setCarbohidratos(dto.getCarbohidratos());
        alimento.setGrasas(dto.getGrasas());
        alimento.setFibra(dto.getFibra());
        alimento.setPorcionGramos(dto.getPorcionGramos());
        alimento.setActivo(true);
        // usuario null → alimento global (visible para todos los usuarios)

        return alimentoMapper.toDTO(alimentoRepository.save(alimento));
    }
}