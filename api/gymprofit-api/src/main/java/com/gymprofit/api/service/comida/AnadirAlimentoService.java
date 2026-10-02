package com.gymprofit.api.service.comida;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.dto.entity.comida.AnadirAlimentoDTO;
import com.gymprofit.api.dto.entity.comida.AnadirAlimentoRespuestaDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoComida;
import com.gymprofit.api.entity.AlimentoRacion;
import com.gymprofit.api.entity.Comida;
import com.gymprofit.api.enums.TipoComida;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.mappers.AlimentoComidaMapper;
import com.gymprofit.api.mappers.ComidaMapper;
import com.gymprofit.api.repository.jpa.IAlimentoComidaRepository;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IComidaRepository;
import com.gymprofit.api.service.alimentocomida.IAlimentoComidaService;
import com.gymprofit.api.service.codigo.CodigoBarrasService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

// ============================================================
// AnadirAlimentoService — añadir un alimento a la comida de un día en un viaje (1.6.1)
//
// Antes eran hasta cuatro peticiones (importar, buscar la comida del día, crearla y
// añadir), cada una con su viaje y su forma de fallar a medias. Aquí, en una transacción:
//   1. Se valida lo pedido (400) antes de tocar nada.
//   2. El alimento: por id, con la regla de siempre (DEC-027: uno de otro usuario, 403;
//      del catálogo o tuyo, sí); por código, por CodigoBarrasService (lo tuyo, el
//      catálogo, productos_off y, con cupo, Open Food Facts; si no, 404 o 503).
//   3. La comida de ese día y ese tipo del usuario del token (DEC-013); si no hay, se crea.
//   4. La línea: si el alimento ya está en esa comida, se suma (con la misma ración, más
//      raciones; si no, en gramos); si no, una nueva. Y los totales de la comida.
// Si algo falla, no queda nada: ni comida vacía ni producto materializado a medias.
// ============================================================
@Service
public class AnadirAlimentoService {

    private final IComidaRepository comidaRepository;
    private final IAlimentoRepository alimentoRepository;
    private final IAlimentoComidaRepository lineaRepository;
    private final IAlimentoComidaService alimentoComidaService;
    private final CodigoBarrasService codigoBarrasService;
    private final ComidaMapper comidaMapper;
    private final AlimentoComidaMapper lineaMapper;
    private final SecurityUtils securityUtils;

    public AnadirAlimentoService(IComidaRepository comidaRepository, IAlimentoRepository alimentoRepository,
                                 IAlimentoComidaRepository lineaRepository, IAlimentoComidaService alimentoComidaService,
                                 CodigoBarrasService codigoBarrasService, ComidaMapper comidaMapper,
                                 AlimentoComidaMapper lineaMapper, SecurityUtils securityUtils) {
        this.comidaRepository = comidaRepository;
        this.alimentoRepository = alimentoRepository;
        this.lineaRepository = lineaRepository;
        this.alimentoComidaService = alimentoComidaService;
        this.codigoBarrasService = codigoBarrasService;
        this.comidaMapper = comidaMapper;
        this.lineaMapper = lineaMapper;
        this.securityUtils = securityUtils;
    }

    /**
     * Añade el alimento a la comida de ese día y ese tipo del usuario del token.
     *
     * @param pedido fecha, tipo, alimento (id o código) y cantidad (gramos o raciones).
     * @return la comida con sus totales y la línea creada o sumada.
     * @throws InvalidDataException     (400) si lo pedido no cuadra o la ración es de otro alimento.
     * @throws com.gymprofit.api.exceptions.UnauthorizedException (403) si el alimento es de otro usuario.
     * @throws NotFoundEntityException  (404) si el alimento o el código no existen.
     */
    @Transactional
    public AnadirAlimentoRespuestaDTO anadir(AnadirAlimentoDTO pedido) {
        TipoComida tipo = tipo(pedido.getTipoComida());
        boolean porId = pedido.getAlimentoId() != null;
        boolean porCodigo = pedido.getBarcode() != null && !pedido.getBarcode().isBlank();
        if (porId == porCodigo) {
            throw new InvalidDataException("error.anadir.alimento");
        }
        boolean conRacion = pedido.getRacionId() != null || pedido.getRacionIndice() != null;
        if (pedido.getRacionId() != null && pedido.getRacionIndice() != null) {
            throw new InvalidDataException("error.racion.incompleta");
        }
        if (!conRacion && pedido.getRaciones() != null) {
            throw new InvalidDataException("error.racion.incompleta");
        }
        if (!conRacion && (pedido.getCantidadGramos() == null || pedido.getCantidadGramos().signum() <= 0)) {
            throw new InvalidDataException("error.anadir.cantidad");
        }

        Alimento alimento = porId ? porId(pedido.getAlimentoId()) : porCodigo(pedido.getBarcode().trim());

        // La línea con lo pedido, aún sin comida: así una ración mal pedida no deja nada.
        AlimentoComida nueva = new AlimentoComida();
        nueva.setAlimento(alimento);
        if (conRacion) {
            Integer racionId = pedido.getRacionId() != null ? pedido.getRacionId()
                    : racionEn(alimento, pedido.getRacionIndice());
            alimentoComidaService.ponerCantidad(nueva, pedido.getCantidadGramos(), racionId, pedido.getRaciones());
        } else {
            alimentoComidaService.ponerGramos(nueva, pedido.getCantidadGramos());
        }

        Comida comida = comidaDelDia(tipo, pedido);
        AlimentoComida linea = lineaRepository.findByComidaIdAndAlimentoId(comida.getId(), alimento.getId())
                .map(existente -> sumar(existente, nueva))
                .orElseGet(() -> {
                    nueva.setComida(comida);
                    return nueva;
                });
        AlimentoComida guardada = lineaRepository.save(linea);
        alimentoComidaService.recalcularTotales(comida.getId());
        return new AnadirAlimentoRespuestaDTO(comidaMapper.toDTO(comida), lineaMapper.toDTO(guardada));
    }

    private static TipoComida tipo(String texto) {
        try {
            return TipoComida.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("error.tipoComida.invalido", texto);
        }
    }

    private Alimento porId(Integer id) {
        Alimento alimento = alimentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("error.alimento.noExiste", id));
        // El id llega en el cuerpo: uno de otro usuario es suyo, 403 (DEC-027).
        securityUtils.checkOwnershipIfOwned(alimento.getUsuario() == null ? null : alimento.getUsuario().getId());
        return alimento;
    }

    private Alimento porCodigo(String codigo) {
        AlimentoDTO dto = codigoBarrasService.porCodigo(codigo);
        return alimentoRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundEntityException("error.alimento.noExiste", dto.getId()));
    }

    // La ración en esa posición de la lista del alimento (en su orden).
    private static Integer racionEn(Alimento alimento, int indice) {
        List<AlimentoRacion> raciones = alimento.getRaciones();
        if (raciones == null || indice >= raciones.size()) {
            throw new InvalidDataException("error.racion.otroAlimento", indice);
        }
        return raciones.get(indice).getId();
    }

    private Comida comidaDelDia(TipoComida tipo, AnadirAlimentoDTO pedido) {
        Integer usuarioId = securityUtils.getCurrentUserId();
        LocalDateTime inicio = pedido.getFecha().atStartOfDay();
        LocalDateTime fin = pedido.getFecha().atTime(LocalTime.MAX);
        return comidaRepository.findFirstByUsuarioIdAndTipoComidaAndFechaBetweenOrderByIdAsc(usuarioId, tipo, inicio, fin)
                .orElseGet(() -> {
                    Comida comida = new Comida();
                    comida.setUsuario(securityUtils.getCurrentUser());
                    comida.setTipoComida(tipo);
                    // Como la crea la app: el día a las 00:00.
                    comida.setFecha(inicio);
                    comida.setTotalCalorias(0);
                    comida.setTotalProteinas(BigDecimal.ZERO);
                    comida.setTotalCarbohidratos(BigDecimal.ZERO);
                    comida.setTotalGrasas(BigDecimal.ZERO);
                    return comidaRepository.save(comida);
                });
    }

    // El alimento ya estaba en la comida: una sola línea, con la cantidad sumada. Si las
    // dos son de la misma ración, siguen siéndolo; si no, la suma va en gramos.
    private AlimentoComida sumar(AlimentoComida existente, AlimentoComida nueva) {
        BigDecimal gramos = existente.getCantidadGramos().add(nueva.getCantidadGramos());
        boolean mismaRacion = existente.getRacion() != null && nueva.getRacion() != null
                && existente.getRacion().getId().equals(nueva.getRacion().getId());
        if (mismaRacion) {
            alimentoComidaService.ponerCantidad(existente, gramos, existente.getRacion().getId(),
                    existente.getRaciones().add(nueva.getRaciones()));
        } else {
            existente.setRacion(null);
            existente.setRaciones(null);
            alimentoComidaService.ponerGramos(existente, gramos);
        }
        return existente;
    }
}
