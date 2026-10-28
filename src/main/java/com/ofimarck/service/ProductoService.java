package com.ofimarck.service;

import com.ofimarck.domain.Categoria;
import com.ofimarck.domain.Producto;
import com.ofimarck.dto.response.ProductoDTO;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.repository.CategoriaRepository;
import com.ofimarck.repository.ProductoRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación de {@link IProductoService} para la gestión del catálogo de productos.
 *
 * <p>Principio de Responsabilidad Única (S de SOLID): esta clase concentra
 * exclusivamente la lógica de negocio de productos (consulta, creación,
 * actualización y cambio de estado). No depende de la capa de presentación.
 *
 * <p>Principio de Inversión de Dependencias (D de SOLID): depende de
 * {@link ProductoRepository} y {@link CategoriaRepository} inyectados por
 * constructor.
 *
 * <p>Uso de Apache Commons Lang3 (rúbrica FASE 2):
 * <ul>
 *   <li>{@link StringUtils#normalizeSpace(String)}: elimina espacios en blanco
 *       excesivos y normaliza el texto ingresado por el usuario (ej. búsquedas
 *       de catálogo).</li>
 *   <li>{@link StringUtils#stripAccents(String)}: elimina tildes y diacríticos
 *       de nombres y apellidos, evitando problemas de codificación y
 *       facilitando búsquedas insensibles a acentos.</li>
 * </ul>
 *
 * <p>Uso de Logback/SLF4J (rúbrica FASE 2): registro de eventos de creación,
 * actualización y cambio de estado de productos.
 */
@Service
public class ProductoService implements IProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoService.class);

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(ProductoRepository productoRepository, CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public List<Producto> listarTodos(Integer categoriaId, Boolean estado, String termino) {
        List<Producto> lista;
        boolean tieneTermino = termino != null && !termino.isBlank();
        boolean tieneCategoria = categoriaId != null && categoriaId > 0;

        // Sanitizar término de búsqueda con Apache Commons antes de usarlo en la consulta
        String terminoLimpio = tieneTermino ? StringUtils.normalizeSpace(StringUtils.stripAccents(termino)) : null;

        if (tieneCategoria && tieneTermino) {
            lista = productoRepository.buscarPorCategoriaYTermino(categoriaId, terminoLimpio);
        } else if (tieneCategoria) {
            lista = productoRepository.findByCategoria_IdCategoriaOrderByFechaCreacionDesc(categoriaId);
        } else if (tieneTermino) {
            lista = productoRepository.buscarPorTermino(terminoLimpio);
        } else {
            lista = productoRepository.findByOrderByFechaCreacionDesc();
        }

        if (estado != null) {
            lista = lista.stream()
                    .filter(p -> p.getEstado().equals(estado))
                    .collect(Collectors.toList());
        }

        log.debug("productos_listados categoriaId={} estado={} termino={} resultados={}",
                categoriaId, estado, termino, lista.size());

        return lista;
    }

    @Override
    public List<Producto> listarAlertasStock() {
        List<Producto> alertas = productoRepository.findAlertasStock();
        log.info("alerta_stock_generada cantidadProductos={}", alertas.size());
        return alertas;
    }

    @Override
    public Producto buscarPorId(Integer id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
    }

    @Override
    public Producto buscarPorSku(String sku) {
        String skuLimpio = StringUtils.normalizeSpace(StringUtils.stripAccents(sku));
        return productoRepository.findBySku(skuLimpio)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con SKU: " + sku));
    }

    @Override
    @Transactional
    public Producto crear(ProductoDTO dto) {
        String skuLimpio = StringUtils.normalizeSpace(StringUtils.stripAccents(dto.getSku())).toUpperCase();
        if (productoRepository.existsBySkuIgnoreCase(skuLimpio)) {
            log.warn("producto_duplicado sku={} intentoCreacion", skuLimpio);
            throw new BadRequestException("Ya existe un producto con el SKU: " + skuLimpio);
        }
        Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Categoría no encontrada con ID: " + dto.getIdCategoria()));

        Producto p = new Producto();
        mapearDtoAEntidad(dto, p, categoria);
        Producto creado = productoRepository.save(p);

        log.info("producto_creado idProducto={} sku={} nombre={}", creado.getIdProducto(), creado.getSku(), creado.getNombre());

        return creado;
    }

    @Override
    @Transactional
    public Producto actualizar(Integer id, ProductoDTO dto) {
        Producto p = buscarPorId(id);
        String skuLimpio = StringUtils.normalizeSpace(StringUtils.stripAccents(dto.getSku())).toUpperCase();
        if (!p.getSku().equalsIgnoreCase(skuLimpio) &&
                productoRepository.existsBySkuIgnoreCase(skuLimpio)) {
            log.warn("producto_duplicado sku={} intentoActualizacion idProducto={}", skuLimpio, id);
            throw new BadRequestException("Ya existe otro producto con el SKU: " + skuLimpio);
        }
        Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Categoría no encontrada con ID: " + dto.getIdCategoria()));

        mapearDtoAEntidad(dto, p, categoria);
        Producto actualizado = productoRepository.save(p);

        log.info("producto_actualizado idProducto={} sku={}", actualizado.getIdProducto(), actualizado.getSku());

        return actualizado;
    }

    @Override
    @Transactional
    public Producto cambiarEstado(Integer id, Boolean nuevoEstado) {
        Producto p = buscarPorId(id);
        p.setEstado(nuevoEstado);
        Producto guardado = productoRepository.save(p);

        log.info("producto_estado_cambiado idProducto={} estado={}", id, nuevoEstado);

        return guardado;
    }

    private void mapearDtoAEntidad(ProductoDTO dto, Producto p, Categoria categoria) {
        p.setSku(StringUtils.normalizeSpace(StringUtils.stripAccents(dto.getSku())).toUpperCase());
        p.setNombre(StringUtils.normalizeSpace(StringUtils.stripAccents(dto.getNombre())));
        p.setDescripcion(dto.getDescripcion());
        p.setMaterial(dto.getMaterial());
        p.setDimensiones(dto.getDimensiones());
        p.setPrecio(dto.getPrecio());
        p.setStock(dto.getStock());
        p.setStockMinimo(dto.getStockMinimo());
        p.setCategoria(categoria);
        if (dto.getUrlImagen() != null && !dto.getUrlImagen().isBlank()) {
            p.setUrlImagen(dto.getUrlImagen().trim());
        }
        if (dto.getEstado() != null) {
            p.setEstado(dto.getEstado());
        }
    }
}
