package com.gymprofit.api.service.ejerciciorealizado;

import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoCreateDTO;
import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoDTO;
import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoPatchDTO;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.dto.entity.serierealizada.SerieRealizadaCreateDTO;
import com.gymprofit.api.entity.SerieRealizada;
import com.gymprofit.api.entity.EjercicioRealizado;
import com.gymprofit.api.entity.SesionEntrenamiento;
import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.exceptions.CreateEntityException;
import com.gymprofit.api.exceptions.DeleteEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UpdateEntityException;
import com.gymprofit.api.mappers.EjercicioRealizadoMapper;
import com.gymprofit.api.repository.jpa.IEjercicioRealizadoRepository;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// ============================================================
// EjercicioRealizadoService — implementación del registro de ejercicios realizados
// Gestiona la persistencia de los ejercicios efectivamente ejecutados en
// cada sesión de entrenamiento, validando la propiedad de la sesión
// (checkOwnership) para que cada usuario solo acceda a sus propios datos.
// ============================================================
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class EjercicioRealizadoService implements IEjercicioRealizadoService{

    private final IEjercicioRealizadoRepository ejercicioRealizadoRepository;
    private final IEjercicioRepository ejercicioRepository;
    private final ISesionEntrenamientoRepository sesionEntrenamientoRepository;
    private final EjercicioRealizadoMapper ejercicioRealizadoMapper;
    private final SecurityUtils securityUtils;
    private final Logger logger = LoggerFactory.getLogger(EjercicioRealizadoService.class);


    // Devuelve todos los ejercicios realizados; requiere rol ADMIN.
    @Override
    public List<EjercicioRealizadoDTO> findAll() {
        logger.info("Buscando todos los ejercicios realizados");

        securityUtils.requireAdmin();

        List<EjercicioRealizado> ejerciciosRealizados = ejercicioRealizadoRepository.findAll();

        return ejercicioRealizadoMapper.toDTOList(ejerciciosRealizados);
    }

    // Busca un ejercicio realizado por id, comprobando que pertenece al usuario autenticado.
    @Override
    public EjercicioRealizadoDTO findById(Integer id) {
        logger.info("Buscando ejercicio realizado por id: {}", id);

        EjercicioRealizado ejercicioRealizado = ejercicioRealizadoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio realizado con id " + id + " no existe"));

        securityUtils.checkOwnership(ejercicioRealizado.getSesion().getUsuario().getId());

        return ejercicioRealizadoMapper.toDTO(ejercicioRealizado);
    }

    // Crea un ejercicio realizado, validando que la sesión y el ejercicio existen y pertenecen al usuario.
    @Transactional
    @Override
    public EjercicioRealizadoDTO save(EjercicioRealizadoCreateDTO ejercicioRealizadoCreateDTO) {
        logger.info("Creando nuevo ejercicio realizado para sesión id: {}", ejercicioRealizadoCreateDTO.getSesionId());

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(ejercicioRealizadoCreateDTO.getSesionId())
                .orElseThrow(() -> new NotFoundEntityException("La sesión con id " + ejercicioRealizadoCreateDTO.getSesionId() + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        Ejercicio ejercicio = ejercicioRepository.findById(ejercicioRealizadoCreateDTO.getEjercicioId())
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio con id " + ejercicioRealizadoCreateDTO.getEjercicioId() + " no existe"));

        try {
            EjercicioRealizado ejercicioRealizado = ejercicioRealizadoMapper.toEntity(ejercicioRealizadoCreateDTO);
            ejercicioRealizado.setSesion(sesion);
            ejercicioRealizado.setEjercicio(ejercicio);

            aplicarSeries(ejercicioRealizado, ejercicioRealizadoCreateDTO.getSeries());

            EjercicioRealizado ejercicioGuardado = ejercicioRealizadoRepository.save(ejercicioRealizado);

            return ejercicioRealizadoMapper.toDTO(ejercicioGuardado);
        } catch (Exception e) {
            throw new CreateEntityException(EjercicioRealizado.class.getSimpleName(), ejercicioRealizadoCreateDTO, e);
        }
    }

    /**
     * Cuelga del ejercicio las series que mandó el cliente y deduce de ellas el
     * resumen.
     *
     * <p>El resumen (series completadas y peso usado) NO se acepta del cliente
     * cuando hay series: si llegaran los dos por separado podrían contradecirse y
     * no habría forma de saber cuál es el bueno. Como peso de resumen se toma el
     * más alto de la sesión, que es el que interesa para un récord personal.
     *
     * <p>Si no llegan series se respeta lo que mande el cliente: las sesiones
     * guardadas antes de que existiera esta tabla siguen siendo válidas.
     *
     * @param ejercicio el ejercicio al que colgarlas.
     * @param series    lo que mandó el cliente; puede ser nulo o vacío.
     */
    private void aplicarSeries(EjercicioRealizado ejercicio,
                               List<SerieRealizadaCreateDTO> series) {
        if (series == null || series.isEmpty()) return;

        BigDecimal pesoMaximo = null;
        int completadas = 0;

        ejercicio.getSeries().clear();

        for (SerieRealizadaCreateDTO dto : series) {
            SerieRealizada serie = new SerieRealizada();
            serie.setNumero(dto.getNumero());
            serie.setRepeticiones(dto.getRepeticiones());
            serie.setPeso(dto.getPeso());
            serie.setCompletada(dto.getCompletada() == null || dto.getCompletada());
            serie.setEjercicioRealizado(ejercicio);
            ejercicio.getSeries().add(serie);

            if (Boolean.TRUE.equals(serie.getCompletada())) completadas++;
            if (dto.getPeso() != null
                    && (pesoMaximo == null || dto.getPeso().compareTo(pesoMaximo) > 0)) {
                pesoMaximo = dto.getPeso();
            }
        }

        ejercicio.setSeriesCompletadas(completadas);
        if (pesoMaximo != null) ejercicio.setPesoUsado(pesoMaximo);
    }

    // Actualiza los datos de series, repeticiones, peso, tiempo y notas de un ejercicio realizado.
    @Override
    @Transactional
    public EjercicioRealizadoDTO modify(EjercicioRealizadoDTO ejercicioRealizadoDTO) {
        logger.info("Modificando ejercicio realizado con id: {}", ejercicioRealizadoDTO.getId());

        EjercicioRealizado ejercicioRealizado = ejercicioRealizadoRepository.findById(ejercicioRealizadoDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio realizado con id " + ejercicioRealizadoDTO.getId() + " no existe"));

        securityUtils.checkOwnership(ejercicioRealizado.getSesion().getUsuario().getId());

        try {
            ejercicioRealizado.setSeriesCompletadas(ejercicioRealizadoDTO.getSeriesCompletadas());
            ejercicioRealizado.setRepeticionesReales(ejercicioRealizadoDTO.getRepeticionesReales());
            ejercicioRealizado.setPesoUsado(ejercicioRealizadoDTO.getPesoUsado());
            ejercicioRealizado.setTiempoSegundos(ejercicioRealizadoDTO.getTiempoSegundos());
            ejercicioRealizado.setNotas(ejercicioRealizadoDTO.getNotas());

            EjercicioRealizado actualizado = ejercicioRealizadoRepository.save(ejercicioRealizado);

            return ejercicioRealizadoMapper.toDTO(actualizado);
        } catch (Exception e) {
            throw new UpdateEntityException(EjercicioRealizado.class.getSimpleName(), ejercicioRealizadoDTO, e);
        }
    }

    // Elimina un ejercicio realizado, comprobando la propiedad de la sesión asociada.
    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Eliminando ejercicio realizado con id: {}", id);

        EjercicioRealizado ejercicioRealizado = ejercicioRealizadoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio realizado con id " + id + " no existe"));

        securityUtils.checkOwnership(ejercicioRealizado.getSesion().getUsuario().getId());

        try {
            ejercicioRealizadoRepository.delete(ejercicioRealizado);

            logger.info("Ejercicio realizado con id {} eliminado correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(EjercicioRealizado.class.getSimpleName(), id, e);
        }
    }

    // Lista los ejercicios realizados de una sesión, tras validar la propiedad de la sesión.
    @Override
    public List<EjercicioRealizadoDTO> findBySesionId(Integer sesionId) {
        logger.info("Buscando ejercicios realizados por sesión id: {}", sesionId);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(sesionId)
                .orElseThrow(() -> new NotFoundEntityException("La sesión con id " + sesionId + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        List<EjercicioRealizado> ejerciciosRealizados = ejercicioRealizadoRepository.findBySesionId(sesionId);

        return ejercicioRealizadoMapper.toDTOList(ejerciciosRealizados);
    }

    /**
     * Lista los registros de un ejercicio concreto <b>del usuario autenticado</b>.
     * <p>
     * El id que llega por la ruta es el del <em>ejercicio del catálogo</em>, no el de un
     * recurso de nadie, así que aquí no hay propiedad que comprobar: no existe un «id
     * ajeno» que rechazar con un 403. Lo que había que arreglar es el alcance, porque la
     * consulta devolvía los registros de <b>todos los usuarios</b> y bastaba pedir
     * cualquier ejercicio del catálogo para leer cuánto levanta cada uno y qué días
     * entrena. El usuario sale del token y nunca de la petición.
     * <p>
     * ADMIN conserva la vista global, que es la que tenía declarada {@code findAll()}.
     *
     * @param ejercicioId ejercicio del catálogo cuyo histórico se pide.
     * @return los registros propios, o los de todos si quien pregunta es ADMIN.
     */
    @Override
    public List<EjercicioRealizadoDTO> findByEjercicioId(Integer ejercicioId) {
        logger.info("Buscando ejercicios realizados por ejercicio id: {}", ejercicioId);

        exigirEjercicioExistente(ejercicioId);

        List<EjercicioRealizado> ejerciciosRealizados = securityUtils.isAdmin()
                ? ejercicioRealizadoRepository.findByEjercicioId(ejercicioId)
                : ejercicioRealizadoRepository.findByEjercicioIdAndSesionUsuarioId(
                        ejercicioId, securityUtils.getCurrentUserId());

        return ejercicioRealizadoMapper.toDTOList(ejerciciosRealizados);
    }

    // Lista los registros de un ejercicio dentro de una sesión, comprobando la propiedad de la sesión.
    @Override
    public List<EjercicioRealizadoDTO> findBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId) {
        logger.info("Buscando ejercicios realizados por sesión id: {} y ejercicio id: {}", sesionId, ejercicioId);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(sesionId)
                .orElseThrow(() -> new NotFoundEntityException("La sesión con id " + sesionId + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        List<EjercicioRealizado> ejerciciosRealizados = ejercicioRealizadoRepository.findBySesionIdAndEjercicioId(sesionId, ejercicioId);

        return ejercicioRealizadoMapper.toDTOList(ejerciciosRealizados);
    }

    /**
     * Cuenta los ejercicios realizados en una sesión, comprobando la propiedad de la sesión.
     * <p>
     * Un contador es información sobre el recurso aunque no devuelva ninguna fila suya:
     * sin esta comprobación, iterar ids de sesión ajenas dibujaba el historial de
     * entrenamiento de cualquiera —qué días entrenó y cuántos ejercicios hizo— sin leer
     * un solo registro.
     *
     * @param sesionId sesión cuyos ejercicios se cuentan.
     * @throws NotFoundEntityException si la sesión no existe.
     */
    @Override
    public Long countBySesionId(Integer sesionId) {
        logger.info("Contando ejercicios realizados por sesión id: {}", sesionId);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(sesionId)
                .orElseThrow(() -> new NotFoundEntityException("La sesión con id " + sesionId + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        return ejercicioRealizadoRepository.countBySesionId(sesionId);
    }

    /**
     * Cuenta cuántas veces ha registrado <b>el usuario autenticado</b> un ejercicio concreto.
     * <p>
     * Mismo caso que {@link #findByEjercicioId(Integer)}: el id de la ruta es el de un
     * ejercicio del catálogo y no hay propiedad que rechazar, pero el contador global
     * revelaba la actividad de todos los usuarios de la plataforma. ADMIN mantiene la
     * cuenta global.
     *
     * @param ejercicioId ejercicio del catálogo que se cuenta.
     */
    @Override
    public Long countByEjercicioId(Integer ejercicioId) {
        logger.info("Contando ejercicios realizados por ejercicio id: {}", ejercicioId);

        return securityUtils.isAdmin()
                ? ejercicioRealizadoRepository.countByEjercicioId(ejercicioId)
                : ejercicioRealizadoRepository.countByEjercicioIdAndSesionUsuarioId(
                        ejercicioId, securityUtils.getCurrentUserId());
    }

    // Elimina todos los ejercicios realizados de una sesión, comprobando su propiedad.
    @Transactional
    @Override
    public void deleteBySesionId(Integer sesionId) {
        logger.info("Eliminando ejercicios realizados por sesión id: {}", sesionId);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(sesionId)
                .orElseThrow(() -> new NotFoundEntityException("La sesión con id " + sesionId + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        try {
            ejercicioRealizadoRepository.deleteBySesionId(sesionId);

            logger.info("Ejercicios realizados de la sesión {} eliminados correctamente", sesionId);
        } catch (Exception e) {
            throw new DeleteEntityException(EjercicioRealizado.class.getSimpleName(), sesionId, e);
        }
    }

    // Elimina los registros de un ejercicio concreto dentro de una sesión, comprobando su propiedad.
    @Transactional
    @Override
    public void deleteBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId) {
        logger.info("Eliminando ejercicios realizados por sesión id: {} y ejercicio id: {}", sesionId, ejercicioId);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(sesionId)
                .orElseThrow(() -> new NotFoundEntityException("La sesión con id " + sesionId + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        try {
            ejercicioRealizadoRepository.deleteBySesionIdAndEjercicioId(sesionId, ejercicioId);

            logger.info("Ejercicios realizados de sesión {} y ejercicio {} eliminados correctamente", sesionId, ejercicioId);
        } catch (Exception e) {
            throw new DeleteEntityException(EjercicioRealizado.class.getSimpleName(), sesionId, e);
        }
    }

    /**
     * Comprueba si existe un registro de ese ejercicio en esa sesión, validando la
     * propiedad de la sesión.
     * <p>
     * Un booleano también es información ajena: preguntando ejercicio a ejercicio sobre
     * una sesión de otro se reconstruye su entrenamiento entero, un sí o un no cada vez.
     *
     * @throws NotFoundEntityException si la sesión no existe.
     */
    @Override
    public boolean existsBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId) {
        logger.info("Verificando si existe ejercicio realizado para sesión id: {} y ejercicio id: {}", sesionId, ejercicioId);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(sesionId)
                .orElseThrow(() -> new NotFoundEntityException("La sesión con id " + sesionId + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        return ejercicioRealizadoRepository.existsBySesionIdAndEjercicioId(sesionId, ejercicioId);
    }

    // Actualiza parcialmente un ejercicio realizado (solo los campos no nulos del patch).
    @Transactional
    @Override
    public EjercicioRealizadoDTO patch(Integer id, EjercicioRealizadoPatchDTO patchDTO) {
        logger.info("Aplicando patch a ejercicio realizado con id: {}", id);

        EjercicioRealizado ejercicioRealizado = ejercicioRealizadoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio realizado con id " + id + " no existe"));

        securityUtils.checkOwnership(ejercicioRealizado.getSesion().getUsuario().getId());

        try {
            if (patchDTO.getSeriesCompletadas() != null) ejercicioRealizado.setSeriesCompletadas(patchDTO.getSeriesCompletadas());
            if (patchDTO.getRepeticionesReales() != null) ejercicioRealizado.setRepeticionesReales(patchDTO.getRepeticionesReales());
            if (patchDTO.getPesoUsado() != null) ejercicioRealizado.setPesoUsado(patchDTO.getPesoUsado());
            if (patchDTO.getTiempoSegundos() != null) ejercicioRealizado.setTiempoSegundos(patchDTO.getTiempoSegundos());
            if (patchDTO.getNotas() != null) ejercicioRealizado.setNotas(patchDTO.getNotas());

            return ejercicioRealizadoMapper.toDTO(ejercicioRealizadoRepository.save(ejercicioRealizado));
        } catch (Exception e) {
            throw new UpdateEntityException(EjercicioRealizado.class.getSimpleName(), id, e);
        }
    }

    /**
     * Un id de ejercicio que no existe es un 404, y se comprueba AQUÍ (DEC-033).
     * Antes se deducía de que la lista saliera vacía, que no es lo mismo: «no hay
     * datos» y «ese ejercicio no existe» son dos respuestas distintas, y la app no
     * podía separarlas. La comprobación va DESPUÉS de la de propiedad para que a
     * quien no es dueño se le responda 403 sin decirle si el id existe.
     */
    private void exigirEjercicioExistente(Integer ejercicioId) {
        if (!ejercicioRepository.existsById(ejercicioId)) {
            throw new NotFoundEntityException("El ejercicio con id " + ejercicioId + " no existe");
        }
    }
}
