package com.gymprofit.api.service.sesionentrenamiento;

import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoPatchDTO;
import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.sesionentrenamiento.VolumenMuscularDTO;
import com.gymprofit.api.repository.jpa.IEjercicioRealizadoRepository;
import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.SesionEntrenamiento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.CreateEntityException;
import com.gymprofit.api.exceptions.DeleteEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UpdateEntityException;
import com.gymprofit.api.mappers.SesionEntrenamientoMapper;
import com.gymprofit.api.repository.jpa.IRutinaRepository;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.logro.ILogroService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        } catch (NotFoundEntityException e) {
            throw e;
        } catch (Exception e) {
            throw new CreateEntityException(SesionEntrenamiento.class.getSimpleName(), sesionEntrenamientoCreateDTO, e);
        }
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

                sesion.setRutina(rutina);
            } else {
                sesion.setRutina(null);
            }

            sesion.setFechaInicio(sesionEntrenamientoDTO.getFechaInicio());
            sesion.setFechaFin(sesionEntrenamientoDTO.getFechaFin());
            sesion.setDuracionMinutos(sesionEntrenamientoDTO.getDuracionMinutos());
            sesion.setCaloriasQuemadas(sesionEntrenamientoDTO.getCaloriasQuemadas());
            sesion.setNotas(sesionEntrenamientoDTO.getNotas());
            sesion.setCompletada(sesionEntrenamientoDTO.getCompletada());

            SesionEntrenamiento sesionActualizada = sesionEntrenamientoRepository.save(sesion);

            return sesionEntrenamientoMapper.toDTO(sesionActualizada);
        } catch (NotFoundEntityException e) {
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
    // guarda calorías/notas opcionales y evalúa si el usuario desbloquea nuevos logros.
    @Transactional
    @Override
    public SesionEntrenamientoDTO completarSesion(Integer id, Integer caloriasQuemadas, String notas) {
        logger.info("Completando sesión de entrenamiento con id: {}", id);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La sesión de entrenamiento con id " + id + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        try {
            sesion.setFechaFin(LocalDateTime.now());
            sesion.setCompletada(true);

            if (caloriasQuemadas != null) {
                sesion.setCaloriasQuemadas(caloriasQuemadas);
            }

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

        logger.info("Buscando sesiones de entrenamiento por usuario id: {}", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioId(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Lista las sesiones asociadas a una rutina (solo ADMIN).
    @Override
    public List<SesionEntrenamientoDTO> findByRutinaId(Integer rutinaId) {
        securityUtils.requireAdmin();

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

        logger.info("Buscando sesiones completadas del usuario id: {}", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioIdAndCompletadaTrue(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Lista las sesiones pendientes de un usuario.
    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdAndPendientes(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Buscando sesiones pendientes del usuario id: {}", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioIdAndCompletadaFalse(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Lista las sesiones de un usuario dentro de una fecha concreta (día completo 00:00-23:59).
    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdAndFecha(Integer usuarioId, LocalDate fecha) {
        securityUtils.checkOwnership(usuarioId);

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

        logger.info("Buscando sesiones del usuario {} ordenadas por fecha", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.getSesionesByUsuarioOrderByFecha(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    // Lista las sesiones completadas de un usuario ordenadas por fecha.
    @Override
    public List<SesionEntrenamientoDTO> findCompletadasByUsuario(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

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
            if (patchDTO.getCaloriasQuemadas() != null) sesion.setCaloriasQuemadas(patchDTO.getCaloriasQuemadas());
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

            String clave = normalizarMusculo(musculoPrimario, grupo);
            if (clave == null) continue;

            acumulado.merge(clave, series, Integer::sum);
        }

        return acumulado.entrySet().stream()
                .map(e -> new VolumenMuscularDTO(e.getKey(), e.getValue()))
                .toList();
    }

    /**
     * Reduce el músculo de un ejercicio a una de las claves que la silueta sabe pintar.
     * <p>
     * Manda el músculo primario. Cuando falta —pasa en buena parte del catálogo
     * importado de wger— se cae al grupo grueso y se elige el músculo más
     * representativo de ese grupo, que es preferible a no pintar nada.
     *
     * @return clave normalizada, o {@code null} si no hay nada que pintar (CARDIO).
     */
    private String normalizarMusculo(String musculoPrimario, Object grupo) {
        if (musculoPrimario != null && !musculoPrimario.isBlank()) {
            String limpio = sinTildes(musculoPrimario.trim().toLowerCase());
            switch (limpio) {
                case "abdominales":     return "abdominales";
                case "aductores":       return "aductores";
                case "abductores":      return "gluteos";
                case "biceps":          return "biceps";
                case "gemelos":         return "gemelos";
                case "pecho":           return "pecho";
                case "antebrazos":      return "antebrazos";
                case "gluteos":         return "gluteos";
                case "isquiotibiales":  return "isquiotibiales";
                case "dorsales":
                case "espalda media":   return "dorsales";
                case "lumbares":        return "lumbares";
                case "cuello":          return "cuello";
                case "cuadriceps":      return "cuadriceps";
                case "hombros":         return "hombros";
                case "trapecios":       return "trapecios";
                case "triceps":         return "triceps";
                default:                break;   // cae al grupo grueso
            }
        }

        if (grupo == null) return null;

        switch (grupo.toString().toUpperCase()) {
            case "PECHO":    return "pecho";
            case "ESPALDA":  return "dorsales";
            case "PIERNAS":  return "cuadriceps";
            case "HOMBROS":  return "hombros";
            case "BRAZOS":   return "biceps";
            case "ABDOMEN":  return "abdominales";
            case "FULLBODY": return "pecho";
            // CARDIO no tiene músculo que encender: no se inventa uno.
            default:         return null;
        }
    }

    // Quita las tildes para que "bíceps" y "biceps" sean el mismo músculo.
    private String sinTildes(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
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
}
