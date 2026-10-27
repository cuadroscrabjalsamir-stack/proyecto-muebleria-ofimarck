package com.ofimarck.mapper;

import com.ofimarck.domain.Producto;
import com.ofimarck.dto.response.ProductoDTO;
import com.ofimarck.dto.response.ProductoPublicoDTO;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {

    public ProductoDTO toDto(Producto producto) {
        ProductoDTO dto = new ProductoDTO();
        dto.setIdProducto(producto.getIdProducto());
        dto.setSku(producto.getSku());
        dto.setNombre(producto.getNombre());
        dto.setDescripcion(producto.getDescripcion());
        dto.setMaterial(producto.getMaterial());
        dto.setDimensiones(producto.getDimensiones());
        dto.setPrecio(producto.getPrecio());
        dto.setStock(producto.getStock());
        dto.setStockMinimo(producto.getStockMinimo());
        dto.setIdCategoria(producto.getCategoria() != null ? producto.getCategoria().getIdCategoria() : null);
        dto.setNombreCategoria(producto.getCategoria() != null ? producto.getCategoria().getNombre() : null);
        dto.setUrlImagen(producto.getUrlImagen());
        dto.setEstado(producto.getEstado());
        return dto;
    }

    public ProductoPublicoDTO toPublicoDto(Producto producto) {
        return new ProductoPublicoDTO(producto);
    }
}