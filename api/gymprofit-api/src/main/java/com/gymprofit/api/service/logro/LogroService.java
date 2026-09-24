package com.gymprofit.api.service.logro;

import com.gymprofit.api.dto.entity.logro.LogroCreateDTO;
import com.gymprofit.api.dto.entity.logro.LogroDTO;
import com.gymprofit.api.dto.entity.logro.LogroProgresoDTO;
import com.gymprofit.api.dto.entity.logro.UsuarioLogroDTO;
import com.gymprofit.api.entity.Logro;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.entity.UsuarioLogro;
import com.gymprofit.api.config.security.SecurityUtils;
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
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ============================================================
// LogroService — implementación del sistema de logros/achievements
// Gestiona el catálogo de logros y evalúa el progreso de cada usuario
// (sesiones completadas, ejercicios realizados, objetivos cumplidos)
// para otorgar automáticamente los logros correspondientes.
// ============================================================
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogroService implements ILogroService {

    private final ILogroRepository logroRepository;
    private final IUsuarioLogroRepository usuarioLogroRepository;
    private final IUsuarioRepository usuarioRepository;
    private final ISesionEntrenamientoRepository sesionRepository;
    private final IEjercicioRealizadoRepository ejercicioRealizadoRepository;
    private final IObjetivoPersonalRepository objetivoPersonalRepository;
    private final LogroMapper logroMapper;
    private final SecurityUtils securityUtils;

    private static final Logger logger = LoggerFactory.getLogger(LogroService.class);

    // Devuelve el catálogo completo de logros disponibles.
    @Override
    public List<LogroDTO> findAll() {
        return logroMapper.toDTOList(logroRepository.findAll());
    }

    /**
     * Devuelve los logros obtenidos por un usuario, comprobando que quien pregunta es él.
     * <p>
     * El catálogo de logros ({@code findAll}) es público y sigue siendo consultable por un
     * GUEST: son los mismos para todo el mundo. Los logros <em>obtenidos</em> no: dicen
     * cuánto entrena una persona y desde cuándo, y sin esta comprobación se leían iterando
     * ids de usuario con un token de invitado, que se consigue sin credenciales.
     * <p>
     * La comprobación va <b>antes</b> de mirar si el usuario existe, y no después: al revés,
     * la diferencia entre 404 y 403 convertiría la ruta en un detector de qué ids existen.
     *
     * @param usuarioId usuario cuyos logros se piden; tiene que ser el del token, salvo ADMIN.
     * @throws NotFoundEntityException si el usuario no existe.
     */
    @Override
    public List<UsuarioLogroDTO> findByUsuarioId(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

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
        List<Logro> todos = logroRepository.findAll();

        Map<TipoLogro.Metrica, Long> valores = contarMetricas(usuarioId);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundEntityException("Usuario con id " + usuarioId + " no encontrado"));

        List<String> nuevos = new ArrayList<>();

        // Idioma del request (Accept-Language → LocaleContextHolder): si es inglés,
        // los nombres de los logros nuevos se devuelven en EN cuando hay traducción.
        boolean idiomaEn = "en".equals(LocaleContextHolder.getLocale().getLanguage());

        for (Logro logro : todos) {
            if (logroIds.contains(logro.getId())) continue;

            // Métrica y umbral salen del tipo: el mismo sitio que lee el progreso.
            TipoLogro tipo = logro.getTipo();
            if (tipo.alcanzado(valores.get(tipo.getMetrica()))) {
                UsuarioLogro usuarioLogro = new UsuarioLogro();
                usuarioLogro.setUsuario(usuario);
                usuarioLogro.setLogro(logro);
                usuarioLogro.setFechaObtenido(LocalDateTime.now());
                usuarioLogroRepository.save(usuarioLogro);
                // Nombre localizado: EN si el request es inglés y existe traducción; ES en caso contrario.
                String nombreLocalizado = (idiomaEn && logro.getNombreEn() != null && !logro.getNombreEn().isBlank())
                        ? logro.getNombreEn()
                        : logro.getNombre();
                nuevos.add(nombreLocalizado);
                logger.info("Logro '{}' otorgado al usuario {}", logro.getNombre(), usuarioId);
            }
        }

        return nuevos;
    }

    /**
     * Catálogo con el estado del usuario del token: conseguido y cuándo, o cuánto lleva.
     * <p>
     * No lleva id en la ruta a propósito: el usuario sale del token (DEC-013), así que
     * no hay id ajeno que pedir y la clase de IDOR no existe en vez de estar protegida.
     * <p>
     * El progreso se cuenta con {@link #contarMetricas} y se compara con
     * {@link TipoLogro#alcanzado}, lo mismo que usa {@link #evaluarLogros}: en el borde
     * (6 de 7, 7 de 7) no pueden decir cosas distintas. Se recorta al umbral para que
     * la app no pinte «9 de 7».
     */
    @Override
    public List<LogroProgresoDTO> progresoDelUsuarioActual() {
        Integer usuarioId = securityUtils.getCurrentUserId();
        Map<TipoLogro.Metrica, Long> valores = contarMetricas(usuarioId);

        Map<Integer, UsuarioLogro> obtenidos = new HashMap<>();
        for (UsuarioLogro ul : usuarioLogroRepository.findByUsuarioId(usuarioId)) {
            obtenidos.put(ul.getLogro().getId(), ul);
        }

        boolean idiomaEn = "en".equals(LocaleContextHolder.getLocale().getLanguage());
        List<LogroProgresoDTO> resultado = new ArrayList<>();
        for (Logro logro : logroRepository.findAll()) {
            TipoLogro tipo = logro.getTipo();
            long valor = valores.get(tipo.getMetrica());
            UsuarioLogro obtenido = obtenidos.get(logro.getId());

            resultado.add(new LogroProgresoDTO(
                    logro.getId(),
                    localizado(idiomaEn, logro.getNombreEn(), logro.getNombre()),
                    localizado(idiomaEn, logro.getDescripcionEn(), logro.getDescripcion()),
                    tipo,
                    tipo.getMetrica(),
                    tipo.getUmbral(),
                    (int) Math.min(valor, tipo.getUmbral()),
                    obtenido != null,
                    obtenido != null ? obtenido.getFechaObtenido() : null));
        }
        return resultado;
    }

    // Un recuento por métrica. Es el único sitio que sabe cómo se cuenta cada una.
    private Map<TipoLogro.Metrica, Long> contarMetricas(Integer usuarioId) {
        Map<TipoLogro.Metrica, Long> valores = new EnumMap<>(TipoLogro.Metrica.class);
        valores.put(TipoLogro.Metrica.SESIONES_COMPLETADAS,
                sesionRepository.countByUsuarioIdAndCompletadaTrue(usuarioId));
        valores.put(TipoLogro.Metrica.EJERCICIOS_REALIZADOS,
                ejercicioRealizadoRepository.countBySesionUsuarioId(usuarioId));
        valores.put(TipoLogro.Metrica.OBJETIVOS_COMPLETADOS,
                objetivoPersonalRepository.countByUsuarioIdAndCompletadoTrue(usuarioId));
        return valores;
    }

    // Texto en inglés si el request es inglés y hay traducción; si no, el español.
    private static String localizado(boolean idiomaEn, String en, String es) {
        return idiomaEn && en != null && !en.isBlank() ? en : es;
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
