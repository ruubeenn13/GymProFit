package com.gymprofit.api.service.programa;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.programa.DiaProgramaDTO;
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
import com.gymprofit.api.enums.TipoEjercicioRutina;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.mappers.RutinaEjercicioMapper;
import com.gymprofit.api.mappers.RutinaMapper;
import com.gymprofit.api.repository.jpa.IProgramaRepository;
import com.gymprofit.api.repository.jpa.IProgramaUsuarioRepository;
import com.gymprofit.api.repository.jpa.IRutinaEjercicioRepository;
import com.gymprofit.api.repository.jpa.IRutinaRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// ============================================================
// ProgramaService — catálogo de programas y seguir uno (GP-074)
//
// El catálogo es público para cualquier cuenta, como el de ejercicios (DEC-027): el
// código del programa no tiene dueño. Seguir uno escribe solo para el usuario del
// token (DEC-013): no hay id de usuario en la petición.
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
        int tiempo = minutos == null ? ReglasPrograma.MINUTOS_POR_DEFECTO : minutos;
        if (!ReglasPrograma.MINUTOS_VALIDOS.contains(tiempo)) {
            throw new InvalidDataException("error.programa.minutosNoValidos");
        }
        Programa programa = programaOr404(codigo);

        Integer usuarioId = securityUtils.getCurrentUserId();
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundEntityException("error.usuario.noExiste", usuarioId));
        logger.info("Usuario id={} sigue el programa {} con {} min", usuarioId, codigo, tiempo);

        LocalDateTime ahora = LocalDateTime.now();
        ProgramaUsuario sigue = new ProgramaUsuario();
        sigue.setUsuario(usuario);
        sigue.setPrograma(programa);
        sigue.setMinutos(tiempo);
        sigue.setFechaInicio(ahora);
        sigue = programaUsuarioRepository.save(sigue);

        List<RutinaConEjerciciosDTO> copias = new ArrayList<>();
        for (Rutina plantilla : rutinasDistintas(programa)) {
            copias.add(copiar(plantilla, programa, sigue, usuario, tiempo, ahora));
        }
        return new ProgramaSeguidoDTO(sigue.getId(), programa.getCodigo(), tiempo, ahora, copias);
    }

    // Copia una plantilla para el usuario, con las reglas aplicadas.
    private RutinaConEjerciciosDTO copiar(Rutina plantilla, Programa programa, ProgramaUsuario sigue,
                                          Usuario usuario, int minutos, LocalDateTime ahora) {
        List<RutinaEjercicio> origen = rutinaEjercicioRepository.findByRutinaIdOrderByOrdenAsc(plantilla.getId());
        List<ReglasPrograma.Ejercicio> entrada = new ArrayList<>();
        for (int i = 0; i < origen.size(); i++) {
            entrada.add(paraReglas(i, origen.get(i)));
        }
        ReglasPrograma.Resultado ajuste = ReglasPrograma.aplicar(entrada, programa.getNivel(),
                usuario.getNivelExperiencia(), usuario.getObjetivo(), minutos);

        boolean en = enIngles();
        Rutina copia = new Rutina();
        copia.setNombre(texto(en, plantilla.getNombre(), plantilla.getNombreEn()));
        copia.setDescripcion(texto(en, plantilla.getDescripcion(), plantilla.getDescripcionEn()));
        copia.setCategoria(texto(en, plantilla.getCategoria(), plantilla.getCategoriaEn()));
        copia.setDuracionMinutos(ajuste.duracion());
        copia.setNivel(ajuste.avanzado() ? Nivel.AVANZADO : plantilla.getNivel());
        copia.setEsPredefinida(false);
        copia.setEsPlantilla(false);
        copia.setFechaCreacion(ahora);
        copia.setActiva(true);
        copia.setUsuario(usuario);
        copia.setPlantilla(plantilla);
        copia.setProgramaUsuario(sigue);
        copia = rutinaRepository.save(copia);

        List<RutinaEjercicio> filas = new ArrayList<>();
        int orden = 1;
        for (ReglasPrograma.Ejercicio e : ajuste.ejercicios()) {
            RutinaEjercicio o = origen.get(e.indice());
            RutinaEjercicio c = new RutinaEjercicio();
            c.setRutina(copia);
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
        filas = rutinaEjercicioRepository.saveAll(filas);

        RutinaConEjerciciosDTO dto = rutinaMapper.toConEjercicios(copia);
        dto.setEjercicios(ejerciciosDTO(filas));
        // numEjercicios es una @Formula: en la misma transacción del alta aún no está leída.
        dto.setNumEjercicios(filas.size());
        return dto;
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
            dto.setNombreEjercicio(texto(en, re.getEjercicio().getNombre(), re.getEjercicio().getNombreEn()));
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
