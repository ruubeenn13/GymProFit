package com.gymprofit.api.service.productooff;

import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoRacion;
import com.gymprofit.api.entity.ProductoOff;
import com.gymprofit.api.repository.jpa.IAlimentoRacionRepository;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

// ============================================================
// MaterializadorProducto — convierte un producto en alimento del catálogo (GP-164)
//
// Un producto de productos_off no se puede poner en una comida: las comidas apuntan a
// `alimentos`. Al elegirlo (o al escanearlo) se crea su fila de catálogo, sin dueño
// (DEC-032), con fuente OFF y sin revisar. A partir de ahí es esa fila la que usan las
// comidas, y reimportar productos_off ya no la toca.
//
// Va en la transacción de quien llama. Si dos personas eligen el mismo producto en el
// mismo instante, la segunda choca con la clave única y recibe un error; al repetir, ya
// encuentra la fila. Es raro y no deja nada a medias, así que no compensa más.
// ============================================================
@Component
public class MaterializadorProducto {

    private final IAlimentoRepository alimentoRepository;
    private final IAlimentoRacionRepository racionRepository;

    public MaterializadorProducto(IAlimentoRepository alimentoRepository,
                                  IAlimentoRacionRepository racionRepository) {
        this.alimentoRepository = alimentoRepository;
        this.racionRepository = racionRepository;
    }

    /**
     * Crea la fila de catálogo del producto, con su ración si la declara.
     *
     * @param producto producto de productos_off.
     * @return el alimento creado.
     */
    @Transactional
    public Alimento materializar(ProductoOff producto) {
        Alimento alimento = new Alimento();
        alimento.setNombre(recortar(producto.getNombre(), 100));
        alimento.setMarca(producto.getMarca());
        alimento.setBarcode(producto.getCodigo());
        alimento.setCalorias(producto.getKcal().setScale(0, RoundingMode.HALF_UP).intValue());
        alimento.setProteinas(producto.getProteinas());
        alimento.setCarbohidratos(producto.getCarbohidratos());
        alimento.setGrasas(producto.getGrasas());
        alimento.setFibra(producto.getFibra());
        alimento.setPorcionGramos(100);
        alimento.setActivo(true);
        alimento.setFuente("OFF");
        alimento.setCodigoOrigen(producto.getCodigo());
        alimento.setRevisado(false);
        // usuario null: catálogo, no la comida de quien lo elige (DEC-032).
        Alimento guardado = alimentoRepository.saveAndFlush(alimento);

        BigDecimal gramos = producto.getRacionGramos();
        if (gramos != null && gramos.signum() > 0) {
            AlimentoRacion racion = new AlimentoRacion();
            racion.setAlimento(guardado);
            racion.setNombre("1 ración");
            racion.setNombreEn("1 serving");
            racion.setGramos(gramos);
            racion.setFuente(recortar("Open Food Facts: " + (producto.getRacionTexto() == null
                    ? gramos.stripTrailingZeros().toPlainString() + " g" : producto.getRacionTexto()), 255));
            racion.setOrden(1);
            // También en la lista de la entidad, para que la respuesta ya la lleve.
            guardado.getRaciones().add(racionRepository.save(racion));
        }
        return guardado;
    }

    private static String recortar(String texto, int maximo) {
        if (texto.length() <= maximo) return texto;
        int corte = Character.isHighSurrogate(texto.charAt(maximo - 1)) ? maximo - 1 : maximo;
        return texto.substring(0, corte);
    }
}
