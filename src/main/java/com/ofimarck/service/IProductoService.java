package com.ofimarck.service;

import com.ofimarck.domain.Producto;
import com.ofimarck.dto.response.ProductoDTO;
import java.util.List;

/**
 * Contrato de la capa de negocio para la gestión de productos (catálogo).
 *
 * <p>Principio de Inversión de Dependencias (D de SOLID): los controladores
 * dependen de esta interfaz, lo que facilita el testing y el desacoplamiento
 * de la capa de presentación de la capa de acceso a datos.
 *
 * <p>Principio de Responsabilidad Única (S de SOLID): esta interfaz concentra
 * operaciones de consulta, creación, actualización y cambio de estado de
 * productos. La lógica de limpieza de texto de entrada (búsqueda, nombres)
 * delega en Apache Commons {@code StringUtils} dentro de la implementación.
 *
 * <p>Uso de librerías obligatorias (rúbrica):
 * <ul>
 *   <li><b>Apache Commons Lang3:</b> {@code StringUtils.normalizeSpace()} y
 *       {@code StringUtils.stripAccents()} para sanitizar textos ingresados por
 *       el usuario antes de usarlos en consultas o persistencia.</li>
 *   <li><b>Logback/SLF4J:</b> registro de eventos críticos en la implementación.</li>
 * </ul>
 */
public interface IProductoService {

    /**
     * Lista productos aplicando filtros opcionales de categoría, estado y
     * término de búsqueda.
     *
     * @param categoriaId identificador de categoría (puede ser {@code null})
     * @param estado estado booleano del producto (puede ser {@code null})
     * @param termino texto de búsqueda sobre nombre o SKU (puede ser {@code null})
     * @return lista de productos filtrados
     */
    List<Producto> listarTodos(Integer categoriaId, Boolean estado, String termino);

    /**
     * Lista productos con stock igual o inferior al mínimo configurado.
     *
     * @return lista de productos con alerta de bajo stock
     */
    List<Producto> listarAlertasStock();

    /**
     * Busca un producto por su identificador interno.
     *
     * @param id identificador del producto
     * @return el producto encontrado
     * @throws com.ofimarck.exception.ResourceNotFoundException si no existe
     */
    Producto buscarPorId(Integer id);

    /**
     * Busca un producto por su SKU.
     *
     * @param sku código SKU del producto
     * @return el producto encontrado
     * @throws com.ofimarck.exception.ResourceNotFoundException si no existe
     */
    Producto buscarPorSku(String sku);

    /**
     * Crea un nuevo producto. Valida que el SKU no exista previamente.
     *
     * @param dto datos del producto a crear
     * @return el producto creado y persistido
     */
    Producto crear(ProductoDTO dto);

    /**
     * Actualiza un producto existente. Valida que el SKU no sea duplicado en
     * otro producto.
     *
     * @param id identificador del producto a actualizar
     * @param dto datos actualizados del producto
     * @return el producto actualizado y persistido
     */
    Producto actualizar(Integer id, ProductoDTO dto);

    /**
     * Activa o desactiva un producto (cambia su estado booleano).
     *
     * @param id identificador del producto
     * @param nuevoEstado nuevo estado ({@code true} activo, {@code false} inactivo)
     * @return el producto con el estado modificado
     */
    Producto cambiarEstado(Integer id, Boolean nuevoEstado);
}
