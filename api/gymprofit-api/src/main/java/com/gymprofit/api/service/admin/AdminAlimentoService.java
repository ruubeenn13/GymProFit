package com.gymprofit.api.service.admin;

import com.gymprofit.api.dto.admin.AdminAlimentoDTO;
import com.gymprofit.api.dto.admin.AdminAlimentosResumenDTO;
import com.gymprofit.api.dto.common.PageDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IProductoOffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

// ============================================================
// AdminAlimentoService — el catálogo de alimentos en la web (GP-085)
//
// Solo filas sin dueño. En alimentos, usuario_id NULL es el catálogo público y un
// usuario_id es la dieta de alguien: esos no salen por /admin, ni filtrando.
// El origen se deduce del código de barras: lo tiene lo que llegó de Open Food
// Facts al escanear (DEC-032) y no lo tiene lo que se escribió a mano.
// ============================================================
@Service
@RequiredArgsConstructor
public class AdminAlimentoService implements IAdminAlimentoService {

    private static final int TAMANO_MAXIMO = 100;

    private final IAlimentoRepository alimentoRepository;
    private final IProductoOffRepository productoOffRepository;

    @Override
    @Transactional(readOnly = true)
    public PageDTO<AdminAlimentoDTO> listar(String q, String categoria, boolean sinIngles, String origen,
                                            String fuente, int page, int size) {
        String patron = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        PageRequest pagina = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), TAMANO_MAXIMO),
                Sort.by("nombre", "id"));
        Page<Alimento> alimentos = alimentoRepository.buscarCatalogoAdmin(patron,
                categoria == null || categoria.isBlank() ? null : categoria.trim(),
                sinIngles, deOpenFoodFacts(origen), fuente(fuente), pagina);
        return PageDTO.of(alimentos, alimentos.getContent().stream().map(AdminAlimentoService::aDTO).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAlimentosResumenDTO resumen() {
        java.util.Map<String, Long> porFuente = new java.util.LinkedHashMap<>();
        for (String f : List.of("CIQUAL", "USDA", "OFF", "MANUAL")) porFuente.put(f, 0L);
        for (Object[] fila : alimentoRepository.contarCatalogoPorFuente()) {
            porFuente.merge(fila[0] == null ? "MANUAL" : (String) fila[0], ((Number) fila[1]).longValue(), Long::sum);
        }
        return new AdminAlimentosResumenDTO(alimentoRepository.countByUsuarioIsNull(),
                alimentoRepository.contarCatalogoSinIngles(), alimentoRepository.categoriasDelCatalogo(),
                porFuente, productoOffRepository.count());
    }

    private static Boolean deOpenFoodFacts(String origen) {
        if (origen == null || origen.isBlank()) return null;
        return switch (origen.trim().toUpperCase(Locale.ROOT)) {
            case "OPEN_FOOD_FACTS" -> true;
            case "MANUAL" -> false;
            default -> throw new InvalidDataException("error.origen.invalido", origen);
        };
    }

    // Fuentes del catálogo (GP-127): CIQUAL y USDA, los básicos; OFF, Open Food Facts;
    // MANUAL, los hechos a mano (sin fuente).
    private static String fuente(String fuente) {
        if (fuente == null || fuente.isBlank()) return null;
        String f = fuente.trim().toUpperCase(Locale.ROOT);
        if (!java.util.Set.of("CIQUAL", "USDA", "OFF", "MANUAL").contains(f)) {
            throw new InvalidDataException("error.fuente.invalida", fuente);
        }
        return f;
    }

    /** El alimento como lo edita la web (también en los avisos, lote 1.6.1). */
    public static AdminAlimentoDTO aDTO(Alimento a) {
        return AdminAlimentoDTO.builder()
                .id(a.getId())
                .nombre(a.getNombre())
                .nombreEn(a.getNombreEn())
                .marca(a.getMarca())
                .categoria(a.getCategoria())
                .barcode(a.getBarcode())
                .calorias(a.getCalorias())
                .proteinas(a.getProteinas())
                .carbohidratos(a.getCarbohidratos())
                .grasas(a.getGrasas())
                .fibra(a.getFibra())
                .porcionGramos(a.getPorcionGramos())
                .activo(Boolean.TRUE.equals(a.getActivo()))
                .origen(a.getBarcode() != null ? "OPEN_FOOD_FACTS" : "MANUAL")
                .fuente(a.getFuente())
                .revisado(a.isRevisado())
                .build();
    }
}
