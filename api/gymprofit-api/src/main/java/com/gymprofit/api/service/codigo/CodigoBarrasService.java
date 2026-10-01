package com.gymprofit.api.service.codigo;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.dto.entity.productooff.ProductoOffImportDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.ProductoOff;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.ServicioSaturadoException;
import com.gymprofit.api.mappers.AlimentoMapper;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IProductoOffRepository;
import com.gymprofit.api.service.busqueda.IndiceAlimentos;
import com.gymprofit.api.service.externo.LimiteOpenFoodFacts;
import com.gymprofit.api.service.externo.OpenFoodFactsClient;
import com.gymprofit.api.service.productooff.MaterializadorProducto;
import com.gymprofit.api.service.productooff.ProductoOffService;
import com.gymprofit.api.service.productooff.ProductoOffValidacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// ============================================================
// CodigoBarrasService — de un código de barras a un alimento (GP-160)
//
// GET /alimentos/codigo/{codigo} y POST /alimentos/importar pasan por aquí. Por orden:
//   1. Tus alimentos con ese código (solo en GET /codigo: lo que se importa es catálogo).
//   2. El catálogo: el producto ya materializado (o uno con código hecho por ADMIN).
//   3. productos_off: se materializa desde ahí, sin salir de casa.
//   4. Open Food Facts, una lectura, si queda cupo (LimiteOpenFoodFacts): el producto se
//      guarda en productos_off y se materializa. Sin cupo, 503 con Retry-After.
// Si no está en ninguna parte, 404, y la app ofrece crearlo con el código puesto (1.6.1).
// Lo que Open Food Facts no tiene se recuerda un día, para no gastar cupo dos veces.
//
// Un alimento propio con código nunca le sale a otro usuario (DEC-027): el paso 1
// filtra por el usuario del token y los demás son catálogo, sin dueño.
// ============================================================
@Service
public class CodigoBarrasService {

    private static final Logger logger = LoggerFactory.getLogger(CodigoBarrasService.class);

    private final IAlimentoRepository alimentoRepository;
    private final IProductoOffRepository productoOffRepository;
    private final MaterializadorProducto materializador;
    private final ProductoOffService productoOffService;
    private final OpenFoodFactsClient openFoodFactsClient;
    private final LimiteOpenFoodFacts limite;
    private final AlimentoMapper alimentoMapper;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventos;

    public CodigoBarrasService(IAlimentoRepository alimentoRepository, IProductoOffRepository productoOffRepository,
                               MaterializadorProducto materializador, ProductoOffService productoOffService,
                               OpenFoodFactsClient openFoodFactsClient, LimiteOpenFoodFacts limite,
                               AlimentoMapper alimentoMapper, SecurityUtils securityUtils,
                               ApplicationEventPublisher eventos) {
        this.alimentoRepository = alimentoRepository;
        this.productoOffRepository = productoOffRepository;
        this.materializador = materializador;
        this.productoOffService = productoOffService;
        this.openFoodFactsClient = openFoodFactsClient;
        this.limite = limite;
        this.alimentoMapper = alimentoMapper;
        this.securityUtils = securityUtils;
        this.eventos = eventos;
    }

    /**
     * El alimento de ese código para quien escanea: el suyo si lo tiene; si no, el del
     * catálogo, materializándolo si hace falta.
     *
     * @param codigo código de barras, solo cifras.
     * @return el alimento.
     * @throws NotFoundEntityException     (404) si no existe en ninguna parte.
     * @throws ServicioSaturadoException   (503) si hacía falta preguntar a Open Food Facts y no queda cupo.
     */
    @Transactional
    public AlimentoDTO porCodigo(String codigo) {
        String limpio = limpiar(codigo);
        Optional<Alimento> mio = alimentoRepository
                .findFirstByBarcodeAndUsuarioIdAndActivoTrueOrderByIdAsc(limpio, securityUtils.getCurrentUserId());
        if (mio.isPresent()) return alimentoMapper.toDTO(mio.get());
        return delCatalogo(limpio);
    }

    /**
     * Materializa en el catálogo el producto de ese código y lo devuelve (POST
     * /alimentos/importar). No mira los alimentos propios: lo que se importa es catálogo.
     *
     * @param codigo código de barras, solo cifras.
     * @return el alimento del catálogo con ese código.
     */
    @Transactional
    public AlimentoDTO importar(String codigo) {
        return delCatalogo(limpiar(codigo));
    }

    private AlimentoDTO delCatalogo(String codigo) {
        Optional<Alimento> catalogo = alimentoRepository.findFirstByBarcodeAndUsuarioIsNullOrderByIdAsc(codigo);
        if (catalogo.isPresent()) {
            Alimento alimento = catalogo.get();
            // Si un ADMIN lo desactivó y alguien lo vuelve a elegir, vuelve.
            if (!Boolean.TRUE.equals(alimento.getActivo())) {
                alimento.setActivo(true);
                alimentoRepository.save(alimento);
                eventos.publishEvent(new IndiceAlimentos.CatalogoCambiado());
            }
            return alimentoMapper.toDTO(alimento);
        }

        Optional<ProductoOff> producto = productoOffRepository.findByCodigo(codigo);
        if (producto.isPresent()) return materializar(producto.get());

        return deOpenFoodFacts(codigo);
    }

    private AlimentoDTO deOpenFoodFacts(String codigo) {
        if (limite.esDesconocido(codigo)) {
            throw new NotFoundEntityException("error.openfoodfacts.noExiste", codigo);
        }
        long espera = limite.pedirLectura();
        if (espera > 0) {
            logger.warn("Sin cupo para leer {} de Open Food Facts; reintentar en {} s", codigo, espera);
            throw new ServicioSaturadoException(espera, "error.openfoodfacts.saturado");
        }
        Optional<ProductoOffImportDTO> valido = openFoodFactsClient.porBarcode(codigo)
                .flatMap(ProductoOffValidacion::limpiar);
        if (valido.isEmpty()) {
            // Ni lo tiene, ni lo que tiene sirve (sin nombre o sin los cuatro valores):
            // para quien escanea es lo mismo, y la app le ofrece crearlo.
            limite.apuntarDesconocido(codigo);
            throw new NotFoundEntityException("error.openfoodfacts.noExiste", codigo);
        }
        ProductoOffImportDTO p = valido.get();
        // Se guarda con el código que se ha pedido: es el que tiene que encontrar la
        // próxima vez, aunque Open Food Facts lo escriba de otra forma.
        p.setCodigo(codigo);
        productoOffService.guardarUno(p);
        ProductoOff guardado = productoOffRepository.findByCodigo(codigo)
                .orElseThrow(() -> new NotFoundEntityException("error.openfoodfacts.noExiste", codigo));
        return materializar(guardado);
    }

    private AlimentoDTO materializar(ProductoOff producto) {
        Alimento alimento = materializador.materializar(producto);
        eventos.publishEvent(new IndiceAlimentos.CatalogoCambiado());
        return alimentoMapper.toDTO(alimento);
    }

    // Solo cifras, de 1 a 32: lo demás es un 400, no una pregunta a Open Food Facts.
    private static String limpiar(String codigo) {
        String limpio = codigo == null ? "" : codigo.trim();
        if (limpio.isEmpty() || limpio.length() > 32 || !limpio.chars().allMatch(Character::isDigit)) {
            throw new InvalidDataException("error.barcode.invalido");
        }
        return limpio;
    }
}
