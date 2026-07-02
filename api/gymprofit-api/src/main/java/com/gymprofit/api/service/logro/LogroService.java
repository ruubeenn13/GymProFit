package com.gymprofit.api.service.logro;

import com.gymprofit.api.dto.entity.logro.LogroCreateDTO;
import com.gymprofit.api.dto.entity.logro.LogroDTO;
import com.gymprofit.api.dto.entity.logro.UsuarioLogroDTO;
import com.gymprofit.api.entity.Logro;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.entity.UsuarioLogro;
import com.gymprofit.api.enums.TipoLogro;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.mappers.LogroMapper;
import com.gymprofit.api.repository.jpa.IEjercicioRealizadoRepository;
import com.gymprofit.api.repository.jpa.ILogroRepository;
import com.gymprofit.api.repository.jpa.IObjetivoPersonalRepository;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioLogroRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// ============================================================
// LogroService — implementación del sistema de logros/achievements
// Gestiona el catálogo de logros y evalúa el progreso de cada usuario
// (sesiones completadas, ejercicios realizados, objetivos cumplidos)
// para otorgar automáticamente los logros correspondientes.
// ============================================================
@Service
@RequiredArgsConstructor
public class LogroService implements ILogroService {

    private final ILogroRepository logroRepository;
    private final IUsuarioLogroRepository usuarioLogroRepository;
    private final IUsuarioRepository usuarioRepository;
    private final ISesionEntrenamientoRepository sesionRepository;
    private final IEjercicioRealizadoRepository ejercicioRealizadoRepository;
    private final IObjetivoPersonalRepository objetivoPersonalRepository;
    private final LogroMapper logroMapper;

    private static final Logger logger = LoggerFactory.getLogger(LogroService.class);

    // Devuelve el catálogo completo de logros disponibles.
    @Override
    public List<LogroDTO> findAll() {
        return logroMapper.toDTOList((List<Logro>) logroRepository.findAll());
    }

    // Devuelve los logros obtenidos por un usuario, validando que exista.
    @Override
    public List<UsuarioLogroDTO> findByUsuarioId(Integer usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new NotFoundEntityException("Usuario con id " + usuarioId + " no encontrado");
        }
        return logroMapper.toUsuarioLogroDTOList(usuarioLogroRepository.findByUsuarioId(usuarioId));
    }

    // Crea un nuevo logro en el catálogo, validando el tipo enviado.
    @Override
    @Transactional
    public LogroDTO save(LogroCreateDTO createDTO) {
        TipoLogro tipo = parseTipo(createDTO.getTipo());

        Logro logro = new Logro();
        logro.setNombre(createDTO.getNombre());
        logro.setDescripcion(createDTO.getDescripcion());
        logro.setTipo(tipo);

        return logroMapper.toDTO(logroRepository.save(logro));
    }

    // Actualiza los campos no nulos de un logro existente del catálogo.
    @Override
    @Transactional
    public LogroDTO update(Integer id, LogroCreateDTO updateDTO) {
        Logro logro = logroRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Logro con id " + id + " no encontrado"));

        if (updateDTO.getNombre() != null) logro.setNombre(updateDTO.getNombre());
        if (updateDTO.getDescripcion() != null) logro.setDescripcion(updateDTO.getDescripcion());
        if (updateDTO.getTipo() != null) logro.setTipo(parseTipo(updateDTO.getTipo()));

        return logroMapper.toDTO(logroRepository.save(logro));
    }

    // Evalúa el progreso del usuario (sesiones, ejercicios, objetivos) y otorga los logros
    // pendientes que cumpla, devolviendo los nombres de los logros nuevos concedidos.
    @Override
    @Transactional
    public List<String> evaluarLogros(Integer usuarioId) {
        // Logros ya obtenidos, para no volver a evaluarlos.
        Set<Integer> logroIds = new HashSet<>(usuarioLogroRepository.findLogroIdsByUsuarioId(usuarioId));
        List<Logro> todos = (List<Logro>) logroRepository.findAll();

        long sesionesCompletadas  = sesionRepository.countByUsuarioIdAndCompletadaTrue(usuarioId);
        long ejerciciosRealizados = ejercicioRealizadoRepository.countBySesionUsuarioId(usuarioId);
        long objetivosCompletados = objetivoPersonalRepository.countByUsuarioIdAndCompletadoTrue(usuarioId);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundEntityException("Usuario con id " + usuarioId + " no encontrado"));

        List<String> nuevos = new ArrayList<>();

        for (Logro logro : todos) {
            if (logroIds.contains(logro.getId())) continue;

            // Condición de desbloqueo específica según el tipo de logro.
            boolean cumple = switch (logro.getTipo()) {
                case PRIMERA_SESION    -> sesionesCompletadas >= 1;
                case CONSTANCIA        -> sesionesCompletadas >= 7;
                case DEDICADO          -> sesionesCompletadas >= 30;
                case CENTENARIO        -> ejerciciosRealizados >= 100;
                case OBJETIVO_CUMPLIDO -> objetivosCompletados >= 1;
                case MAQUINA           -> objetivosCompletados >= 10;
            };

            if (cumple) {
                UsuarioLogro usuarioLogro = new UsuarioLogro();
                usuarioLogro.setUsuario(usuario);
                usuarioLogro.setLogro(logro);
                usuarioLogro.setFechaObtenido(LocalDateTime.now());
                usuarioLogroRepository.save(usuarioLogro);
                nuevos.add(logro.getNombre());
                logger.info("Logro '{}' otorgado al usuario {}", logro.getNombre(), usuarioId);
            }
        }

        return nuevos;
    }

    // Convierte el string recibido en el enum TipoLogro, lanzando excepción si no es válido.
    private TipoLogro parseTipo(String tipo) {
        try {
            return TipoLogro.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Tipo de logro inválido: " + tipo);
        }
    }
}
