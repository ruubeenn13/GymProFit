package com.gymprofit.api.service.alimentocomida;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaCreateDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaPatchDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoComida;
import com.gymprofit.api.entity.AlimentoRacion;
import com.gymprofit.api.entity.Comida;
import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.exceptions.*;
import com.gymprofit.api.mappers.AlimentoComidaMapper;
import com.gymprofit.api.repository.jpa.IAlimentoComidaRepository;
import com.gymprofit.api.repository.jpa.IAlimentoRacionRepository;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IComidaRepository;
import com.gymprofit.api.service.plato.Platos;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// ============================================================
// AlimentoComidaService — servicio de la relación alimento-comida
// Gestiona las líneas que asocian un alimento a una comida con una cantidad
// en gramos, calculando calorías por línea y recalculando los totales
// (calorías, proteínas, carbohidratos, grasas) de la comida asociada.
// ============================================================
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class AlimentoComidaService implements IAlimentoComidaService {

    private final IAlimentoComidaRepository alimentoComidaRepository;
    private final IComidaRepository comidaRepository;
    private final IAlimentoRepository alimentoRepository;
    private final IAlimentoRacionRepository racionRepository;
    private final AlimentoComidaMapper alimentoComidaMapper;
    private final SecurityUtils securityUtils;
    private final Logger logger = LoggerFactory.getLogger(AlimentoComidaService.class);


    // Las relaciones alimento-comida de quien llama (requiere rol ADMIN). Las de los demás
    // son de su diario, solo suyas también para un ADMIN (DEC-042).
    @Override
    public List<AlimentoComidaDTO> findAll() {
        logger.info("Buscando todos los alimentos-comidas");
        securityUtils.requireAdmin();

        List<AlimentoComida> alimentosComida = propias(alimentoComidaRepository.findAll());

        return alimentoComidaMapper.toDTOList(alimentosComida);
    }

    // Busca una relación por id y verifica que el usuario autenticado es
    // propietario de la comida asociada, también si es ADMIN (DEC-042).
    @Override
    public AlimentoComidaDTO findById(Integer id) {
        logger.info("Buscando alimento-comida por id: {}", id);

        AlimentoComida alimentoComida = alimentoComidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("error.alimentoComida.noExiste", id));

        securityUtils.checkDueno(alimentoComida.getComida().getUsuario().getId());

        return alimentoComidaMapper.toDTO(alimentoComida);
    }

    // Crea una nueva relación alimento-comida: valida que no exista ya la
    // combinación comida+alimento, calcula las calorías de la línea y
    // recalcula los totales nutricionales de la comida.
    @Override
    @Transactional
    public AlimentoComidaDTO save(AlimentoComidaCreateDTO alimentoComidaCreateDTO) {
        logger.info("Creando nuevo alimento-comida para comida id: {} y alimento id: {}", alimentoComidaCreateDTO.getAlimentoId(), alimentoComidaCreateDTO.getComidaId());

        try {
            Comida comida = comidaRepository.findById(alimentoComidaCreateDTO.getComidaId())
                    .orElseThrow(() -> new NotFoundEntityException("error.comida.noExiste", alimentoComidaCreateDTO.getComidaId()));

            securityUtils.checkDueno(comida.getUsuario().getId());

            Alimento alimento = alimentoRepository.findById(alimentoComidaCreateDTO.getAlimentoId())
                    .orElseThrow(() -> new NotFoundEntityException("error.alimento.noExiste", alimentoComidaCreateDTO.getAlimentoId()));

            checkAlimentoAccesible(alimento);

            if (alimentoComidaRepository.existsByComidaIdAndAlimentoId(
                    alimentoComidaCreateDTO.getComidaId(),
                    alimentoComidaCreateDTO.getAlimentoId())) {
                throw new DuplicateEntityException("error.alimentoComida.duplicado");
            }

            AlimentoComida alimentoComida = alimentoComidaMapper.toEntity(alimentoComidaCreateDTO);
            alimentoComida.setComida(comida);
            alimentoComida.setAlimento(alimento);
            ponerGramos(alimentoComida, alimentoComidaCreateDTO.getCantidadGramos());

            AlimentoComida alimentoComidaGuardado = alimentoComidaRepository.save(alimentoComida);
            recalcularTotalesComida(comida);

            return alimentoComidaMapper.toDTO(alimentoComidaGuardado);
        } catch (NotFoundEntityException | DuplicateEntityException | UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new CreateEntityException(AlimentoComida.class.getSimpleName(), alimentoComidaCreateDTO, e);
        }
    }

    // Modifica una relación existente (comida, alimento y cantidad) y
    // recalcula sus calorías y los totales de la comida.
    @Override
    @Transactional
    public AlimentoComidaDTO modify(AlimentoComidaDTO alimentoComidaDTO) {
        logger.info("Modificando alimento-comida con id: {}", alimentoComidaDTO.getId());

        AlimentoComida alimentoComida = alimentoComidaRepository.findById(alimentoComidaDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("error.alimentoComida.noExiste", alimentoComidaDTO.getId()));

        securityUtils.checkDueno(alimentoComida.getComida().getUsuario().getId());

        try {
            Comida comida = comidaRepository.findById(alimentoComidaDTO.getComidaId())
                    .orElseThrow(() -> new NotFoundEntityException("error.comida.noExiste", alimentoComidaDTO.getComidaId()));

            securityUtils.checkDueno(comida.getUsuario().getId());

            Alimento alimento = alimentoRepository.findById(alimentoComidaDTO.getAlimentoId())
                    .orElseThrow(() -> new NotFoundEntityException("error.alimento.noExiste", alimentoComidaDTO.getAlimentoId()));

            checkAlimentoAccesible(alimento);

            alimentoComida.setComida(comida);
            alimentoComida.setAlimento(alimento);
            alimentoComida.setRacion(null);
            alimentoComida.setRaciones(null);
            ponerGramos(alimentoComida, alimentoComidaDTO.getCantidadGramos());

            AlimentoComida alimentoComidaActualizado = alimentoComidaRepository.save(alimentoComida);
            recalcularTotalesComida(comida);

            return alimentoComidaMapper.toDTO(alimentoComidaActualizado);
        } catch (NotFoundEntityException | UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UpdateEntityException(AlimentoComida.class.getSimpleName(), alimentoComidaDTO, e);
        }
    }

    // Elimina una relación y recalcula los totales nutricionales de la comida.
    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Eliminando alimnto-comida con id: {}", id);

        AlimentoComida alimentoComida = alimentoComidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("error.alimentoComida.noExiste", id));

        Comida comida = alimentoComida.getComida();
        securityUtils.checkDueno(comida.getUsuario().getId());
        try {
            alimentoComidaRepository.delete(alimentoComida);
            recalcularTotalesComida(comida);

            logger.info("Alimento-comida con id {} eliminado correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(AlimentoComida.class.getSimpleName(), id, e);
        }
    }

    // Busca todos los alimentos de una comida (verifica propiedad de la comida).
    @Override
    public List<AlimentoComidaDTO> findByComidaId(Integer comidaId) {
        logger.info("Buscando alimentos de la comida id: {}", comidaId);
        checkComidaOwnership(comidaId);

        List<AlimentoComida> alimentosComida = alimentoComidaRepository.findByComidaId(comidaId);

        return alimentoComidaMapper.toDTOList(alimentosComida);
    }

    // Las comidas de quien llama que contienen un alimento (requiere ADMIN, DEC-042).
    @Override
    public List<AlimentoComidaDTO> findByAlimentoId(Integer alimentoId) {
        logger.info("Buscando comidas que contienen el alimento id: {}", alimentoId);
        securityUtils.requireAdmin();

        exigirAlimentoExistente(alimentoId);

        List<AlimentoComida> alimentosComida = propias(alimentoComidaRepository.findByAlimentoId(alimentoId));

        return alimentoComidaMapper.toDTOList(alimentosComida);
    }

    // Busca la relación entre una comida y un alimento concretos.
    @Override
    public AlimentoComidaDTO findByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId) {
        logger.info("Buscando relación entre comida {} y alimento {}", comidaId, alimentoId);
        checkComidaOwnership(comidaId);

        AlimentoComida alimentoComida = alimentoComidaRepository.findByComidaIdAndAlimentoId(comidaId, alimentoId)
                .orElseThrow(() -> new NotFoundEntityException("error.alimentoComida.sinRelacion", comidaId, alimentoId));

        return alimentoComidaMapper.toDTO(alimentoComida);
    }

    // Elimina todos los alimentos asociados a una comida (p.ej. al borrar la comida).
    @Transactional
    @Override
    public void deleteByComidaId(Integer comidaId) {
        logger.info("Eliminando todos los alimentos de la comida id: {}", comidaId);
        checkComidaOwnership(comidaId);

        try {
            alimentoComidaRepository.deleteByComidaId(comidaId);

            logger.info("Alimentos de la comida {} eliminados correctamente", comidaId);
        } catch (Exception e) {
            throw new DeleteEntityException("AlimentoComida", comidaId, e);
        }
    }

    // Elimina la relación entre una comida y un alimento concretos.
    @Transactional
    @Override
    public void deleteByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId) {
        logger.info("Eliminando alimento {} de la comida {}", alimentoId, comidaId);
        checkComidaOwnership(comidaId);

        try {
            alimentoComidaRepository.deleteByComidaIdAndAlimentoId(comidaId, alimentoId);

            logger.info("Alimento {} eliminado de la comida {} correctamente", alimentoId, comidaId);
        } catch (Exception e) {
            throw new DeleteEntityException("AlimentoComida", comidaId, e);
        }
    }

    // Comprueba si ya existe relación entre una comida y un alimento.
    @Override
    public boolean existsByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId) {
        logger.info("Verificando si existe la relación entre la comida {} y el alimento {}", comidaId, alimentoId);
        checkComidaOwnership(comidaId);

        return alimentoComidaRepository.existsByComidaIdAndAlimentoId(comidaId, alimentoId);
    }

    // Cuenta los alimentos asociados a una comida.
    @Override
    public Long countByComidaId(Integer comidaId) {
        logger.info("Contando alimentos de la comida id: {}", comidaId);
        checkComidaOwnership(comidaId);

        return alimentoComidaRepository.countByComidaId(comidaId);
    }

    // Cuenta las comidas de quien llama que contienen un alimento (requiere ADMIN, DEC-042).
    @Override
    public Long countByAlimentoId(Integer alimentoId) {
        logger.info("Contando comidas que contienen el alimento id: {}", alimentoId);
        securityUtils.requireAdmin();

        exigirAlimentoExistente(alimentoId);

        return (long) propias(alimentoComidaRepository.findByAlimentoId(alimentoId)).size();
    }

    // Aplica una actualización parcial: la cantidad nueva recalcula la línea entera con
    // los valores de hoy del alimento (lote 1.6.6, A1). Unas kcal escritas por el cliente
    // ya no se aceptan: una línea guarda kcal y macros de la misma cuenta, y ninguna build
    // las mandaba. Recalcula totales.
    @Transactional
    @Override
    public AlimentoComidaDTO patch(Integer id, AlimentoComidaPatchDTO patchDTO) {
        logger.info("Aplicando patch a alimento-comida con id: {}", id);

        AlimentoComida alimentoComida = alimentoComidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("error.alimentoComida.noExiste", id));

        securityUtils.checkDueno(alimentoComida.getComida().getUsuario().getId());

        // Fuera del try: una ración mal pedida es un 400, no un error de actualización.
        boolean conRacion = patchDTO.getRacionId() != null || patchDTO.getRaciones() != null;
        if (!conRacion && patchDTO.getCantidadGramos() == null && patchDTO.getCaloriasTotales() != null) {
            throw new InvalidDataException("error.linea.caloriasAMano");
        }
        if (!conRacion && patchDTO.getCantidadGramos() != null && patchDTO.getCantidadGramos().signum() <= 0) {
            throw new InvalidDataException("error.anadir.cantidad");
        }
        if (conRacion) {
            ponerCantidad(alimentoComida, patchDTO.getCantidadGramos(), patchDTO.getRacionId(), patchDTO.getRaciones());
        }

        try {
            if (conRacion) {
                // Ya puesta arriba, con sus calorías.
            } else if (patchDTO.getCantidadGramos() != null) {
                // Solo gramos: ya no es «2 rebanadas».
                alimentoComida.setRacion(null);
                alimentoComida.setRaciones(null);
                ponerGramos(alimentoComida, patchDTO.getCantidadGramos());
            }

            AlimentoComida saved = alimentoComidaRepository.save(alimentoComida);
            recalcularTotalesComida(saved.getComida());
            return alimentoComidaMapper.toDTO(saved);
        } catch (Exception e) {
            throw new UpdateEntityException(AlimentoComida.class.getSimpleName(), id, e);
        }
    }

    /**
     * Carga la comida indicada y verifica que el usuario autenticado es su propietario, también
     * si es ADMIN (DEC-042).
     * Propiedad indirecta: el recurso alimento-comida hereda el owner de su comida.
     *
     * @param comidaId id de la comida a la que pertenece(rá) la línea.
     * @throws NotFoundEntityException si la comida no existe.
     * @throws com.gymprofit.api.exceptions.UnauthorizedException (→ 403) si no es propietario.
     */
    private void checkComidaOwnership(Integer comidaId) {
        Comida comida = comidaRepository.findById(comidaId)
                .orElseThrow(() -> new NotFoundEntityException("error.comida.noExiste", comidaId));
        securityUtils.checkDueno(comida.getUsuario().getId());
    }

    // Solo las líneas de las comidas de quien llama (DEC-042).
    private List<AlimentoComida> propias(List<AlimentoComida> lineas) {
        Integer yo = securityUtils.getCurrentUserId();
        return lineas.stream()
                .filter(l -> l.getComida().getUsuario().getId().equals(yo))
                .toList();
    }

    /**
     * Verifica que el alimento que se va a meter en la comida se le puede enseñar a quien llama.
     * <p>
     * El id del alimento llega en el cuerpo de la petición, así que es un id elegido por el
     * cliente y hay que tratarlo como tal. Un alimento del catálogo (dueño NULL) es de todos
     * y se permite: es el caso normal. Uno creado por otro usuario es suyo, y aceptarlo aquí
     * sería una IDOR con forma de escritura, porque {@code AlimentoComidaDTO} devuelve su
     * nombre, su categoría, su {@code usuarioIdAlimento} y sus macros. Criterio de DEC-027:
     * el id tiene dueño, luego 403, también a un ADMIN (DEC-042).
     *
     * @param alimento alimento ya cargado que se quiere asociar a la comida.
     * @throws com.gymprofit.api.exceptions.UnauthorizedException (→ 403) si es de otro usuario.
     */
    private void checkAlimentoAccesible(Alimento alimento) {
        securityUtils.checkDuenoSiLoTiene(
                alimento.getUsuario() == null ? null : alimento.getUsuario().getId());
    }

    /**
     * Pone la cantidad de una línea elegida por raciones (lote 1.6.1): la ración y cuántas,
     * y los gramos y las calorías. Los gramos que lleguen mandan; si no llegan, salen de la
     * ración. La ración tiene que ser de ese alimento.
     *
     * @param linea    la línea, con su alimento.
     * @param gramos   gramos pedidos, o null para calcularlos.
     * @param racionId ración elegida; obligatoria con {@code raciones}.
     * @param raciones cuántas, mayor que 0; obligatoria con {@code racionId}.
     * @throws InvalidDataException (400) si falta una de las dos o la ración es de otro alimento.
     */
    @Override
    public void ponerCantidad(AlimentoComida linea, BigDecimal gramos, Integer racionId, BigDecimal raciones) {
        if (racionId == null || raciones == null || raciones.signum() <= 0) {
            throw new InvalidDataException("error.racion.incompleta");
        }
        if (gramos != null && gramos.signum() <= 0) {
            throw new InvalidDataException("error.racion.incompleta");
        }
        AlimentoRacion racion = racionRepository.findById(racionId)
                .filter(r -> r.getAlimento().getId().equals(linea.getAlimento().getId()))
                .orElseThrow(() -> new InvalidDataException("error.racion.otroAlimento", racionId));
        // Un plato (lote 1.6.6): «2 raciones» son dos de las que tiene hoy, venga la que
        // venga (la de otra build, la de una línea copiada de antes de cambiarlo), y sus
        // gramos salen siempre de ella: los de un plato sin peso no son de verdad.
        if (Platos.esPlato(linea.getAlimento())) {
            AlimentoRacion actual = Platos.racionActual(linea.getAlimento());
            if (actual == null) throw new InvalidDataException("error.racion.otroAlimento", racionId);
            racion = actual;
            gramos = null;
        }
        BigDecimal cantidad = gramos != null ? gramos
                : racion.getGramos().multiply(raciones).setScale(2, java.math.RoundingMode.HALF_UP);
        linea.setRacion(racion);
        linea.setRaciones(raciones);
        linea.setCantidadGramos(cantidad);
        ValoresLinea.de(linea.getAlimento(), cantidad).ponerEn(linea);
    }

    /**
     * Pone los gramos de una línea, sus calorías y sus macros (lotes 1.6.1 y 1.6.6). No toca
     * la ración, salvo en un plato sin peso hecho: ahí no hay gramos de verdad, y los que
     * lleguen (de una build anterior, que ve «1 ración (100 g)») son raciones de 100 g.
     *
     * @param linea  la línea, con su alimento.
     * @param gramos gramos, mayores que 0.
     */
    @Override
    public void ponerGramos(AlimentoComida linea, BigDecimal gramos) {
        Alimento alimento = linea.getAlimento();
        if (Platos.esPlato(alimento) && !Platos.conPeso(alimento) && gramos != null) {
            AlimentoRacion actual = Platos.racionActual(alimento);
            BigDecimal raciones = gramos.divide(Platos.GRAMOS_SIN_PESO, 2, java.math.RoundingMode.HALF_UP);
            if (actual == null || raciones.signum() <= 0) throw new InvalidDataException("error.anadir.cantidad");
            ponerCantidad(linea, null, actual.getId(), raciones);
            return;
        }
        linea.setCantidadGramos(gramos);
        ValoresLinea.de(alimento, gramos).ponerEn(linea);
    }

    // Recorre todas las líneas alimento-comida de una comida y recalcula
    // sus totales de calorías, proteínas, carbohidratos y grasas, guardando
    // la comida actualizada.
    /**
     * Recalcula los totales de una comida por su id. Sin comprobación de propiedad: es
     * interna y no tiene ruta HTTP (ver el contrato).
     */
    @Override
    @Transactional
    public void recalcularTotales(Integer comidaId) {
        Comida comida = comidaRepository.findById(comidaId)
                .orElseThrow(() -> new NotFoundEntityException("error.comida.noExiste", comidaId));

        recalcularTotalesComida(comida);
    }

    private void recalcularTotalesComida(Comida comida) {
        List<AlimentoComida> items = alimentoComidaRepository.findByComidaId(comida.getId());
        int totalCal = 0;
        BigDecimal totalProt = BigDecimal.ZERO;
        BigDecimal totalCarb = BigDecimal.ZERO;
        BigDecimal totalGras = BigDecimal.ZERO;

        // Lo guardado en cada línea (lote 1.6.6, A1), no los valores de hoy del alimento.
        for (AlimentoComida item : items) {
            if (item.getCaloriasTotales() != null) totalCal += item.getCaloriasTotales();
            if (item.getProteinasTotales() != null) totalProt = totalProt.add(item.getProteinasTotales());
            if (item.getCarbohidratosTotales() != null) totalCarb = totalCarb.add(item.getCarbohidratosTotales());
            if (item.getGrasasTotales() != null) totalGras = totalGras.add(item.getGrasasTotales());
        }

        comida.setTotalCalorias(totalCal);
        comida.setTotalProteinas(totalProt);
        comida.setTotalCarbohidratos(totalCarb);
        comida.setTotalGrasas(totalGras);
        comidaRepository.save(comida);
    }

    /**
     * Un id de alimento que no existe es un 404, y se comprueba AQUÍ (DEC-033).
     * Antes se deducía de que la lista saliera vacía, que no es lo mismo: «no hay
     * datos» y «ese alimento no existe» son dos respuestas distintas, y la app no
     * podía separarlas. La comprobación va DESPUÉS de la de propiedad para que a
     * quien no es dueño se le responda 403 sin decirle si el id existe.
     */
    private void exigirAlimentoExistente(Integer alimentoId) {
        Alimento alimento = alimentoRepository.findById(alimentoId)
                .orElseThrow(() -> new NotFoundEntityException("error.alimento.noExiste", alimentoId));
        // Del catálogo o de quien llama: uno de otro, 403 (DEC-042).
        checkAlimentoAccesible(alimento);
    }
}
