package com.gymprofit.api.service.busqueda;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.common.PageDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.dto.entity.alimento.RacionDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.ProductoOff;
import com.gymprofit.api.service.productooff.RacionesProducto;
import com.gymprofit.api.mappers.AlimentoMapper;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IProductoOffRepository;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ============================================================
// BusquedaAlimentosService — GET /alimentos/buscar, sin salir de casa (GP-162, DEC-040)
//
// Busca solo en lo nuestro; ninguna búsqueda por texto llega a Open Food Facts. Los
// resultados van en tres grupos y en este orden, cada uno con su `grupo`:
//   1. TUYO: tus alimentos y los que has apuntado en los últimos 60 días.
//   2. BASICO: los básicos (y el resto del catálogo hecho a mano).
//   3. PRODUCTO: los productos de productos_off y los ya materializados. Uno sin
//      materializar va con id nulo y su código, como iban los de Open Food Facts: la app
//      repartida ya los importa al elegirlos.
// Sin texto: lo tuyo por uso reciente y después la lista fija de básicos habituales.
//
// Lo de otro usuario nunca sale (DEC-027): TUYO se lee filtrando por el usuario del
// token y los otros dos grupos son catálogo, sin dueño.
//
// La paginación recorre la misma lista ordenada en cada página, con desempates por id:
// no hay repetidos entre páginas mientras no cambien los datos.
// ============================================================
@Service
@Transactional(readOnly = true)
public class BusquedaAlimentosService {

    public static final String TUYO = "TUYO";
    public static final String BASICO = "BASICO";
    public static final String PRODUCTO = "PRODUCTO";

    static final int DIAS_RECIENTES = 60;

    private final IndiceAlimentos indice;
    private final JdbcTemplate jdbc;
    private final IAlimentoRepository alimentoRepository;
    private final IProductoOffRepository productoOffRepository;
    private final AlimentoMapper alimentoMapper;
    private final SecurityUtils securityUtils;

    public BusquedaAlimentosService(IndiceAlimentos indice, JdbcTemplate jdbc, IAlimentoRepository alimentoRepository,
                                    IProductoOffRepository productoOffRepository, AlimentoMapper alimentoMapper,
                                    SecurityUtils securityUtils) {
        this.indice = indice;
        this.jdbc = jdbc;
        this.alimentoRepository = alimentoRepository;
        this.productoOffRepository = productoOffRepository;
        this.alimentoMapper = alimentoMapper;
        this.securityUtils = securityUtils;
    }

    /** Un resultado sin pintar: un alimento (id) o un producto de productos_off (id de producto). */
    private record Ref(String grupo, boolean esProducto, int id) {
    }

    /** Un alimento del usuario: suyo o apuntado hace poco. */
    private record Tuyo(int id, String nombre, String nombreEn, String categoria, String marca,
                        LocalDateTime ultimoUso) {
    }

    /**
     * Página de resultados.
     *
     * @param q         texto; vacío o null, sin texto.
     * @param categoria una de AlimentoController.CATEGORIAS; null, todas. Con categoría no
     *                  salen productos, que no tienen.
     * @param page      página, desde 0.
     * @param size      tamaño de página, de 1 a 100.
     */
    public PageDTO<AlimentoDTO> buscar(String q, String categoria, int page, int size) {
        int pagina = Math.max(0, page);
        int tam = Math.min(Math.max(1, size), 100);
        String cat = categoria == null || categoria.isBlank() ? null : categoria.trim();
        List<String> consulta = Normalizador.terminos(q);
        Integer usuarioId = securityUtils.getCurrentUserId();

        List<Tuyo> tuyos = cargarTuyos(usuarioId).stream()
                .filter(t -> cat == null || cat.equals(t.categoria()))
                .toList();
        List<Ref> refs = new ArrayList<>();
        Set<Integer> idsTuyos = new HashSet<>();
        // Productos que casan pero no hace falta pintar: cuentan en el total, nada más.
        int sinPintar = 0;

        IndiceAlimentos.Catalogo catalogo = indice.catalogo();
        if (consulta.isEmpty()) {
            tuyos.stream()
                    .sorted(Comparator.comparing(Tuyo::ultimoUso, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Tuyo::id, Comparator.reverseOrder()))
                    .forEach(t -> {
                        refs.add(new Ref(TUYO, false, t.id()));
                        idsTuyos.add(t.id());
                    });
            sinTextoCatalogo(catalogo, cat, idsTuyos, refs);
        } else {
            for (Tuyo t : ordenarTuyos(tuyos, consulta)) {
                refs.add(new Ref(TUYO, false, t.id()));
                idsTuyos.add(t.id());
            }
            sinPintar = conTextoCatalogoYProductos(catalogo, consulta, cat, idsTuyos, refs, (pagina + 1) * tam);
        }

        int total = refs.size() + sinPintar;
        int desde = Math.min(refs.size(), pagina * tam);
        int hasta = Math.min(refs.size(), desde + tam);
        List<AlimentoDTO> contenido = pintar(refs.subList(desde, hasta), catalogo);
        int totalPaginas = Math.max(1, (int) Math.ceil((double) total / tam));
        return new PageDTO<>(contenido, pagina, tam, total, totalPaginas, pagina * tam + tam >= total);
    }

    // --- Sin texto ----------------------------------------------------------

    private void sinTextoCatalogo(IndiceAlimentos.Catalogo catalogo, String cat, Set<Integer> idsTuyos,
                                  List<Ref> refs) {
        if (cat == null) {
            for (Integer id : indice.idsHabituales()) {
                if (!idsTuyos.contains(id)) refs.add(new Ref(BASICO, false, id));
            }
            return;
        }
        // Con categoría: todo el catálogo de esa categoría, los habituales primero.
        catalogo.docs().stream()
                .filter(d -> cat.equals(d.categoria()) && !idsTuyos.contains(d.id()))
                .sorted(Comparator.comparing((IndiceAlimentos.DocCatalogo d) -> d.esProducto())
                        .thenComparing(d -> d.posicionHabitual() < 0)
                        .thenComparingInt(IndiceAlimentos.DocCatalogo::posicionHabitual)
                        .thenComparing(IndiceAlimentos.DocCatalogo::nombre, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(IndiceAlimentos.DocCatalogo::id))
                .forEach(d -> refs.add(new Ref(d.esProducto() ? PRODUCTO : BASICO, false, d.id())));
    }

    // --- Con texto ----------------------------------------------------------

    /**
     * Nivel de coincidencia, menor es mejor: 0, el nombre es exactamente lo buscado;
     * 1, todos los términos exactos y el nombre empieza por el primero; 2, todos exactos;
     * 3, empieza por el primero con prefijo o errata; 4, el resto.
     */
    private static int nivel(List<String> consulta, List<String> nombre, boolean exacto, boolean empieza) {
        if (exacto && new HashSet<>(nombre).equals(new HashSet<>(consulta))) return 0;
        if (exacto && empieza) return 1;
        if (exacto) return 2;
        return empieza ? 3 : 4;
    }

    private List<Tuyo> ordenarTuyos(List<Tuyo> tuyos, List<String> consulta) {
        if (tuyos.isEmpty()) return List.of();
        IndiceTexto.Constructor constructor = new IndiceTexto.Constructor();
        List<List<String>> nombres = new ArrayList<>();
        for (Tuyo t : tuyos) {
            List<String> es = Normalizador.terminos(t.nombre());
            List<String> todos = new ArrayList<>(es);
            todos.addAll(Normalizador.terminos(t.nombreEn()));
            todos.addAll(Normalizador.terminos(t.marca()));
            constructor.anadir(todos);
            nombres.add(es);
        }
        IndiceTexto texto = constructor.construir();
        IndiceTexto.Coincidencias c = texto.buscar(consulta);
        Set<Integer> primeros = posicionesPrimerTermino(texto, consulta);
        Map<Integer, Integer> niveles = new HashMap<>();
        for (int d = c.todos().nextSetBit(0); d >= 0; d = c.todos().nextSetBit(d + 1)) {
            List<String> nombre = nombres.get(d);
            boolean empieza = !nombre.isEmpty() && primeros.contains(texto.posicion(nombre.get(0)));
            niveles.put(d, nivel(consulta, nombre, c.exactos().get(d), empieza));
        }
        List<Integer> docs = new ArrayList<>(niveles.keySet());
        docs.sort(Comparator.comparing((Integer d) -> niveles.get(d))
                .thenComparing(d -> tuyos.get(d).ultimoUso(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparingInt(d -> tuyos.get(d).nombre().length())
                .thenComparingInt(d -> tuyos.get(d).id()));
        return docs.stream().map(tuyos::get).toList();
    }

    /**
     * Añade a {@code refs} los básicos y los productos que casan, ordenados, pero solo
     * hasta {@code hasta} resultados en total: de una búsqueda como «aceite» casan miles de
     * productos y solo se pinta una página.
     *
     * @return cuántos productos casan y no se han añadido (cuentan en el total).
     */
    private int conTextoCatalogoYProductos(IndiceAlimentos.Catalogo catalogo, List<String> consulta, String cat,
                                           Set<Integer> idsTuyos, List<Ref> refs, int hasta) {
        IndiceAlimentos.Productos productos = cat == null ? indice.productos() : null;

        // Catálogo: básicos aquí; los productos ya materializados, con los productos.
        IndiceTexto.Coincidencias c = catalogo.texto().buscar(consulta);
        Set<Integer> primerosCatalogo = posicionesPrimerTermino(catalogo.texto(), consulta);
        record Candidato(int nivel, int habitual, long escaneos, int longitud, Ref ref) {
        }
        List<Candidato> basicos = new ArrayList<>();
        List<Candidato> deProductos = new ArrayList<>();
        BitSet todos = c.todos();
        for (int d = todos.nextSetBit(0); d >= 0; d = todos.nextSetBit(d + 1)) {
            IndiceAlimentos.DocCatalogo doc = catalogo.docs().get(d);
            if (idsTuyos.contains(doc.id()) || (cat != null && !cat.equals(doc.categoria()))) continue;
            int nivelEs = nivelDoc(catalogo.texto(), consulta, doc.terminosEs(), c.exactos().get(d), primerosCatalogo);
            int nivelEn = doc.terminosEn().isEmpty() ? 4
                    : nivelDoc(catalogo.texto(), consulta, doc.terminosEn(), c.exactos().get(d), primerosCatalogo);
            int nivel = Math.min(nivelEs, nivelEn);
            if (doc.esProducto()) {
                // Si está también en productos_off, sale una sola vez, desde allí.
                if (productos != null && productos.contieneCodigo(IndiceAlimentos.codigoNumerico(doc.barcode()))) {
                    continue;
                }
                if (cat == null) {
                    deProductos.add(new Candidato(nivel, 1, 0, doc.nombre().length(), new Ref(PRODUCTO, false, doc.id())));
                }
            } else {
                basicos.add(new Candidato(nivel, doc.posicionHabitual() >= 0 ? 0 : 1, 0, doc.nombre().length(),
                        new Ref(BASICO, false, doc.id())));
            }
        }
        // El nombre exacto, primero; después, entre los que casan con todo exacto, los
        // habituales antes que los que solo empiezan por la palabra («pollo» da antes la
        // pechuga que el pollo asado); y al final, prefijos y erratas.
        basicos.sort(Comparator.comparingInt((Candidato x) -> x.nivel() == 0 ? 0 : 1)
                .thenComparingInt(x -> x.nivel() <= 2 ? 0 : 1)
                .thenComparingInt(Candidato::habitual)
                .thenComparingInt(Candidato::nivel)
                .thenComparingInt(Candidato::longitud).thenComparingInt(x -> x.ref().id()));
        basicos.forEach(b -> refs.add(b.ref()));

        int falta = Math.max(0, hasta - refs.size());
        int casan = deProductos.size();
        if (productos != null) {
            // Con miles de coincidencias, un comparador sobre objetos se nota en Render (0,1
            // de CPU). Cada producto se resume en un número cuyo orden es el de la lista
            // (nivel, más escaneados, nombre más corto y, al final, el orden del índice, que
            // es el de su id), se ordenan los números y solo se convierten los que hacen falta.
            IndiceTexto.Coincidencias p = productos.texto().buscar(consulta);
            Set<Integer> primeros = posicionesPrimerTermino(productos.texto(), consulta);
            BitSet encontrados = p.todos();
            long[] claves = new long[encontrados.cardinality()];
            int n = 0;
            boolean mirarTuyos = !idsTuyos.isEmpty();
            for (int d = encontrados.nextSetBit(0); d >= 0; d = encontrados.nextSetBit(d + 1)) {
                if (mirarTuyos) {
                    Integer materializado = catalogo.idPorCodigo().get(productos.codigos()[d]);
                    if (materializado != null && idsTuyos.contains(materializado)) continue;
                }
                boolean exacto = p.exactos().get(d);
                boolean empieza = primeros.contains(productos.primerTermino()[d]);
                int nivel = exacto ? (empieza ? 1 : 2) : (empieza ? 3 : 4);
                claves[n++] = clave(nivel, productos.escaneos()[d], productos.longitud()[d], d);
            }
            Arrays.sort(claves, 0, n);
            casan += n;
            for (int i = 0; i < Math.min(n, falta); i++) {
                int d = (int) (claves[i] & MASCARA_DOC);
                int nivel = (int) (claves[i] >>> BIT_NIVEL);
                deProductos.add(new Candidato(nivel, 1, productos.escaneos()[d], productos.longitud()[d],
                        new Ref(PRODUCTO, true, productos.ids()[d])));
            }
        }
        deProductos.sort(Comparator.comparingInt(Candidato::nivel)
                .thenComparing(Candidato::escaneos, Comparator.reverseOrder())
                .thenComparingInt(Candidato::longitud)
                .thenComparing(x -> x.ref().esProducto())
                .thenComparingInt(x -> x.ref().id()));
        int anadidos = Math.min(falta, deProductos.size());
        deProductos.subList(0, anadidos).forEach(x -> refs.add(x.ref()));
        return casan - anadidos;
    }

    // Clave de orden de un producto: 3 bits de nivel, 24 de escaneos al revés, 10 de
    // longitud del nombre y 20 de posición en el índice (hasta 1 048 575 productos).
    private static final int BIT_NIVEL = 54;
    private static final long MAX_ESCANEOS = (1L << 24) - 1;
    private static final long MASCARA_DOC = (1L << 20) - 1;

    static long clave(int nivel, int escaneos, int longitud, int doc) {
        long inverso = MAX_ESCANEOS - Math.min(Math.max(escaneos, 0), MAX_ESCANEOS);
        return ((long) nivel << BIT_NIVEL) | (inverso << 30) | ((long) Math.min(longitud, 1023) << 20)
                | (doc & MASCARA_DOC);
    }

    private static int nivelDoc(IndiceTexto texto, List<String> consulta, List<String> nombre, boolean exacto,
                                Set<Integer> primeros) {
        boolean empieza = !nombre.isEmpty() && primeros.contains(texto.posicion(nombre.get(0)));
        return nivel(consulta, nombre, exacto, empieza);
    }

    // Posiciones del vocabulario con las que casa el primer término de la consulta.
    private static Set<Integer> posicionesPrimerTermino(IndiceTexto texto, List<String> consulta) {
        return new HashSet<>(texto.candidatos(consulta.get(0), consulta.size() == 1));
    }

    // --- Lo tuyo ------------------------------------------------------------

    // Tus alimentos y los que has apuntado en los últimos 60 días (solo los que puedes
    // ver: catálogo o tuyos), en una sola consulta: cada viaje a Aiven son ~14 ms (GP-168).
    // Las dos mitades van por índice de usuario; un alimento tuyo apuntado hace poco sale
    // en las dos, y manda la primera, que lleva su último uso de siempre.
    private List<Tuyo> cargarTuyos(Integer usuarioId) {
        Map<Integer, Tuyo> porId = new LinkedHashMap<>();
        Timestamp desde = Timestamp.valueOf(LocalDateTime.now().minusDays(DIAS_RECIENTES));
        jdbc.query("""
                SELECT 0 AS parte, a.id, a.nombre, a.nombre_en, a.categoria, a.marca,
                       (SELECT MAX(c.fecha) FROM alimentos_comida ac JOIN comidas c ON c.id = ac.comida_id
                        WHERE ac.alimento_id = a.id AND c.usuario_id = ?) AS ultimo
                FROM alimentos a WHERE a.usuario_id = ? AND a.activo = 1
                UNION ALL
                SELECT 1 AS parte, a.id, a.nombre, a.nombre_en, a.categoria, a.marca, MAX(c.fecha) AS ultimo
                FROM comidas c
                JOIN alimentos_comida ac ON ac.comida_id = c.id
                JOIN alimentos a ON a.id = ac.alimento_id
                WHERE c.usuario_id = ? AND c.fecha >= ? AND a.activo = 1
                  AND (a.usuario_id IS NULL OR a.usuario_id = ?)
                GROUP BY a.id, a.nombre, a.nombre_en, a.categoria, a.marca
                ORDER BY parte""", rs -> {
            porId.putIfAbsent(rs.getInt("id"), new Tuyo(rs.getInt("id"), rs.getString("nombre"),
                    rs.getString("nombre_en"), rs.getString("categoria"), rs.getString("marca"),
                    fecha(rs.getTimestamp("ultimo"))));
        }, usuarioId, usuarioId, usuarioId, desde, usuarioId);
        return new ArrayList<>(porId.values());
    }

    private static LocalDateTime fecha(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }

    // --- Pintar la página ---------------------------------------------------

    private List<AlimentoDTO> pintar(List<Ref> pagina, IndiceAlimentos.Catalogo catalogo) {
        List<Integer> idsProductos = pagina.stream().filter(Ref::esProducto).map(Ref::id).toList();
        Map<Integer, ProductoOff> productos = new HashMap<>();
        productoOffRepository.findAllById(idsProductos).forEach(p -> productos.put(p.getId(), p));

        // Un producto ya materializado se pinta con su alimento, que lleva id.
        Map<Integer, Integer> alimentoDeProducto = new HashMap<>();
        for (ProductoOff p : productos.values()) {
            Integer id = catalogo.idPorCodigo().get(IndiceAlimentos.codigoNumerico(p.getCodigo()));
            if (id != null) alimentoDeProducto.put(p.getId(), id);
        }
        Set<Integer> idsAlimentos = new HashSet<>(alimentoDeProducto.values());
        pagina.stream().filter(r -> !r.esProducto()).forEach(r -> idsAlimentos.add(r.id()));
        Map<Integer, Alimento> alimentos = new HashMap<>();
        // Los alimentos con sus raciones, en una consulta (antes, dos).
        if (!idsAlimentos.isEmpty()) {
            alimentoRepository.conRaciones(idsAlimentos).forEach(a -> alimentos.put(a.getId(), a));
        }

        boolean ingles = "en".equals(LocaleContextHolder.getLocale().getLanguage());
        List<AlimentoDTO> resultado = new ArrayList<>();
        for (Ref ref : pagina) {
            AlimentoDTO dto = null;
            if (!ref.esProducto() || alimentoDeProducto.containsKey(ref.id())) {
                Alimento a = alimentos.get(ref.esProducto() ? alimentoDeProducto.get(ref.id()) : ref.id());
                if (a != null) dto = alimentoMapper.toDTO(a);
            } else {
                ProductoOff p = productos.get(ref.id());
                if (p != null) dto = productoADto(p, ingles);
            }
            // Si se borró entre que se indexó y ahora, no sale.
            if (dto == null) continue;
            dto.setGrupo(ref.grupo());
            resultado.add(dto);
        }
        return resultado;
    }

    /** Un producto sin materializar, como lo espera la app: id nulo y el código para importarlo. */
    static AlimentoDTO productoADto(ProductoOff p, boolean ingles) {
        AlimentoDTO dto = new AlimentoDTO();
        dto.setNombre(p.getNombre().length() <= 100 ? p.getNombre() : p.getNombre().substring(0, 100));
        dto.setMarca(p.getMarca());
        dto.setBarcode(p.getCodigo());
        dto.setCalorias(p.getKcal().setScale(0, RoundingMode.HALF_UP).intValue());
        dto.setProteinas(p.getProteinas());
        dto.setCarbohidratos(p.getCarbohidratos());
        dto.setGrasas(p.getGrasas());
        dto.setFibra(p.getFibra());
        dto.setPorcionGramos(100);
        dto.setActivo(true);
        dto.setFuente("OFF");
        dto.setRevisado(false);
        // Las mismas que tendrá al materializarse (RacionesProducto), aún sin id.
        dto.setRaciones(RacionesProducto.de(p).stream()
                .map(r -> RacionDTO.de(null, ingles ? r.nombreEn() : r.nombre(), r.gramos(), ingles, null))
                .toList());
        return dto;
    }
}
