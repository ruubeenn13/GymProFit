package com.gymprofit.api.service.sesionentrenamiento;

import com.gymprofit.api.dto.entity.serierealizada.SerieRealizadaCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.EjercicioSesionCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionCompletaCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoPatchDTO;
import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.sesionentrenamiento.VolumenMuscularDTO;
import com.gymprofit.api.dto.entity.record.RecordDTO;
import com.gymprofit.api.repository.jpa.IEjercicioRealizadoRepository;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.entity.EjercicioRealizado;
import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.SerieRealizada;
import com.gymprofit.api.entity.SesionEntrenamiento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.CreateEntityException;
import com.gymprofit.api.exceptions.DeleteEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UnauthorizedException;
import com.gymprofit.api.exceptions.UpdateEntityException;
import com.gymprofit.api.mappers.SesionEntrenamientoMapper;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.repository.jpa.IRutinaRepository;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.ejercicio.Musculos;
import com.gymprofit.api.service.logro.ILogroService;
import com.gymprofit.api.service.record.IRecordService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// ============================================================
// SesionEntrenamientoService — implementa la gestión de sesiones de entrenamiento.
// Controla el ciclo de vida de una sesión (crear, modificar, completar, borrar),
// aplica comprobaciones de propiedad por usuario y dispara la evaluación de logros
// cuando una sesión se marca como completada.
// ============================================================
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class SesionEntrenamientoService implements ISesionEntrenamientoService{

    private final ISesionEntrenamientoRepository sesionEntrenamientoRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IRutinaRepository rutinaRepository;
    private final SesionEntrenamientoMapper sesionEntrenamientoMapper;
    private final ILogroService logroService;
    private final SecurityUtils securityUtils;
    // Necesario para el volumen por músculo: el dato vive en los ejercicios realizados.
    private final IEjercicioRealizadoRepository ejercicioRealizadoRepository;
    // El guardado completo valida contra el catálogo cada ejercicio que llega.
    private final IEjercicioRepository ejercicioRepository;

    // Récords batidos al guardar (GP-088).
    private final IRecordService recordService;
    // Logger para trazar las operaciones del servicio.
    private final Logger logger = LoggerFactory.getLogger(SesionEntrenamientoService.class);


    // Lista todas las sesiones de entrenamiento (solo ADMIN).
    @Override
    public List<SesionEntrenamientoDTO> findAll() {
        securityUtils.requireAdmin();

        logger.info("Buscando todas las sesiones de entrenaminento");

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findAll();

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Busca una sesión por id, comprobando que el solicitante sea el dueño (o ADMIN).
    @Override
    public SesionEntrenamientoDTO findById(Integer id) {
        logger.info("Buscando sesión de entrenamiento por id: {}", id);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La sesión de entrenamiento con id " + id + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        return sesionEntrenamientoMapper.toDTO(sesion);
    }

    // Crea una nueva sesión de entrenamiento. Si no es ADMIN, se fuerza el usuario propietario
    // al usuario autenticado (evita crear sesiones a nombre de otro usuario).
    @Override
    @Transactional
    public SesionEntrenamientoDTO save(SesionEntrenamientoCreateDTO sesionEntrenamientoCreateDTO) {
        if (!securityUtils.isAdmin()) {
            sesionEntrenamientoCreateDTO.setUsuarioId(securityUtils.getCurrentUserId());
        }

        logger.info("Creando nueva sesión de entrenamiento para usuario id: {}", sesionEntrenamientoCreateDTO.getUsuarioId());

        try {
            Usuario usuario = usuarioRepository.findById(sesionEntrenamientoCreateDTO.getUsuarioId())
                    .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + sesionEntrenamientoCreateDTO.getUsuarioId() + " no existe"));

            SesionEntrenamiento sesion = sesionEntrenamientoMapper.toEntity(sesionEntrenamientoCreateDTO);
            sesion.setUsuario(usuario);

            if (sesionEntrenamientoCreateDTO.getRutinaId() != null) {
                Rutina rutina = rutinaRepository.findById(sesionEntrenamientoCreateDTO.getRutinaId())
                        .orElseThrow(() -> new NotFoundEntityException("La rutina con id " + sesionEntrenamientoCreateDTO.getRutinaId() + " no existe"));

                checkRutinaUtilizable(rutina);

                sesion.setRutina(rutina);
            }

            // Si no se indica fecha de inicio, se toma el momento actual.
            if (sesion.getFechaInicio() == null) {
                sesion.setFechaInicio(LocalDateTime.now());
            }

            // Si no se indica fecha de fin, se calcula a partir de la duración estimada.
            if (sesion.getFechaFin() == null) {
                int minutos = sesionEntrenamientoCreateDTO.getDuracionMinutos() != null
                        ? sesionEntrenamientoCreateDTO.getDuracionMinutos() : 0;
                sesion.setFechaFin(sesion.getFechaInicio().plusMinutes(minutos));
            }

            if (sesionEntrenamientoCreateDTO.getCompletada() != null) {
                sesion.setCompletada(sesionEntrenamientoCreateDTO.getCompletada());
            }

            SesionEntrenamiento sesionGuardada = sesionEntrenamientoRepository.save(sesion);

            SesionEntrenamientoDTO dto = sesionEntrenamientoMapper.toDTO(sesionGuardada);
            // Si la sesión se crea ya completada, se evalúan posibles logros nuevos del usuario.
            if (Boolean.TRUE.equals(sesionGuardada.getCompletada())) {
                List<String> nuevos = logroService.evaluarLogros(sesionGuardada.getUsuario().getId());
                if (!nuevos.isEmpty()) dto.setNuevosLogros(nuevos);
            }
            return dto;
        } catch (NotFoundEntityException | UnauthorizedException e) {
            // El 403 de la rutina ajena tiene que salir tal cual: envuelto en
            // CreateEntityException se convertiría en un 500 y dejaría de ser un rechazo.
            throw e;
        } catch (Exception e) {
            throw new CreateEntityException(SesionEntrenamiento.class.getSimpleName(), sesionEntrenamientoCreateDTO, e);
        }
    }

    /**
     * Guarda la sesión entera —sesión, ejercicios y series— en UNA transacción y de
     * forma idempotente.
     *
     * <p><strong>Por qué existe.</strong> El camino viejo obligaba a crear primero la
     * sesión para tener un id que poner en cada ejercicio, y a mandar después un POST
     * por ejercicio. Si fallaba uno de los de en medio quedaba una sesión a medias, y
     * la pantalla ya se había cerrado dándola por buena.
     *
     * <p><strong>Idempotencia.</strong> Un guardado atómico obliga a poder reintentar,
     * y un reintento sin protección duplica entrenamientos: con mala red la petición
     * puede llegar y guardarse y perderse solo la respuesta. Por eso el cliente manda
     * una clave por intento y aquí se mira ANTES de crear nada.
     *
     * <p>La comprobación previa no basta cuando dos reintentos llegan a la vez: entre la
     * consulta y la inserción cabe otra petición. Entonces el índice único
     * {@code (usuario_id, idempotencia_clave)} deja entrar a una y a la otra le lanza
     * DataIntegrityViolationException. Esa excepción NO se atrapa aquí: esta transacción
     * ya no sirve para nada y tiene que revertirse entera. La recupera
     * {@link GuardadoSesionCompletaService}, fuera de ella (GP-076).
     *
     * @param dto la sesión completa.
     * @return la sesión recién creada, o la que ya existía para esa clave.
     */
    @Override
    @Transactional
    public SesionEntrenamientoDTO guardarCompleta(SesionCompletaCreateDTO dto) {
        // El dueño sale del token, nunca del cuerpo (DEC-013).
        Integer usuarioId = securityUtils.getCurrentUserId();

        logger.info("Guardado completo de sesión para usuario id: {} con clave {}",
                usuarioId, dto.getClaveIdempotencia());

        // Reintento reconocido: se devuelve lo que ya hay y no se crea nada.
        Optional<SesionEntrenamiento> yaGuardada =
                sesionEntrenamientoRepository.findByUsuarioIdAndIdempotenciaClave(usuarioId, dto.getClaveIdempotencia());
        if (yaGuardada.isPresent()) {
            logger.info("Clave de idempotencia repetida: se devuelve la sesión {}", yaGuardada.get().getId());
            // Con sus récords: un reintento tras perder la respuesta no debe perderlos.
            return conCambiosDeMarca(sesionEntrenamientoMapper.toDTO(yaGuardada.get()), usuarioId);
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + usuarioId + " no existe"));

        SesionEntrenamiento sesion = new SesionEntrenamiento();
        sesion.setUsuario(usuario);
        sesion.setIdempotenciaClave(dto.getClaveIdempotencia());
        sesion.setDuracionMinutos(dto.getDuracionMinutos());
        sesion.setValoracion(dto.getValoracion());
        sesion.setNotas(dto.getNotas());
        sesion.setCompletada(dto.getCompletada() == null || dto.getCompletada());

        if (dto.getRutinaId() != null) {
            Rutina rutina = rutinaRepository.findById(dto.getRutinaId())
                    .orElseThrow(() -> new NotFoundEntityException("La rutina con id " + dto.getRutinaId() + " no existe"));
            checkRutinaUtilizable(rutina);
            sesion.setRutina(rutina);
        }

        sesion.setFechaInicio(dto.getFechaInicio() != null ? dto.getFechaInicio() : LocalDateTime.now());
        int minutos = dto.getDuracionMinutos() != null ? dto.getDuracionMinutos() : 0;
        sesion.setFechaFin(sesion.getFechaInicio().plusMinutes(minutos));

        // Se persiste la sesión antes que los ejercicios porque estos la necesitan como
        // padre; da igual para la atomicidad, que la garantiza la transacción: si un
        // ejercicio revienta, esta fila se va con él. Con id IDENTITY el INSERT sale ya
        // en save(), así que el choque con el índice único salta aquí mismo.
        SesionEntrenamiento guardada = sesionEntrenamientoRepository.save(sesion);

        if (dto.getEjercicios() != null) {
            for (EjercicioSesionCreateDTO ejercicioDTO : dto.getEjercicios()) {
                ejercicioRealizadoRepository.save(construirEjercicio(guardada, ejercicioDTO));
            }
        }

        SesionEntrenamientoDTO resultado = sesionEntrenamientoMapper.toDTO(guardada);
        if (Boolean.TRUE.equals(guardada.getCompletada())) {
            List<String> nuevos = logroService.evaluarLogros(usuarioId);
            if (!nuevos.isEmpty()) resultado.setNuevosLogros(nuevos);
        }
        return conCambiosDeMarca(resultado, usuarioId);
    }

    /**
     * Añade a la respuesta los récords batidos y las primeras marcas de la sesión (GP-088).
     * <p>
     * Va en la misma transacción que el guardado: la consulta de series ve las recién
     * insertadas porque Hibernate vuelca antes de consultar. Una sesión no completada
     * no cuenta para los récords, así que tampoco los trae.
     */
    private SesionEntrenamientoDTO conCambiosDeMarca(SesionEntrenamientoDTO dto, Integer usuarioId) {
        if (!Boolean.TRUE.equals(dto.getCompletada())) return dto;
        List<List<RecordDTO>> cambios = recordService.cambiosDeMarcaDeSesion(usuarioId, dto.getId());
        if (!cambios.get(0).isEmpty()) dto.setRecordsBatidos(cambios.get(0));
        if (!cambios.get(1).isEmpty()) dto.setPrimerasMarcas(cambios.get(1));
        return dto;
    }

    /**
     * Busca la sesión que guardó una clave de idempotencia, en una transacción NUEVA de
     * solo lectura.
     *
     * <p>Tiene que ser una transacción nueva: con REPEATABLE READ, una lectura dentro de
     * la que perdió la carrera usaría la instantánea de su comprobación previa y no vería
     * la sesión ganadora. Desde la fachada, que no tiene transacción, bastaría REQUIRED;
     * REQUIRES_NEW deja escrito que tiene que seguir siéndolo si alguien la llama desde
     * dentro de otra. Ese segundo caso no tiene test.
     *
     * @param usuarioId dueño, sacado del token.
     * @param clave clave de idempotencia del intento.
     * @return la sesión, si ya existe.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<SesionEntrenamientoDTO> buscarPorClaveIdempotencia(Integer usuarioId, String clave) {
        return sesionEntrenamientoRepository.findByUsuarioIdAndIdempotenciaClave(usuarioId, clave)
                .map(sesionEntrenamientoMapper::toDTO);
    }

    /**
     * Arma un ejercicio de la sesión con sus series.
     *
     * <p>El ejercicio se busca en el catálogo y NO se crea: un id que no existe es un
     * 404 que tumba el guardado entero, que es justo lo que GP-006 quiere. El resumen
     * (series completadas y peso usado) se deduce de las series, igual que en el alta
     * suelta, para que no puedan contradecirse.
     */
    private EjercicioRealizado construirEjercicio(SesionEntrenamiento sesion, EjercicioSesionCreateDTO dto) {
        Ejercicio ejercicio = ejercicioRepository.findById(dto.getEjercicioId())
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio con id " + dto.getEjercicioId() + " no existe"));

        EjercicioRealizado realizado = new EjercicioRealizado();
        realizado.setSesion(sesion);
        realizado.setEjercicio(ejercicio);
        realizado.setRepeticionesReales(dto.getRepeticionesReales());
        realizado.setNotas(dto.getNotas());

        BigDecimal pesoMaximo = null;
        int completadas = 0;

        if (dto.getSeries() != null) {
            for (SerieRealizadaCreateDTO serieDTO : dto.getSeries()) {
                SerieRealizada serie = new SerieRealizada();
                serie.setNumero(serieDTO.getNumero());
                serie.setRepeticiones(serieDTO.getRepeticiones());
                serie.setPeso(serieDTO.getPeso());
                serie.setCompletada(serieDTO.getCompletada() == null || serieDTO.getCompletada());
                serie.setEjercicioRealizado(realizado);
                realizado.getSeries().add(serie);

                if (Boolean.TRUE.equals(serie.getCompletada())) completadas++;
                if (serieDTO.getPeso() != null
                        && (pesoMaximo == null || serieDTO.getPeso().compareTo(pesoMaximo) > 0)) {
                    pesoMaximo = serieDTO.getPeso();
                }
            }
        }

        realizado.setSeriesCompletadas(completadas);
        realizado.setPesoUsado(pesoMaximo);

        return realizado;
    }

    // Sustituye los datos de una sesión existente (fechas, calorías, notas, rutina asociada...).
    @Override
    @Transactional
    public SesionEntrenamientoDTO modify(SesionEntrenamientoDTO sesionEntrenamientoDTO) {
        logger.info("Modificando sesión de entrenamiento con id: {}", sesionEntrenamientoDTO.getId());

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(sesionEntrenamientoDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("La sesión de entrenamiento con id " + sesionEntrenamientoDTO.getId() + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        try {
            // El propietario nunca se reasigna desde el body: se mantiene el usuario original de la sesión.

            if (sesionEntrenamientoDTO.getRutinaId() != null) {
                Rutina rutina = rutinaRepository.findById(sesionEntrenamientoDTO.getRutinaId())
                        .orElseThrow(() -> new NotFoundEntityException("La rutina con id " + sesionEntrenamientoDTO.getRutinaId() + " no existe"));

                checkRutinaUtilizable(rutina);

                sesion.setRutina(rutina);
            } else {
                sesion.setRutina(null);
            }

            sesion.setFechaInicio(sesionEntrenamientoDTO.getFechaInicio());
            sesion.setFechaFin(sesionEntrenamientoDTO.getFechaFin());
            sesion.setDuracionMinutos(sesionEntrenamientoDTO.getDuracionMinutos());
            sesion.setValoracion(sesionEntrenamientoDTO.getValoracion());
            sesion.setNotas(sesionEntrenamientoDTO.getNotas());
            sesion.setCompletada(sesionEntrenamientoDTO.getCompletada());

            SesionEntrenamiento sesionActualizada = sesionEntrenamientoRepository.save(sesion);

            return sesionEntrenamientoMapper.toDTO(sesionActualizada);
        } catch (NotFoundEntityException | UnauthorizedException e) {
            throw  e;
        } catch (Exception e) {
            throw new UpdateEntityException(SesionEntrenamiento.class.getSimpleName(), sesionEntrenamientoDTO, e);
        }
    }

    // Elimina definitivamente una sesión de entrenamiento.
    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Eliminando sesión de entrenamiento con id: {}", id);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La sesión de entrenamiento con id " + id + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        try {
            sesionEntrenamientoRepository.delete(sesion);

            logger.info("Sesión de entrenamiento con id {} eliminada correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(SesionEntrenamiento.class.getSimpleName(), id, e);
        }
    }

    // Marca la sesión como completada, fija la fecha de fin al momento actual,
    // guarda las notas opcionales y evalúa si el usuario desbloquea nuevos logros.
    // Ya no recibe calorías: no había con qué estimarlas (DEC-004 / GP-010).
    @Transactional
    @Override
    public SesionEntrenamientoDTO completarSesion(Integer id, String notas) {
        logger.info("Completando sesión de entrenamiento con id: {}", id);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La sesión de entrenamiento con id " + id + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        try {
            sesion.setFechaFin(LocalDateTime.now());
            sesion.setCompletada(true);

            if (notas != null){
                sesion.setNotas(notas);
            }

            SesionEntrenamiento sesionCompletada = sesionEntrenamientoRepository.save(sesion);

            logger.info("Sesión {} completada", sesionCompletada);

            List<String> nuevos = logroService.evaluarLogros(sesionCompletada.getUsuario().getId());
            SesionEntrenamientoDTO dto = sesionEntrenamientoMapper.toDTO(sesionCompletada);
            if (!nuevos.isEmpty()) dto.setNuevosLogros(nuevos);
            return dto;
        } catch (Exception e) {
            throw new UpdateEntityException(SesionEntrenamiento.class.getSimpleName(), id, e);
        }
    }

    // Lista las sesiones de un usuario concreto (requiere ser el propio usuario o ADMIN).
    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioId(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        exigirUsuarioExistente(usuarioId);

        logger.info("Buscando sesiones de entrenamiento por usuario id: {}", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioId(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Lista las sesiones asociadas a una rutina (solo ADMIN).
    @Override
    public List<SesionEntrenamientoDTO> findByRutinaId(Integer rutinaId) {
        securityUtils.requireAdmin();

        exigirRutinaExistente(rutinaId);

        logger.info("Buscando sesiones de entrenamiento por rutina id: {}", rutinaId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByRutinaId(rutinaId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Lista todas las sesiones completadas del sistema (solo ADMIN).
    @Override
    public List<SesionEntrenamientoDTO> findCompletadas() {
        securityUtils.requireAdmin();

        logger.info("Buscando sesiones de entrenamiento completadas");

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByCompletadaTrue();

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Lista todas las sesiones pendientes del sistema (solo ADMIN).
    @Override
    public List<SesionEntrenamientoDTO> findPendientes() {
        securityUtils.requireAdmin();

        logger.info("Buscando sesiones de entrenamiento pendientes");

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByCompletadaFalse();

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Lista las sesiones completadas de un usuario.
    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdAndCompletadas(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        exigirUsuarioExistente(usuarioId);

        logger.info("Buscando sesiones completadas del usuario id: {}", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioIdAndCompletadaTrue(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Lista las sesiones pendientes de un usuario.
    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdAndPendientes(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        exigirUsuarioExistente(usuarioId);

        logger.info("Buscando sesiones pendientes del usuario id: {}", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioIdAndCompletadaFalse(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Lista las sesiones de un usuario dentro de una fecha concreta (día completo 00:00-23:59).
    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdAndFecha(Integer usuarioId, LocalDate fecha) {
        securityUtils.checkOwnership(usuarioId);

        exigirUsuarioExistente(usuarioId);

        logger.info("Buscando sesiones del usuario {} en la fecha {}", usuarioId, fecha);

        // Se construye el rango horario del día completo para la consulta.
        LocalDateTime inicio = LocalDateTime.of(fecha.getYear(), fecha.getMonth(), fecha.getDayOfMonth(), 0, 0, 0);
        LocalDateTime fin = LocalDateTime.of(fecha.getYear(), fecha.getMonth(), fecha.getDayOfMonth(), 23, 59, 59);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioIdAndFechaInicioBetween(usuarioId, inicio, fin);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Lista las sesiones de todos los usuarios en una fecha concreta (solo ADMIN).
    @Override
    public List<SesionEntrenamientoDTO> findByFecha(LocalDate fecha) {
        securityUtils.requireAdmin();

        logger.info("Buscando sesiones en la fecha {}", fecha);

        LocalDateTime inicio = LocalDateTime.of(fecha.getYear(), fecha.getMonth(), fecha.getDayOfMonth(), 0, 0, 0);
        LocalDateTime fin = LocalDateTime.of(fecha.getYear(), fecha.getMonth(), fecha.getDayOfMonth(), 23, 59, 59);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByFechaInicioBetween(inicio, fin);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Lista las sesiones de un usuario asociadas a una rutina concreta.
    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdAndRutinaId(Integer usuarioId, Integer rutinaId) {
        securityUtils.checkOwnership(usuarioId);

        exigirUsuarioExistente(usuarioId);
        exigirRutinaExistente(rutinaId);

        logger.info("Buscando sesiones del usuario {} con rutina {}", usuarioId, rutinaId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioIdAndRutinaId(usuarioId, rutinaId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Cuenta las sesiones totales de un usuario.
    @Override
    public Long countByUsuarioId(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Contando sesiones del usuario id: {}", usuarioId);

        return sesionEntrenamientoRepository.countByUsuarioId(usuarioId);
    }

    // Cuenta las sesiones completadas de un usuario.
    @Override
    public Long countCompletadasByUsuario(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Contando sesiones completadas del usuario id: {}", usuarioId);

        return sesionEntrenamientoRepository.countByUsuarioIdAndCompletadaTrue(usuarioId);
    }

    // Cuenta las sesiones asociadas a una rutina (solo ADMIN).
    @Override
    public Long countByRutinaId(Integer rutinaId) {
        securityUtils.requireAdmin();

        logger.info("Contando sesiones de la rutina id: {}", rutinaId);

        return sesionEntrenamientoRepository.countByRutinaId(rutinaId);
    }

    // Lista las sesiones de un usuario ordenadas por fecha.
    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdOrderByFecha(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        exigirUsuarioExistente(usuarioId);

        logger.info("Buscando sesiones del usuario {} ordenadas por fecha", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.getSesionesByUsuarioOrderByFecha(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Lista las sesiones completadas de un usuario ordenadas por fecha.
    @Override
    public List<SesionEntrenamientoDTO> findCompletadasByUsuario(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        exigirUsuarioExistente(usuarioId);

        logger.info("Buscando sesiones completadas del usuario {} ordenadas por fecha", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.getSesionesCompletadasByUsuario(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Actualiza parcialmente una sesión de entrenamiento con los campos no nulos del patch.
    @Transactional
    @Override
    public SesionEntrenamientoDTO patch(Integer id, SesionEntrenamientoPatchDTO patchDTO) {
        logger.info("Aplicando patch a sesión de entrenamiento con id: {}", id);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La sesión de entrenamiento con id " + id + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        try {
            if (patchDTO.getFechaInicio() != null) sesion.setFechaInicio(patchDTO.getFechaInicio());
            if (patchDTO.getFechaFin() != null) sesion.setFechaFin(patchDTO.getFechaFin());
            if (patchDTO.getDuracionMinutos() != null) sesion.setDuracionMinutos(patchDTO.getDuracionMinutos());
            if (patchDTO.getValoracion() != null) sesion.setValoracion(patchDTO.getValoracion());
            if (patchDTO.getNotas() != null) sesion.setNotas(patchDTO.getNotas());
            if (patchDTO.getCompletada() != null) sesion.setCompletada(patchDTO.getCompletada());

            return sesionEntrenamientoMapper.toDTO(sesionEntrenamientoRepository.save(sesion));
        } catch (Exception e) {
            throw new UpdateEntityException(SesionEntrenamiento.class.getSimpleName(), id, e);
        }
    }

    /**
     * Series por músculo en los últimos días, para la silueta de Home.
     * <p>
     * Normaliza los nombres antes de sumar: el catálogo mezcla "Bíceps", "biceps" y
     * ejercicios importados sin músculo primario, que caen a su grupo grueso. La app
     * recibe claves ya limpias y no tiene que adivinar nada.
     *
     * @param usuarioId dueño de las sesiones (se comprueba la propiedad).
     * @param dias      ventana hacia atrás, en días.
     */
    @Override
    public List<VolumenMuscularDTO> getVolumenMuscular(Integer usuarioId, int dias) {
        securityUtils.checkOwnership(usuarioId);

        LocalDateTime desde = LocalDateTime.now().minusDays(Math.max(1, dias));
        List<Object[]> filas = ejercicioRealizadoRepository.seriesPorMusculoDesde(usuarioId, desde);

        // Dos filas distintas ("Bíceps" y "biceps") pueden caer en el mismo músculo, así
        // que se acumulan en un mapa en vez de mapearse una a una.
        Map<String, Integer> acumulado = new LinkedHashMap<>();

        for (Object[] fila : filas) {
            String musculoPrimario = (String) fila[0];
            Object grupo = fila[1];
            int series = fila[2] == null ? 0 : ((Number) fila[2]).intValue();
            if (series <= 0) continue;

            String clave = Musculos.normalizar(musculoPrimario, grupo);
            if (clave == null) continue;

            acumulado.merge(clave, series, Integer::sum);
        }

        return acumulado.entrySet().stream()
                .map(e -> new VolumenMuscularDTO(e.getKey(), e.getValue()))
                .toList();
    }

    /**
     * Kilos movidos en una sesión.
     * <p>
     * Manda el detalle por serie cuando existe, porque es el dato real: cuatro series
     * de 60, 65, 70 y 70 kg no son cuatro de 70. Si la sesión es anterior al registro
     * por serie se cae al resumen por ejercicio, que es lo único que se guardó
     * entonces y sigue siendo mejor que enseñar un cero.
     *
     * @param sesionId sesión a medir (se comprueba la propiedad).
     */
    @Override
    public java.math.BigDecimal getVolumenLevantado(Integer sesionId) {
        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(sesionId)
                .orElseThrow(() -> new NotFoundEntityException(
                        "La sesi\u00f3n con id " + sesionId + " no existe"));
        securityUtils.checkOwnership(sesion.getUsuario().getId());

        java.math.BigDecimal porSeries = ejercicioRealizadoRepository.volumenDeSeries(sesionId);
        if (porSeries != null && porSeries.signum() > 0) return porSeries;

        java.math.BigDecimal porResumen = ejercicioRealizadoRepository.volumenDeResumen(sesionId);
        return porResumen == null ? java.math.BigDecimal.ZERO : porResumen;
    }

    /**
     * Verifica que la rutina que se asocia a la sesión se le puede ofrecer a quien llama.
     * <p>
     * El {@code rutinaId} llega en el cuerpo, así que lo elige el cliente. Una rutina sin
     * dueño es una plantilla del sistema y la puede usar cualquiera —es el caso normal al
     * empezar a entrenar—; una rutina con dueño es de esa persona, y enlazarla desde la
     * sesión de otro crea una fila cruzada que nada de la aplicación permite. Criterio de
     * DEC-027: manda el dueño del id, no el verbo de la petición.
     *
     * @param rutina rutina ya cargada que se quiere asociar a la sesión.
     * @throws com.gymprofit.api.exceptions.UnauthorizedException (→ 403) si es de otro usuario.
     */
    private void checkRutinaUtilizable(Rutina rutina) {
        securityUtils.checkOwnershipIfOwned(
                rutina.getUsuario() == null ? null : rutina.getUsuario().getId());
    }

    /**
     * Un id de rutina que no existe es un 404, y se comprueba AQUÍ (DEC-033).
     * Antes se deducía de que la lista saliera vacía, que no es lo mismo: «no hay
     * datos» y «esa rutina no existe» son dos respuestas distintas, y la app no
     * podía separarlas. La comprobación va DESPUÉS de la de propiedad para que a
     * quien no es dueño se le responda 403 sin decirle si el id existe.
     */
    private void exigirRutinaExistente(Integer rutinaId) {
        if (!rutinaRepository.existsById(rutinaId)) {
            throw new NotFoundEntityException("La rutina con id " + rutinaId + " no existe");
        }
    }

    /**
     * Un id de usuario que no existe es un 404, y se comprueba AQUÍ (DEC-033).
     * Antes se deducía de que la lista saliera vacía, que no es lo mismo: «no hay
     * datos» y «ese usuario no existe» son dos respuestas distintas, y la app no
     * podía separarlas. La comprobación va DESPUÉS de la de propiedad para que a
     * quien no es dueño se le responda 403 sin decirle si el id existe.
     */
    private void exigirUsuarioExistente(Integer usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new NotFoundEntityException("El usuario con id " + usuarioId + " no existe");
        }
    }
}
