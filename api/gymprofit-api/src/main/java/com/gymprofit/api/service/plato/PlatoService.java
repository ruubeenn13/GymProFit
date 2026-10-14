package com.gymprofit.api.service.plato;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.dto.entity.plato.IngredienteDTO;
import com.gymprofit.api.dto.entity.plato.IngredientePedidoDTO;
import com.gymprofit.api.dto.entity.plato.LoMioDTO;
import com.gymprofit.api.dto.entity.plato.PlatoDTO;
import com.gymprofit.api.dto.entity.plato.PlatoPedidoDTO;
import com.gymprofit.api.dto.entity.plato.ValoresDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoComida;
import com.gymprofit.api.entity.AlimentoRacion;
import com.gymprofit.api.entity.PlatoIngrediente;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.UnauthorizedException;
import com.gymprofit.api.mappers.AlimentoMapper;
import com.gymprofit.api.repository.jpa.IAlimentoRacionRepository;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IPlatoIngredienteRepository;
import com.gymprofit.api.service.alimentocomida.AlimentosConUltima;
import com.gymprofit.api.service.alimentocomida.IAlimentoComidaService;
import com.gymprofit.api.service.alimentocomida.RacionVigente;
import com.gymprofit.api.service.alimentocomida.ValoresLinea;
import com.gymprofit.api.service.busqueda.UnidadesRacion;
import com.gymprofit.api.service.codigo.CodigoBarrasService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ============================================================
// PlatoService — los platos propios y lo de la pestaña «Mis platos» (lote 1.6.6)
//
// Un plato es un alimento de la cuenta con ingredientes (DEC-047). Sus valores salen de
// sus ingredientes al guardarlo (Platos) y se quedan así hasta que se guarde otra vez,
// aunque después cambie un ingrediente. El dueño sale del token (DEC-013); el plato de
// otra cuenta y el que no existe dan el mismo 403 (DEC-014), también al ADMIN, que no ve
// los alimentos de nadie (DEC-027). Un ingrediente de otra cuenta o que no existe, el
// mismo 403; un plato como ingrediente, 400.
// ============================================================
@Service
public class PlatoService {

    /** Lo que puede pesar un plato hecho: 50 kg. */
    static final BigDecimal PESO_MAXIMO = new BigDecimal("50000");
    /** Lo que cabe en la cantidad de un ingrediente (DECIMAL(6,2)). */
    static final BigDecimal GRAMOS_MAXIMOS = new BigDecimal("9999.99");

    private final IAlimentoRepository alimentoRepository;
    private final IAlimentoRacionRepository racionRepository;
    private final IPlatoIngredienteRepository ingredienteRepository;
    private final IAlimentoComidaService alimentoComidaService;
    private final CodigoBarrasService codigoBarrasService;
    private final AlimentosConUltima alimentosConUltima;
    private final AlimentoMapper alimentoMapper;
    private final SecurityUtils securityUtils;
    private final JdbcTemplate jdbc;

    public PlatoService(IAlimentoRepository alimentoRepository, IAlimentoRacionRepository racionRepository,
                        IPlatoIngredienteRepository ingredienteRepository, IAlimentoComidaService alimentoComidaService,
                        CodigoBarrasService codigoBarrasService, AlimentosConUltima alimentosConUltima,
                        AlimentoMapper alimentoMapper, SecurityUtils securityUtils, JdbcTemplate jdbc) {
        this.alimentoRepository = alimentoRepository;
        this.racionRepository = racionRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.alimentoComidaService = alimentoComidaService;
        this.codigoBarrasService = codigoBarrasService;
        this.alimentosConUltima = alimentosConUltima;
        this.alimentoMapper = alimentoMapper;
        this.securityUtils = securityUtils;
        this.jdbc = jdbc;
    }

    /**
     * Crea un plato de la cuenta del token.
     *
     * @throws InvalidDataException  (400) si el peso no cuadra, sale más de 900 kcal por 100 g
     *                               o un ingrediente es un plato o tiene mal la cantidad.
     * @throws UnauthorizedException (403) si un ingrediente es de otra cuenta o no existe.
     */
    @Transactional
    public PlatoDTO crear(PlatoPedidoDTO pedido) {
        Alimento plato = new Alimento();
        plato.setUsuario(securityUtils.getCurrentUser());
        return guardar(plato, pedido);
    }

    /** El plato con sus ingredientes; uno borrado también, si es suyo. */
    @Transactional(readOnly = true)
    public PlatoDTO leer(Integer id) {
        return aDto(propio(id, false));
    }

    /** Cambia el plato: sustituye sus ingredientes y recalcula sus valores. */
    @Transactional
    public PlatoDTO cambiar(Integer id, PlatoPedidoDTO pedido) {
        return guardar(propio(id, true), pedido);
    }

    /**
     * Borra el plato: lo desactiva, como un alimento propio. Deja de salir en listas y
     * búsquedas; las líneas que lo usan siguen igual. Repetible.
     */
    @Transactional
    public void borrar(Integer id) {
        Alimento plato = propio(id, false);
        plato.setActivo(false);
        alimentoRepository.save(plato);
    }

    /**
     * Lo de la pestaña: los platos y los alimentos creados, activos, cada uno con sus
     * raciones, favorito y última cantidad. Del usado más recientemente al menos; los
     * nunca usados, los más nuevos primero. Cuatro consultas, haya los que haya.
     */
    @Transactional(readOnly = true)
    public LoMioDTO loMio() {
        Integer yo = securityUtils.getCurrentUserId();
        record Uso(int id, boolean plato, Timestamp ultimo, long ultimaLinea) {
        }
        List<Uso> usos = jdbc.query("""
                SELECT a.id, a.plato_raciones, MAX(c.fecha) AS ultimo, MAX(ac.id) AS ultima_linea
                FROM alimentos a
                LEFT JOIN (alimentos_comida ac JOIN comidas c ON c.id = ac.comida_id AND c.usuario_id = ?)
                       ON ac.alimento_id = a.id
                WHERE a.usuario_id = ? AND a.activo = 1
                GROUP BY a.id, a.plato_raciones""",
                (rs, i) -> new Uso(rs.getInt("id"), rs.getObject("plato_raciones") != null,
                        rs.getTimestamp("ultimo"), rs.getLong("ultima_linea")),
                yo, yo);
        Comparator<Uso> orden = Comparator
                .comparing((Uso u) -> u.ultimo() == null)
                .thenComparing(Uso::ultimo, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Comparator.comparingLong(Uso::ultimaLinea).reversed())
                .thenComparing(Comparator.comparingInt(Uso::id).reversed());
        List<Uso> ordenados = usos.stream().sorted(orden).toList();
        List<Integer> ids = ordenados.stream().map(Uso::id).toList();
        Map<Integer, AlimentoDTO> dtos = alimentosConUltima.pintar(ids, yo, null);
        Set<Integer> favoritos = alimentosConUltima.favoritos(ids, yo);
        List<AlimentoDTO> platos = new ArrayList<>();
        List<AlimentoDTO> alimentos = new ArrayList<>();
        for (Uso u : ordenados) {
            AlimentoDTO dto = dtos.get(u.id());
            if (dto == null) continue;
            dto.setFavorito(favoritos.contains(u.id()));
            (u.plato() ? platos : alimentos).add(dto);
        }
        return new LoMioDTO(platos, alimentos);
    }

    // --- Guardar --------------------------------------------------------------

    private PlatoDTO guardar(Alimento plato, PlatoPedidoDTO pedido) {
        int raciones = pedido.getRaciones();
        BigDecimal peso = pedido.getPesoGramos();
        if (peso != null && (peso.signum() <= 0 || peso.compareTo(PESO_MAXIMO) > 0
                || peso.compareTo(BigDecimal.valueOf(raciones)) < 0)) {
            throw new InvalidDataException("error.plato.peso");
        }

        // Los ingredientes, comprobados antes de escribir nada.
        List<PlatoIngrediente> ingredientes = new ArrayList<>();
        ValoresLinea total = ValoresLinea.CERO;
        for (IngredientePedidoDTO p : pedido.getIngredientes()) {
            PlatoIngrediente ing = ingrediente(p);
            ing.setOrden(ingredientes.size() + 1);
            ingredientes.add(ing);
            total = total.mas(ValoresLinea.de(ing.getAlimento(), ing.getCantidadGramos()));
        }

        Platos.Valores valores = Platos.aGuardar(total, raciones, peso);
        if (peso != null && valores.kcal() > Platos.KCAL_MAXIMAS_POR_100) {
            throw new InvalidDataException("error.plato.densidad", Platos.KCAL_MAXIMAS_POR_100);
        }
        if (!Platos.caben(valores)) {
            throw new InvalidDataException("error.plato.demasiado");
        }

        plato.setNombre(pedido.getNombre().trim());
        plato.setCalorias(valores.kcal());
        plato.setProteinas(valores.proteinas());
        plato.setCarbohidratos(valores.carbohidratos());
        plato.setGrasas(valores.grasas());
        plato.setFibra(valores.fibra());
        plato.setPorcionGramos(100);
        plato.setPlatoRaciones(raciones);
        plato.setPlatoPesoGramos(peso == null ? null : peso.setScale(1, java.math.RoundingMode.HALF_UP));
        plato.setActivo(true);
        plato.setRevisado(false);
        Alimento guardado = alimentoRepository.save(plato);
        ponerRaciones(guardado, raciones, guardado.getPlatoPesoGramos());

        if (guardado.getId() != null) ingredienteRepository.borrarDePlato(guardado.getId());
        for (PlatoIngrediente ing : ingredientes) {
            ing.setPlato(guardado);
            ingredienteRepository.save(ing);
        }
        return aDto(guardado);
    }

    // La ración sin peso se crea una vez y no cambia nunca; la de con peso, en su misma
    // fila: una línea con la de antes se lee en gramos si ya no pesa lo mismo (GP-177).
    private void ponerRaciones(Alimento plato, int raciones, BigDecimal peso) {
        List<AlimentoRacion> actuales = plato.getRaciones();
        AlimentoRacion sinPeso = actuales.stream().filter(Platos::esRacionSinPeso).findFirst().orElse(null);
        if (sinPeso == null) {
            actuales.add(racionRepository.save(nueva(plato, Platos.GRAMOS_SIN_PESO, Platos.FUENTE_SIN_PESO, 1)));
        }
        if (peso == null) return;
        BigDecimal gramos = Platos.gramosRacion(peso, raciones);
        AlimentoRacion conPeso = actuales.stream()
                .filter(r -> Platos.FUENTE_CON_PESO.equals(r.getFuente())).findFirst().orElse(null);
        if (conPeso == null) {
            actuales.add(racionRepository.save(nueva(plato, gramos, Platos.FUENTE_CON_PESO, 2)));
        } else if (conPeso.getGramos().compareTo(gramos) != 0) {
            conPeso.setGramos(gramos);
            racionRepository.save(conPeso);
        }
    }

    private static AlimentoRacion nueva(Alimento plato, BigDecimal gramos, String fuente, int orden) {
        AlimentoRacion r = new AlimentoRacion();
        r.setAlimento(plato);
        r.setNombre("1 ración");
        r.setNombreEn("1 serving");
        r.setGramos(gramos);
        r.setFuente(fuente);
        r.setOrden(orden);
        return r;
    }

    // Un ingrediente pedido, con su alimento y su cantidad comprobados.
    private PlatoIngrediente ingrediente(IngredientePedidoDTO p) {
        boolean porId = p.getAlimentoId() != null;
        boolean porCodigo = p.getBarcode() != null && !p.getBarcode().isBlank();
        if (porId == porCodigo) throw new InvalidDataException("error.anadir.alimento");
        boolean conRacion = p.getRacionId() != null || p.getRacionIndice() != null;
        if (p.getRacionId() != null && p.getRacionIndice() != null) throw new InvalidDataException("error.racion.incompleta");
        if (!conRacion && p.getRaciones() != null) throw new InvalidDataException("error.racion.incompleta");
        if (!conRacion && (p.getCantidadGramos() == null || p.getCantidadGramos().signum() <= 0)) {
            throw new InvalidDataException("error.anadir.cantidad");
        }

        Alimento alimento = porId ? visible(p.getAlimentoId()) : porCodigo(p.getBarcode().trim());
        if (Platos.esPlato(alimento)) throw new InvalidDataException("error.plato.platoComoIngrediente");

        // La cantidad, con la regla de una línea (ración de ese alimento, gramos que mandan).
        AlimentoComida cuenta = new AlimentoComida();
        cuenta.setAlimento(alimento);
        if (conRacion) {
            Integer racionId = p.getRacionId() != null ? p.getRacionId() : racionEn(alimento, p.getRacionIndice());
            alimentoComidaService.ponerCantidad(cuenta, p.getCantidadGramos(), racionId, p.getRaciones());
        } else {
            alimentoComidaService.ponerGramos(cuenta, p.getCantidadGramos());
        }
        if (cuenta.getCantidadGramos().compareTo(GRAMOS_MAXIMOS) > 0) throw new InvalidDataException("error.anadir.cantidad");

        PlatoIngrediente ing = new PlatoIngrediente();
        ing.setAlimento(alimento);
        ing.setCantidadGramos(cuenta.getCantidadGramos());
        ing.setRacion(cuenta.getRacion());
        ing.setRaciones(cuenta.getRaciones());
        return ing;
    }

    // Un alimento por su id que la cuenta ve: del catálogo o suyo. El de otra y el que no
    // existe, el mismo 403: no se dice si existe.
    private Alimento visible(Integer id) {
        Integer yo = securityUtils.getCurrentUserId();
        return alimentoRepository.findById(id)
                .filter(a -> a.getUsuario() == null || a.getUsuario().getId().equals(yo))
                .orElseThrow(() -> new UnauthorizedException("error.acceso.recurso"));
    }

    private Alimento porCodigo(String codigo) {
        AlimentoDTO dto = codigoBarrasService.porCodigo(codigo);
        return visible(dto.getId());
    }

    private static Integer racionEn(Alimento alimento, int indice) {
        List<AlimentoRacion> raciones = Platos.racionesVisibles(alimento);
        if (indice >= raciones.size()) throw new InvalidDataException("error.racion.otroAlimento", indice);
        return raciones.get(indice).getId();
    }

    // El plato de la cuenta del token; uno de otra, el que no existe y un alimento que no
    // es un plato dan el mismo 403. Para cambiarlo, además, activo.
    private Alimento propio(Integer id, boolean activo) {
        Integer yo = securityUtils.getCurrentUserId();
        return alimentoRepository.findById(id)
                .filter(Platos::esPlato)
                .filter(a -> a.getUsuario() != null && a.getUsuario().getId().equals(yo))
                .filter(a -> !activo || Boolean.TRUE.equals(a.getActivo()))
                .orElseThrow(() -> new UnauthorizedException("error.acceso.recurso"));
    }

    // --- Leer -----------------------------------------------------------------

    private PlatoDTO aDto(Alimento plato) {
        boolean ingles = "en".equals(LocaleContextHolder.getLocale().getLanguage());
        List<IngredienteDTO> ingredientes = new ArrayList<>();
        ValoresLinea total = ValoresLinea.CERO;
        for (PlatoIngrediente i : ingredienteRepository.dePlato(plato.getId())) {
            ValoresLinea v = ValoresLinea.de(i.getAlimento(), i.getCantidadGramos());
            total = total.mas(v);
            IngredienteDTO dto = new IngredienteDTO();
            dto.setAlimento(alimentoMapper.toDTO(i.getAlimento()));
            dto.setCantidadGramos(i.getCantidadGramos());
            AlimentoRacion r = i.getRacion();
            if (r != null && RacionVigente.cuadra(i.getCantidadGramos(), r.getGramos(), i.getRaciones())) {
                String nombre = ingles ? r.getNombreEn() : r.getNombre();
                dto.setRacionId(r.getId());
                dto.setRacionNombre(nombre);
                dto.setRacionGramos(r.getGramos());
                dto.setRaciones(i.getRaciones());
                UnidadesRacion.de(nombre, ingles).ifPresent(u -> {
                    dto.setRacionUnidad(u.singular());
                    dto.setRacionUnidadPlural(u.plural());
                });
            }
            dto.setValores(valores(v, null));
            ingredientes.add(dto);
        }
        AlimentoRacion actual = Platos.racionActual(plato);
        ValoresDTO porRacion = actual == null ? null
                : valores(ValoresLinea.de(plato, actual.getGramos()), Platos.conPeso(plato) ? actual.getGramos() : null);
        return new PlatoDTO(plato.getId(), plato.getNombre(), plato.getPlatoRaciones(), plato.getPlatoPesoGramos(),
                plato.getActivo(), alimentoMapper.toDTO(plato), valores(total, null), porRacion, ingredientes);
    }

    private static ValoresDTO valores(ValoresLinea v, BigDecimal gramos) {
        return new ValoresDTO(v.kcal(), v.proteinas(), v.carbohidratos(), v.grasas(), v.fibra(), gramos);
    }
}
