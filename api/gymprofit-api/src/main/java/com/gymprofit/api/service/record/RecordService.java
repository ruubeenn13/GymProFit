package com.gymprofit.api.service.record;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioDTO;
import com.gymprofit.api.dto.entity.progresoejercicio.RecordDestacadoDTO;
import com.gymprofit.api.dto.entity.record.PuntoProgresionDTO;
import com.gymprofit.api.dto.entity.record.RecordDTO;
import com.gymprofit.api.dto.entity.record.RecordsDTO;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.repository.jpa.IEjercicioRealizadoRepository;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.ejercicio.Musculos;
import com.gymprofit.api.service.record.CalculadoraRecords.Evento;
import com.gymprofit.api.service.record.CalculadoraRecords.Marca;
import com.gymprofit.api.service.record.CalculadoraRecords.Resultado;
import com.gymprofit.api.service.record.CalculadoraRecords.Serie;
import com.gymprofit.api.service.record.CalculadoraRecords.Tipo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// ============================================================
// RecordService — récords y progresión, calculados al pedirlos (GP-088)
//
// No hay tabla de récords. Se decidió midiendo: calcularlos desde las series de un
// usuario con 450 sesiones y 10 800 series cuesta 11,4 ms de consulta, y guardarlos
// obligaría a recalcularlos al borrar una sesión, al editar una serie o al borrar un
// ejercicio de una sesión, cada uno un sitio donde el récord guardado puede quedarse
// mintiendo. Calculados, un récord no sobrevive a la sesión que lo batió.
// ============================================================
@Service
@RequiredArgsConstructor
public class RecordService implements IRecordService {

    private final IEjercicioRealizadoRepository ejercicioRealizadoRepository;
    private final IEjercicioRepository ejercicioRepository;
    private final IUsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public RecordsDTO recordsDelUsuarioActual(LocalDate desde) {
        Resultado r = calcular(securityUtils.getCurrentUserId());
        Map<Integer, Ejercicio> catalogo = catalogo(r);

        Comparator<Evento> recienteAntes = Comparator
                .comparing((Evento e) -> e.marca().fecha(), Comparator.nullsLast(Comparator.reverseOrder()));

        List<RecordDTO> vigentes = r.recordVigentePorEjercicio().values().stream()
                .sorted(recienteAntes)
                .map(e -> aDTO(e, catalogo))
                .collect(Collectors.toList());

        List<RecordDTO> recientes = new ArrayList<>();
        if (desde != null) {
            LocalDateTime inicio = desde.atStartOfDay();
            recientes = r.records().stream()
                    .filter(e -> e.marca().fecha() != null && !e.marca().fecha().isBefore(inicio))
                    .sorted(recienteAntes)
                    .map(e -> aDTO(e, catalogo))
                    .collect(Collectors.toList());
        }
        return new RecordsDTO(vigentes, recientes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PuntoProgresionDTO> progresionDelUsuarioActual(Integer ejercicioId) {
        exigirEjercicio(ejercicioId);
        Resultado r = calcular(securityUtils.getCurrentUserId());
        return r.progresion().getOrDefault(ejercicioId, List.of()).stream()
                .map(m -> new PuntoProgresionDTO(m.sesionId(), m.fecha(), m.tipo().name(),
                        m.peso(), m.repeticiones(), m.unoRmEstimado()))
                .collect(Collectors.toList());
    }

    @Override
    public List<List<RecordDTO>> cambiosDeMarcaDeSesion(Integer usuarioId, Integer sesionId) {
        Resultado r = calcular(usuarioId);
        Map<Integer, Ejercicio> catalogo = catalogo(r);
        List<RecordDTO> batidos = new ArrayList<>();
        List<RecordDTO> primeras = new ArrayList<>();
        for (Evento e : r.deSesion(sesionId)) {
            (e.esPrimera() ? primeras : batidos).add(aDTO(e, catalogo));
        }
        return List.of(batidos, primeras);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RecordDestacadoDTO> recordDestacado(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);
        Resultado r = calcular(usuarioId);
        // El de más peso; a igual peso, el más reciente: "sigues ahí", no "una vez lo hiciste".
        return r.recordVigentePorEjercicio().values().stream()
                .map(Evento::marca)
                .filter(m -> m.tipo() == Tipo.PESO)
                .max(Comparator.comparing(Marca::peso)
                        .thenComparing(Marca::fecha, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(m -> {
                    Ejercicio e = ejercicioRepository.findById(m.ejercicioId()).orElse(null);
                    return new RecordDestacadoDTO(m.ejercicioId(), e != null ? e.getNombre() : null,
                            m.peso(), m.repeticiones(), m.fecha());
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgresoEjercicioDTO> historialLegado(Integer usuarioId, Integer ejercicioId) {
        securityUtils.checkOwnership(usuarioId);
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new NotFoundEntityException("El usuario con id " + usuarioId + " no existe");
        }
        exigirEjercicio(ejercicioId);

        List<Marca> puntos = new ArrayList<>(calcular(usuarioId).progresion().getOrDefault(ejercicioId, List.of()));
        puntos.sort(Comparator.comparing(Marca::fecha, Comparator.nullsLast(Comparator.reverseOrder())));
        // Misma forma que devolvía la tabla retirada; el id era el de su fila y ya no existe.
        return puntos.stream()
                .map(m -> new ProgresoEjercicioDTO(null, usuarioId, ejercicioId, m.fecha(),
                        m.peso(), m.repeticiones(), null, null))
                .collect(Collectors.toList());
    }

    // Lee las series del usuario y hace el recorrido.
    private Resultado calcular(Integer usuarioId) {
        List<Serie> series = ejercicioRealizadoRepository.seriesCompletadasDeUsuario(usuarioId).stream()
                .map(f -> new Serie((Integer) f[0], (LocalDateTime) f[1], (Integer) f[2],
                        (BigDecimal) f[3], ((Number) f[4]).intValue()))
                .collect(Collectors.toList());
        return CalculadoraRecords.calcular(series);
    }

    // Los ejercicios que aparecen en el resultado, en una sola consulta.
    private Map<Integer, Ejercicio> catalogo(Resultado r) {
        Set<Integer> ids = new HashSet<>(r.progresion().keySet());
        return ejercicioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Ejercicio::getId, Function.identity()));
    }

    private void exigirEjercicio(Integer ejercicioId) {
        if (!ejercicioRepository.existsById(ejercicioId)) {
            throw new NotFoundEntityException("El ejercicio con id " + ejercicioId + " no existe");
        }
    }

    private static RecordDTO aDTO(Evento evento, Map<Integer, Ejercicio> catalogo) {
        Marca m = evento.marca();
        Marca antes = evento.anterior();
        Ejercicio e = catalogo.get(m.ejercicioId());
        return new RecordDTO(
                m.ejercicioId(),
                e != null ? e.getNombre() : null,
                e != null ? e.getNombreEn() : null,
                e != null ? Musculos.normalizar(e.getMusculoPrimario(), e.getGrupoMuscular()) : null,
                m.tipo().name(),
                m.peso(),
                m.repeticiones(),
                m.unoRmEstimado(),
                m.fecha(),
                m.sesionId(),
                antes != null ? antes.peso() : null,
                antes != null ? antes.repeticiones() : null);
    }
}
