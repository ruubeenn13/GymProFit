package com.gymprofit.api.service.admin;

import com.gymprofit.api.dto.admin.AdminEjercicioDTO;
import com.gymprofit.api.dto.admin.AdminEjercicioDetalleDTO;
import com.gymprofit.api.dto.admin.AdminEjercicioUpdateDTO;
import com.gymprofit.api.dto.admin.AdminEjerciciosResumenDTO;
import com.gymprofit.api.dto.common.PageDTO;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.enums.Equipamiento;
import com.gymprofit.api.enums.GrupoMuscular;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.repository.jpa.IRutinaEjercicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;

// ============================================================
// AdminEjercicioService — el catálogo de ejercicios en la web (GP-085)
//
// Sirve sobre todo para traducir: 752 de 873 ejercicios activos se llamaban igual
// en español que en inglés (DEC-020). «Sin revisar» es activo y con nombre_revisado
// a false; los inactivos no los ve nadie y no cuentan como pendientes.
// ============================================================
@Service
@RequiredArgsConstructor
public class AdminEjercicioService implements IAdminEjercicioService {

    private static final int TAMANO_MAXIMO = 100;

    private final IEjercicioRepository ejercicioRepository;
    private final IRutinaEjercicioRepository rutinaEjercicioRepository;

    @Override
    @Transactional(readOnly = true)
    public PageDTO<AdminEjercicioDTO> listar(String q, String grupo, String equipamiento, boolean sinRevisar,
                                             int page, int size) {
        String patron = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        PageRequest pagina = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), TAMANO_MAXIMO),
                Sort.by("nombre", "id"));
        Page<Ejercicio> ejercicios = ejercicioRepository.buscarParaAdmin(patron,
                valor(GrupoMuscular.class, grupo, "error.grupoMuscular.invalido"),
                valor(Equipamiento.class, equipamiento, "error.equipamiento.invalido"),
                sinRevisar, pagina);
        return PageDTO.of(ejercicios, ejercicios.getContent().stream().map(AdminEjercicioService::fila).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminEjerciciosResumenDTO resumen() {
        return new AdminEjerciciosResumenDTO(
                ejercicioRepository.countByActivoTrue(),
                ejercicioRepository.countByActivoTrueAndNombreRevisadoFalse(),
                Arrays.stream(Equipamiento.values())
                        .map(e -> new AdminEjerciciosResumenDTO.Opcion(e.name(), e.getEtiqueta(), e.getEtiquetaEn()))
                        .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminEjercicioDetalleDTO detalle(Integer id) {
        return detalle(buscar(id));
    }

    @Override
    @Transactional
    public AdminEjercicioDetalleDTO guardar(Integer id, AdminEjercicioUpdateDTO dto) {
        Ejercicio e = buscar(id);
        String nombre = dto.getNombre().trim();
        String nombreEn = vacioANulo(dto.getNombreEn());

        e.setNombre(nombre);
        e.setNombreEn(nombreEn);
        e.setDescripcion(vacioANulo(dto.getDescripcion()));
        e.setDescripcionEn(vacioANulo(dto.getDescripcionEn()));
        e.setInstrucciones(vacioANulo(dto.getInstrucciones()));
        e.setInstruccionesEn(vacioANulo(dto.getInstruccionesEn()));
        e.setGrupoMuscular(dto.getGrupoMuscular());
        e.setMusculoPrimario(vacioANulo(dto.getMusculoPrimario()));
        e.setMusculoPrimarioEn(vacioANulo(dto.getMusculoPrimarioEn()));
        e.setEquipamiento(dto.getEquipamiento());
        e.setDificultad(dto.getDificultad());
        e.setActivo(dto.getActivo());
        // Si el español ya no se dice igual que el inglés, alguien lo ha traducido.
        e.setNombreRevisado(dto.getNombreRevisado() || !nombre.equalsIgnoreCase(nombreEn));

        return detalle(ejercicioRepository.save(e));
    }

    private Ejercicio buscar(Integer id) {
        return ejercicioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("error.ejercicio.noExiste", id));
    }

    private AdminEjercicioDetalleDTO detalle(Ejercicio e) {
        return AdminEjercicioDetalleDTO.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .nombreEn(e.getNombreEn())
                .descripcion(e.getDescripcion())
                .descripcionEn(e.getDescripcionEn())
                .instrucciones(e.getInstrucciones())
                .instruccionesEn(e.getInstruccionesEn())
                .grupoMuscular(nombre(e.getGrupoMuscular()))
                .musculoPrimario(e.getMusculoPrimario())
                .musculoPrimarioEn(e.getMusculoPrimarioEn())
                .equipamiento(nombre(e.getEquipamiento()))
                .dificultad(nombre(e.getDificultad()))
                .activo(Boolean.TRUE.equals(e.getActivo()))
                .nombreRevisado(Boolean.TRUE.equals(e.getNombreRevisado()))
                .imagenUrl(e.getImagenUrl())
                .imagenUrl2(e.getImagenUrl2())
                .origen(e.getWgerId() != null ? "WGER" : e.getFedId() != null ? "FREE_EXERCISE_DB" : "MANUAL")
                .equipoNecesario(e.getEquipoNecesario())
                .rutinas(rutinaEjercicioRepository.contarRutinasConEjercicio(e.getId()))
                .build();
    }

    private static AdminEjercicioDTO fila(Ejercicio e) {
        return new AdminEjercicioDTO(e.getId(), e.getNombre(), e.getNombreEn(), nombre(e.getGrupoMuscular()),
                nombre(e.getEquipamiento()), nombre(e.getDificultad()), Boolean.TRUE.equals(e.getActivo()),
                Boolean.TRUE.equals(e.getNombreRevisado()));
    }

    private static String nombre(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /**
     * Valor de una lista cerrada a partir del texto de un filtro. Vacío no filtra; uno
     * que no es de la lista es un 400, no un 500.
     */
    static <E extends Enum<E>> E valor(Class<E> tipo, String texto, String claveError) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return Enum.valueOf(tipo, texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidDataException(claveError, texto);
        }
    }
}
