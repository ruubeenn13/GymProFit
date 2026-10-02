package com.gymprofit.api.service.alimento;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.admin.AdminAvisoAlimentoDTO;
import com.gymprofit.api.dto.common.PageDTO;
import com.gymprofit.api.dto.entity.alimento.AvisoAlimentoCreateDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AvisoAlimento;
import com.gymprofit.api.entity.ProductoOff;
import com.gymprofit.api.enums.MotivoAviso;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IAvisoAlimentoRepository;
import com.gymprofit.api.repository.jpa.IProductoOffRepository;
import com.gymprofit.api.service.admin.AdminAlimentoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// ============================================================
// AvisoAlimentoService — reportar un alimento y los avisos pendientes (lote 1.6.1)
//
// Reportar: el alimento del catálogo por su id (uno de otro usuario, 403, DEC-027; el
// tuyo, 400: lo arreglas tú, y el administrador no ve tus alimentos), o el código de un
// producto (si ya está en el catálogo, el aviso es de ese alimento; si no, del código).
// Un solo aviso abierto por alimento y motivo, con las veces; FrenoAvisos decide si suma.
// Administrar: la lista de pendientes, los más repetidos primero, y «Resuelto».
// ============================================================
@Service
public class AvisoAlimentoService {

    private final IAvisoAlimentoRepository avisoRepository;
    private final IAlimentoRepository alimentoRepository;
    private final IProductoOffRepository productoOffRepository;
    private final FrenoAvisos freno;
    private final SecurityUtils securityUtils;

    public AvisoAlimentoService(IAvisoAlimentoRepository avisoRepository, IAlimentoRepository alimentoRepository,
                                IProductoOffRepository productoOffRepository, FrenoAvisos freno,
                                SecurityUtils securityUtils) {
        this.avisoRepository = avisoRepository;
        this.alimentoRepository = alimentoRepository;
        this.productoOffRepository = productoOffRepository;
        this.freno = freno;
        this.securityUtils = securityUtils;
    }

    /**
     * Reporta un alimento. Si el freno de la cuenta no lo admite, no hace nada y no lo dice.
     *
     * @param pedido alimento (id o código) y motivo.
     * @throws InvalidDataException    (400) si no cuadra, el motivo no existe o el alimento es tuyo.
     * @throws com.gymprofit.api.exceptions.UnauthorizedException (403) si es de otro usuario.
     * @throws NotFoundEntityException (404) si el alimento o el código no existen.
     */
    @Transactional
    public void reportar(AvisoAlimentoCreateDTO pedido) {
        MotivoAviso motivo = motivo(pedido.getMotivo());
        boolean porId = pedido.getAlimentoId() != null;
        boolean porCodigo = pedido.getBarcode() != null && !pedido.getBarcode().isBlank();
        if (porId == porCodigo) {
            throw new InvalidDataException("error.aviso.alimento");
        }

        Alimento alimento = null;
        String barcode = null;
        if (porId) {
            alimento = delCatalogo(pedido.getAlimentoId());
        } else {
            String codigo = pedido.getBarcode().trim();
            Optional<Alimento> materializado = alimentoRepository.findFirstByBarcodeAndUsuarioIsNullOrderByIdAsc(codigo);
            if (materializado.isPresent()) {
                alimento = materializado.get();
            } else if (productoOffRepository.findByCodigo(codigo).isPresent()) {
                barcode = codigo;
            } else {
                throw new NotFoundEntityException("error.aviso.noExiste", codigo);
            }
        }

        String clave = alimento != null ? "a:" + alimento.getId() : "c:" + barcode;
        if (!freno.admitir(securityUtils.getCurrentUserId(), clave, motivo.name())) return;

        LocalDateTime ahora = LocalDateTime.now();
        Optional<AvisoAlimento> abierto = avisoRepository.findByClaveAndMotivoAndAbiertoTrue(clave, motivo);
        AvisoAlimento aviso;
        if (abierto.isPresent()) {
            aviso = abierto.get();
            aviso.setVeces(aviso.getVeces() + 1);
        } else {
            aviso = new AvisoAlimento();
            aviso.setAlimento(alimento);
            aviso.setBarcode(barcode);
            aviso.setClave(clave);
            aviso.setMotivo(motivo);
            aviso.setVeces(1);
            aviso.setAbierto(true);
            aviso.setCreado(ahora);
        }
        aviso.setActualizado(ahora);
        avisoRepository.save(aviso);
    }

    /**
     * Los avisos pendientes, los más repetidos y recientes primero (ADMIN).
     *
     * @param page página, desde 0.
     * @param size tamaño, de 1 a 100.
     */
    @Transactional(readOnly = true)
    public PageDTO<AdminAvisoAlimentoDTO> pendientes(int page, int size) {
        securityUtils.requireAdmin();
        PageRequest pagina = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Order.desc("veces"), Sort.Order.desc("actualizado"), Sort.Order.desc("id")));
        Page<AvisoAlimento> avisos = avisoRepository.findByAbiertoTrue(pagina);

        List<String> codigos = avisos.getContent().stream()
                .filter(a -> a.getAlimento() == null).map(AvisoAlimento::getBarcode).toList();
        Map<String, ProductoOff> productos = new HashMap<>();
        if (!codigos.isEmpty()) {
            productoOffRepository.findByCodigoIn(codigos).forEach(p -> productos.put(p.getCodigo(), p));
        }

        List<AdminAvisoAlimentoDTO> contenido = avisos.getContent().stream().map(a -> {
            Alimento al = a.getAlimento();
            ProductoOff p = al == null ? productos.get(a.getBarcode()) : null;
            return new AdminAvisoAlimentoDTO(a.getId(), al == null ? null : al.getId(),
                    al != null ? al.getBarcode() : a.getBarcode(),
                    al != null ? al.getNombre() : (p == null ? null : p.getNombre()),
                    al != null ? al.getMarca() : (p == null ? null : p.getMarca()),
                    al != null ? al.getFuente() : "OFF",
                    a.getMotivo().name(), a.getVeces(), a.getCreado(), a.getActualizado(),
                    al == null ? null : AdminAlimentoService.aDTO(al));
        }).toList();
        return new PageDTO<>(contenido, avisos.getNumber(), avisos.getSize(), avisos.getTotalElements(),
                Math.max(1, avisos.getTotalPages()), avisos.isLast());
    }

    /**
     * Marca un aviso como resuelto (ADMIN): sale de pendientes, y el siguiente abre otro.
     *
     * @param id el aviso.
     * @throws NotFoundEntityException (404) si no existe.
     */
    @Transactional
    public void resolver(Integer id) {
        securityUtils.requireAdmin();
        AvisoAlimento aviso = avisoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("error.aviso.noEncontrado", id));
        aviso.setAbierto(null);
        aviso.setResuelto(LocalDateTime.now());
        avisoRepository.save(aviso);
    }

    private static MotivoAviso motivo(String texto) {
        try {
            return MotivoAviso.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidDataException("error.aviso.motivo", texto);
        }
    }

    // Un alimento del catálogo. Uno de otro usuario es suyo: 403 sin decir nada de él. El
    // tuyo no se reporta: lo editas tú, y el administrador no ve los alimentos de nadie.
    private Alimento delCatalogo(Integer id) {
        Alimento alimento = alimentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("error.alimento.noExiste", id));
        if (alimento.getUsuario() != null) {
            securityUtils.checkOwnershipIfOwned(alimento.getUsuario().getId());
            throw new InvalidDataException("error.aviso.propio");
        }
        return alimento;
    }
}
