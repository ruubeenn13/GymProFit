package com.gymprofit.api.service.admin;

import com.gymprofit.api.dto.admin.AdminAlimentoDTO;
import com.gymprofit.api.dto.admin.AdminAlimentosResumenDTO;
import com.gymprofit.api.dto.common.PageDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional(readOnly = true)
    public PageDTO<AdminAlimentoDTO> listar(String q, String categoria, boolean sinIngles, String origen,
                                            int page, int size) {
        String patron = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        PageRequest pagina = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), TAMANO_MAXIMO),
                Sort.by("nombre", "id"));
        Page<Alimento> alimentos = alimentoRepository.buscarCatalogoAdmin(patron,
                categoria == null || categoria.isBlank() ? null : categoria.trim(),
                sinIngles, deOpenFoodFacts(origen), pagina);
        return PageDTO.of(alimentos, alimentos.getContent().stream().map(AdminAlimentoService::aDTO).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAlimentosResumenDTO resumen() {
        return new AdminAlimentosResumenDTO(alimentoRepository.countByUsuarioIsNull(),
                alimentoRepository.contarCatalogoSinIngles(), alimentoRepository.categoriasDelCatalogo());
    }

    private static Boolean deOpenFoodFacts(String origen) {
        if (origen == null || origen.isBlank()) return null;
        return switch (origen.trim().toUpperCase(Locale.ROOT)) {
            case "OPEN_FOOD_FACTS" -> true;
            case "MANUAL" -> false;
            default -> throw new InvalidDataException("error.origen.invalido", origen);
        };
    }

    private static AdminAlimentoDTO aDTO(Alimento a) {
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
                .build();
    }
}
