package com.gymprofit.api.service.programa;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.programa.DiaCicloDTO;
import com.gymprofit.api.dto.entity.programa.DiaProgramaDTO;
import com.gymprofit.api.dto.entity.programa.ProgramaQueSigueDTO;
import com.gymprofit.api.dto.entity.programa.RecomendadoDTO;
import com.gymprofit.api.dto.entity.programa.RutinaVistaPreviaDTO;
import com.gymprofit.api.dto.entity.programa.VistaPreviaDTO;
import com.gymprofit.api.dto.entity.programa.ProgramaDTO;
import com.gymprofit.api.dto.entity.programa.ProgramaDetalleDTO;
import com.gymprofit.api.dto.entity.programa.ProgramaSeguidoDTO;
import com.gymprofit.api.dto.entity.programa.RutinaConEjerciciosDTO;
import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioDTO;
import com.gymprofit.api.entity.Programa;
import com.gymprofit.api.entity.ProgramaRutina;
import com.gymprofit.api.entity.ProgramaUsuario;
import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.RutinaEjercicio;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.Equipamiento;
import com.gymprofit.api.enums.EquipamientoPrograma;
import com.gymprofit.api.enums.MedidaSerie;
import com.gymprofit.api.enums.Nivel;
import com.gymprofit.api.enums.NivelExperiencia;
import com.gymprofit.api.enums.TipoObjetivo;
import com.gymprofit.api.enums.TipoEjercicioRutina;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.mappers.RutinaEjercicioMapper;
import com.gymprofit.api.mappers.RutinaMapper;
import com.gymprofit.api.repository.jpa.IProgramaRepository;
import com.gymprofit.api.repository.jpa.IProgramaUsuarioRepository;
import com.gymprofit.api.repository.jpa.IRutinaEjercicioRepository;
import com.gymprofit.api.repository.jpa.IRutinaRepository;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// ============================================================
// ProgramaService — catálogo de programas y seguir uno (GP-074)
//
// El catálogo es público para cualquier cuenta, como el de ejercicios (DEC-027): el
// código del programa no tiene dueño. Seguir uno escribe solo para el usuario del
// token (DEC-013): no hay id de usuario en la petición.
//
// Se sigue un programa a la vez (lote 1.2.1): seguir otro, o el mismo con otro tiempo,
// deja el anterior. La rutina que toca la calcula CicloPrograma a partir de las sesiones.
//
// Textos en el idioma de la petición. En el catálogo se elige entre las dos columnas; en
// las copias se escribe el del idioma de la petición y ya no cambia, porque son del
// usuario, que las puede editar.
// ============================================================
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class ProgramaService implements IProgramaService {

    private final IProgramaRepository programaRepository;
    private final IProgramaUsuarioRepository programaUsuarioRepository;
    private final IRutinaRepository rutinaRepository;
    private final IRutinaEjercicioRepository rutinaEjercicioRepository;
    private final IUsuarioRepository usuarioRepository;
    private final RutinaMapper rutinaMapper;
    private final RutinaEjercicioMapper rutinaEjercicioMapper;
    private final SecurityUtils securityUtils;
    private final ISesionEntrenamientoRepository sesionRepository;
    private final MessageSource messageSource;

    /** Ajustes del perfil que puede enseñar la vista previa. */
    static final String AJUSTE_AVANZADO = "AVANZADO";
    static final String AJUSTE_FUERZA = "FUERZA";
    private final Logger logger = LoggerFactory.getLogger(ProgramaService.class);

    @Override
    public List<ProgramaDTO> listar(String equipamiento, Integer dias, String nivel) {
        EquipamientoPrograma porEquipamiento = equipamiento == null ? null : equipamiento(equipamiento);
        Nivel porNivel = nivel == null ? null : nivelDePrograma(nivel);
        if (dias != null && (dias < 1 || dias > 7)) {
            throw new InvalidDataException("error.programa.diasNoValidos");
        }

        List<ProgramaDTO> lista = new ArrayList<>();
        for (Programa p : programaRepository.findByActivoTrueOrderByIdAsc()) {
            if (porEquipamiento != null && p.getEquipamiento() != porEquipamiento) continue;
            if (porNivel != null && p.getNivel() != porNivel) continue;
            if (dias != null && (dias < p.getDiasMin() || dias > p.getDiasMax())) continue;
            ProgramaDTO dto = new ProgramaDTO();
            rellenar(dto, p);
            lista.add(dto);
        }
        return lista;
    }

    @Override
    public ProgramaDetalleDTO detalle(String codigo) {
        Programa programa = programaOr404(codigo);

        ProgramaDetalleDTO dto = new ProgramaDetalleDTO();
        rellenar(dto, programa);
        List<RutinaConEjerciciosDTO> rutinas = new ArrayList<>();
        for (Rutina plantilla : rutinasDistintas(programa)) {
            RutinaConEjerciciosDTO r = rutinaMapper.toConEjercicios(plantilla);
            r.setEjercicios(ejerciciosDTO(rutinaEjercicioRepository.findByRutinaIdOrderByOrdenAsc(plantilla.getId())));
            r.setNumEjercicios(r.getEjercicios().size());
            rutinas.add(r);
        }
        dto.setRutinas(rutinas);
        return dto;
    }

    @Override
    @Transactional
    public ProgramaSeguidoDTO seguir(String codigo, Integer minutos) {
        int tiempo = minutosValidos(minutos);
        Programa programa = programaOr404(codigo);
        Usuario usuario = usuarioActual();
        logger.info("Usuario id={} sigue el programa {} con {} min", usuario.getId(), codigo, tiempo);

        // Se sigue uno a la vez: el que hubiera se deja, como con DELETE /programas/seguido.
        // Si es el mismo programa (cambiar el tiempo), el nuevo empieza donde tocaba.
        int posicionInicial = 1;
        ProgramaUsuario anterior = programaUsuarioRepository
                .findFirstByUsuarioIdAndFechaFinIsNullOrderByIdDesc(usuario.getId()).orElse(null);
        if (anterior != null) {
            if (anterior.getPrograma().getId().equals(programa.getId())) {
                Integer hoy = estadoDelCiclo(anterior).posicionHoy();
                if (hoy != null) posicionInicial = hoy;
            }
            dejar(anterior);
        }

        LocalDateTime ahora = LocalDateTime.now();
        ProgramaUsuario sigue = new ProgramaUsuario();
        sigue.setUsuario(usuario);
        sigue.setPrograma(programa);
        sigue.setMinutos(tiempo);
        sigue.setFechaInicio(ahora);
        sigue.setPosicionInicial(posicionInicial);
        sigue = programaUsuarioRepository.save(sigue);

        boolean en = enIngles();
        List<RutinaConEjerciciosDTO> copias = new ArrayList<>();
        for (Rutina plantilla : rutinasDistintas(programa)) {
            copias.add(copiar(planificar(plantilla, programa, usuario, tiempo, en), sigue, usuario, ahora, en));
        }
        return new ProgramaSeguidoDTO(sigue.getId(), programa.getCodigo(), tiempo, ahora, posicionInicial, copias);
    }

    @Override
    public VistaPreviaDTO vistaPrevia(String codigo, Integer minutos) {
        int tiempo = minutosValidos(minutos);
        Programa programa = programaOr404(codigo);
        Usuario usuario = usuarioActual();
        boolean en = enIngles();

        List<RutinaVistaPreviaDTO> rutinas = new ArrayList<>();
        boolean avanzado = false;
        for (Rutina plantilla : rutinasDistintas(programa)) {
            Plan plan = planificar(plantilla, programa, usuario, tiempo, en);
            avanzado |= plan.ajuste().avanzado();

            Set<Integer> quedan = new HashSet<>();
            plan.ajuste().ejercicios().forEach(e -> quedan.add(e.indice()));
            List<String> quitados = new ArrayList<>();
            for (int i = 0; i < plan.origen().size(); i++) {
                if (!quedan.contains(i)) quitados.add(nombreEjercicio(en, plan.origen().get(i)));
            }
            Integer antes = seriesDelPrimerBasico(plan.origen());
            Integer despues = seriesDelPrimerBasico(plan.filas());
            Integer seriesBasicos = despues != null && !despues.equals(antes) ? despues : null;

            rutinas.add(new RutinaVistaPreviaDTO(plantilla.getCodigo(),
                    texto(en, plantilla.getNombre(), plantilla.getNombreEn()), plan.ajuste().duracion(),
                    plantilla.getDuracionMinutos(), ejerciciosDTO(plan.filas()), quitados, seriesBasicos));
        }

        List<String> ajustes = new ArrayList<>();
        if (avanzado) ajustes.add(AJUSTE_AVANZADO);
        if (usuario.getObjetivo() == TipoObjetivo.MEJORAR_FUERZA) ajustes.add(AJUSTE_FUERZA);
        return new VistaPreviaDTO(programa.getCodigo(), tiempo, ajustes, rutinas);
    }

    @Override
    public RecomendadoDTO recomendado(String equipamiento, Integer dias) {
        if (equipamiento == null || equipamiento.isBlank()) {
            throw new InvalidDataException("error.programa.equipamientoNoValido", String.valueOf(equipamiento));
        }
        EquipamientoPrograma equipo = equipamiento(equipamiento);
        if (dias == null || dias < 2 || dias > 6) {
            throw new InvalidDataException("error.programa.diasRecomendado");
        }
        NivelExperiencia perfil = usuarioActual().getNivelExperiencia();
        RecomendacionPrograma.Recomendacion r = RecomendacionPrograma.elegir(equipo, dias, perfil);
        Programa programa = programaOr404(r.codigo());

        ProgramaDTO dto = new ProgramaDTO();
        rellenar(dto, programa);
        String motivo = r.motivo() == null ? null
                : messageSource.getMessage(r.motivo(), null, LocaleContextHolder.getLocale());
        return new RecomendadoDTO(dto, perfil == null ? NivelExperiencia.PRINCIPIANTE.name() : perfil.name(),
                perfil != null, motivo);
    }

    @Override
    public Optional<ProgramaQueSigueDTO> seguido() {
        Integer usuarioId = securityUtils.getCurrentUserId();
        Optional<ProgramaUsuario> abierto = programaUsuarioRepository
                .findFirstByUsuarioIdAndFechaFinIsNullOrderByIdDesc(usuarioId);
        if (abierto.isEmpty()) return Optional.empty();

        ProgramaUsuario sigue = abierto.get();
        Programa programa = sigue.getPrograma();
        boolean en = enIngles();
        Map<String, Rutina> copias = copiasPorPlantilla(sigue);
        CicloPrograma.Estado estado = estadoDelCiclo(sigue, copias);

        List<DiaCicloDTO> ciclo = new ArrayList<>();
        for (ProgramaRutina dia : programa.getSemana()) {
            Rutina plantilla = dia.getRutina();
            Rutina copia = copias.get(plantilla.getCodigo());
            boolean activa = copia != null && Boolean.TRUE.equals(copia.getActiva());
            ciclo.add(new DiaCicloDTO(dia.getPosicion(), plantilla.getCodigo(),
                    copia == null ? null : copia.getId(),
                    copia != null ? copia.getNombre() : texto(en, plantilla.getNombre(), plantilla.getNombreEn()),
                    copia != null ? copia.getDuracionMinutos() : null, activa,
                    estado.hechas().contains(dia.getPosicion())));
        }

        // Sus rutinas activas en el orden en que tocan desde hoy, cada una una vez.
        List<RutinaConEjerciciosDTO> rutinas = new ArrayList<>();
        if (estado.posicionHoy() != null) {
            List<ProgramaRutina> semana = programa.getSemana();
            Set<String> vistas = new HashSet<>();
            for (int k = 0; k < semana.size(); k++) {
                String plantilla = semana.get((estado.posicionHoy() - 1 + k) % semana.size()).getRutina().getCodigo();
                Rutina copia = copias.get(plantilla);
                if (copia == null || !Boolean.TRUE.equals(copia.getActiva()) || !vistas.add(plantilla)) continue;
                RutinaConEjerciciosDTO r = rutinaMapper.toConEjercicios(copia);
                r.setEjercicios(ejerciciosDTO(rutinaEjercicioRepository.findByRutinaIdOrderByOrdenAsc(copia.getId())));
                r.setNumEjercicios(r.getEjercicios().size());
                rutinas.add(r);
            }
        }

        ProgramaDTO programaDTO = new ProgramaDTO();
        rellenar(programaDTO, programa);
        return Optional.of(new ProgramaQueSigueDTO(sigue.getId(), programaDTO, sigue.getMinutos(),
                sigue.getFechaInicio(), sigue.getPosicionInicial(), estado.posicionHoy(), ciclo, rutinas));
    }

    @Override
    @Transactional
    public void dejarSeguido() {
        programaUsuarioRepository.findFirstByUsuarioIdAndFechaFinIsNullOrderByIdDesc(securityUtils.getCurrentUserId())
                .ifPresent(this::dejar);
    }

    // Lo deja: fecha de fin y sus rutinas fuera de Entrenar (desactivadas). Las sesiones y
    // los récords no se tocan: cuelgan de la rutina, que sigue existiendo.
    private void dejar(ProgramaUsuario sigue) {
        logger.info("Usuario id={} deja el programa {} (seguido id={})", sigue.getUsuario().getId(),
                sigue.getPrograma().getCodigo(), sigue.getId());
        sigue.setFechaFin(LocalDateTime.now());
        programaUsuarioRepository.save(sigue);
        for (Rutina copia : rutinaRepository.findByProgramaUsuarioId(sigue.getId())) {
            if (Boolean.TRUE.equals(copia.getActiva())) {
                copia.setActiva(false);
                rutinaRepository.save(copia);
            }
        }
    }

    private CicloPrograma.Estado estadoDelCiclo(ProgramaUsuario sigue) {
        return estadoDelCiclo(sigue, copiasPorPlantilla(sigue));
    }

    private CicloPrograma.Estado estadoDelCiclo(ProgramaUsuario sigue, Map<String, Rutina> copias) {
        List<String> semana = sigue.getPrograma().getSemana().stream().map(d -> d.getRutina().getCodigo()).toList();
        Set<String> activas = new HashSet<>();
        for (Map.Entry<String, Rutina> c : copias.entrySet()) {
            if (Boolean.TRUE.equals(c.getValue().getActiva())) activas.add(c.getKey());
        }
        return CicloPrograma.calcular(semana, activas, sigue.getPosicionInicial(),
                sesionRepository.plantillasDeSesionesDelPrograma(sigue.getId()));
    }

    // Las copias de un «programa que sigue», por código de su plantilla.
    private Map<String, Rutina> copiasPorPlantilla(ProgramaUsuario sigue) {
        Map<String, Rutina> copias = new HashMap<>();
        for (Rutina r : rutinaRepository.findByProgramaUsuarioId(sigue.getId())) {
            if (r.getPlantilla() != null) copias.putIfAbsent(r.getPlantilla().getCodigo(), r);
        }
        return copias;
    }

    // Una plantilla pasada por las reglas: la base común de seguir y de la vista previa,
    // para que lo que enseña la vista previa sea exactamente lo que se crea.
    private record Plan(Rutina plantilla, List<RutinaEjercicio> origen, ReglasPrograma.Resultado ajuste,
                        List<RutinaEjercicio> filas) { }

    private Plan planificar(Rutina plantilla, Programa programa, Usuario usuario, int minutos, boolean en) {
        List<RutinaEjercicio> origen = rutinaEjercicioRepository.findByRutinaIdOrderByOrdenAsc(plantilla.getId());
        List<ReglasPrograma.Ejercicio> entrada = new ArrayList<>();
        for (int i = 0; i < origen.size(); i++) {
            entrada.add(paraReglas(i, origen.get(i)));
        }
        ReglasPrograma.Resultado ajuste = ReglasPrograma.aplicar(entrada, programa.getNivel(),
                usuario.getNivelExperiencia(), usuario.getObjetivo(), minutos);

        List<RutinaEjercicio> filas = new ArrayList<>();
        int orden = 1;
        for (ReglasPrograma.Ejercicio e : ajuste.ejercicios()) {
            RutinaEjercicio o = origen.get(e.indice());
            RutinaEjercicio c = new RutinaEjercicio();
            c.setEjercicio(o.getEjercicio());
            c.setOrden(orden++);
            c.setSeries(e.series());
            c.setRepeticiones(e.max());
            c.setRepeticionesMin(e.min());
            c.setRepeticionesMax(e.max());
            c.setTiempoDescanso(e.descanso());
            c.setMedida(e.medida());
            c.setTipo(e.tipo());
            c.setPorLado(e.porLado());
            c.setPesoRecomendado(o.getPesoRecomendado());
            c.setNotas(texto(en, o.getNotas(), o.getNotasEn()));
            filas.add(c);
        }
        return new Plan(plantilla, origen, ajuste, filas);
    }

    // Guarda la copia de un plan para el usuario.
    private RutinaConEjerciciosDTO copiar(Plan plan, ProgramaUsuario sigue, Usuario usuario,
                                          LocalDateTime ahora, boolean en) {
        Rutina plantilla = plan.plantilla();
        Rutina copia = new Rutina();
        copia.setNombre(texto(en, plantilla.getNombre(), plantilla.getNombreEn()));
        copia.setDescripcion(texto(en, plantilla.getDescripcion(), plantilla.getDescripcionEn()));
        copia.setCategoria(texto(en, plantilla.getCategoria(), plantilla.getCategoriaEn()));
        copia.setDuracionMinutos(plan.ajuste().duracion());
        copia.setNivel(plan.ajuste().avanzado() ? Nivel.AVANZADO : plantilla.getNivel());
        copia.setEsPredefinida(false);
        copia.setEsPlantilla(false);
        copia.setFechaCreacion(ahora);
        copia.setActiva(true);
        copia.setUsuario(usuario);
        copia.setPlantilla(plantilla);
        copia.setProgramaUsuario(sigue);
        copia = rutinaRepository.save(copia);

        for (RutinaEjercicio fila : plan.filas()) fila.setRutina(copia);
        List<RutinaEjercicio> filas = rutinaEjercicioRepository.saveAll(plan.filas());

        RutinaConEjerciciosDTO dto = rutinaMapper.toConEjercicios(copia);
        dto.setEjercicios(ejerciciosDTO(filas));
        // numEjercicios es una @Formula: en la misma transacción del alta aún no está leída.
        dto.setNumEjercicios(filas.size());
        return dto;
    }

    // Series del primer básico de una lista de ejercicios; null si no hay básicos.
    private static Integer seriesDelPrimerBasico(List<RutinaEjercicio> filas) {
        for (RutinaEjercicio f : filas) {
            if (f.getTipo() == TipoEjercicioRutina.BASICO) return f.getSeries();
        }
        return null;
    }

    private int minutosValidos(Integer minutos) {
        int tiempo = minutos == null ? ReglasPrograma.MINUTOS_POR_DEFECTO : minutos;
        if (!ReglasPrograma.MINUTOS_VALIDOS.contains(tiempo)) {
            throw new InvalidDataException("error.programa.minutosNoValidos");
        }
        return tiempo;
    }

    private Usuario usuarioActual() {
        Integer usuarioId = securityUtils.getCurrentUserId();
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundEntityException("error.usuario.noExiste", usuarioId));
    }

    private static String nombreEjercicio(boolean en, RutinaEjercicio re) {
        return texto(en, re.getEjercicio().getNombre(), re.getEjercicio().getNombreEn());
    }

    // Una fila de la plantilla, como la entienden las reglas. Si le faltara algún
    // opcional (las plantillas del catálogo los llevan todos) se toma lo de siempre.
    private static ReglasPrograma.Ejercicio paraReglas(int indice, RutinaEjercicio re) {
        int max = re.getRepeticionesMax() != null ? re.getRepeticionesMax() : re.getRepeticiones();
        int min = re.getRepeticionesMin() != null ? re.getRepeticionesMin() : max;
        return new ReglasPrograma.Ejercicio(indice,
                re.getTipo() != null ? re.getTipo() : TipoEjercicioRutina.EXTRA,
                re.getSeries(), min, max,
                re.getMedida() != null ? re.getMedida() : MedidaSerie.REPETICIONES,
                re.getPorLado(),
                re.getTiempoDescanso() != null ? re.getTiempoDescanso() : 60,
                re.getEjercicio().getEquipamiento() == Equipamiento.PESO_CORPORAL);
    }

    // Cada rutina distinta de la semana, una vez, en el orden en que aparece.
    private static List<Rutina> rutinasDistintas(Programa programa) {
        Map<Integer, Rutina> distintas = new LinkedHashMap<>();
        for (ProgramaRutina dia : programa.getSemana()) {
            distintas.putIfAbsent(dia.getRutina().getId(), dia.getRutina());
        }
        return new ArrayList<>(distintas.values());
    }

    private void rellenar(ProgramaDTO dto, Programa p) {
        boolean en = enIngles();
        dto.setCodigo(p.getCodigo());
        dto.setNombre(texto(en, p.getNombre(), p.getNombreEn()));
        dto.setDescripcion(texto(en, p.getDescripcion(), p.getDescripcionEn()));
        dto.setNivel(p.getNivel().name());
        dto.setEquipamiento(p.getEquipamiento().name());
        dto.setDiasMin(p.getDiasMin());
        dto.setDiasMax(p.getDiasMax());
        List<DiaProgramaDTO> semana = new ArrayList<>();
        for (ProgramaRutina dia : p.getSemana()) {
            Rutina r = dia.getRutina();
            semana.add(new DiaProgramaDTO(dia.getPosicion(), r.getCodigo(),
                    texto(en, r.getNombre(), r.getNombreEn()), r.getDuracionMinutos()));
        }
        dto.setSemana(semana);
    }

    // Los ejercicios de una rutina, con el nombre del ejercicio y la nota en el idioma de
    // la petición.
    private List<RutinaEjercicioDTO> ejerciciosDTO(List<RutinaEjercicio> filas) {
        boolean en = enIngles();
        List<RutinaEjercicioDTO> lista = new ArrayList<>();
        for (RutinaEjercicio re : filas) {
            RutinaEjercicioDTO dto = rutinaEjercicioMapper.toDTO(re);
            dto.setNombreEjercicio(nombreEjercicio(en, re));
            dto.setNotas(texto(en, re.getNotas(), re.getNotasEn()));
            lista.add(dto);
        }
        return lista;
    }

    private Programa programaOr404(String codigo) {
        return programaRepository.findByCodigoAndActivoTrue(codigo)
                .orElseThrow(() -> new NotFoundEntityException("error.programa.noExiste", codigo));
    }

    private static EquipamientoPrograma equipamiento(String valor) {
        try {
            return EquipamientoPrograma.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("error.programa.equipamientoNoValido", valor);
        }
    }

    // El avanzado usa los programas de intermedio (catálogo, punto 8.7), y el experto igual.
    private static Nivel nivelDePrograma(String valor) {
        Nivel n;
        try {
            n = Nivel.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("error.programa.nivelNoValido", valor);
        }
        return n == Nivel.AVANZADO || n == Nivel.EXPERTO ? Nivel.INTERMEDIO : n;
    }

    private static boolean enIngles() {
        return "en".equals(LocaleContextHolder.getLocale().getLanguage());
    }

    // El texto en inglés si se pide en inglés y lo hay; si no, el español.
    private static String texto(boolean en, String es, String enTexto) {
        return en && enTexto != null && !enTexto.isBlank() ? enTexto : es;
    }
}
