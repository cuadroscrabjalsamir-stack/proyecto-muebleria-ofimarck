package com.ofimarck.service;

import com.ofimarck.domain.Categoria;
import com.ofimarck.domain.Producto;
import com.ofimarck.dto.response.ProductoDTO;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.repository.CategoriaRepository;
import com.ofimarck.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(ProductoRepository productoRepository, CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public List<Producto> listarTodos(Integer categoriaId, Boolean estado, String termino) {
        List<Producto> lista;
        boolean tieneTermino = termino != null && !termino.isBlank();
        boolean tieneCategoria = categoriaId != null && categoriaId > 0;

        if (tieneCategoria && tieneTermino) {
            lista = productoRepository.buscarPorCategoriaYTermino(categoriaId, termino.trim());
        } else if (tieneCategoria) {
            lista = productoRepository.findByCategoria_IdCategoriaOrderByFechaCreacionDesc(categoriaId);
        } else if (tieneTermino) {
            lista = productoRepository.buscarPorTermino(termino.trim());
        } else {
            lista = productoRepository.findByOrderByFechaCreacionDesc();
        }

        if (estado != null) {
            lista = lista.stream()
                    .filter(p -> p.getEstado().equals(estado))
                    .collect(Collectors.toList());
        }

        return lista;
    }

    public List<Producto> listarAlertasStock() {
        return productoRepository.findAlertasStock();
    }

    public Producto buscarPorId(Integer id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
    }

    public Producto buscarPorSku(String sku) {
        return productoRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con SKU: " + sku));
    }

    @Transactional
    public Producto crear(ProductoDTO dto) {
        if (productoRepository.existsBySkuIgnoreCase(dto.getSku())) {
            throw new BadRequestException("Ya existe un producto con el SKU: " + dto.getSku());
        }
        Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Categoría no encontrada con ID: " + dto.getIdCategoria()));

        Producto p = new Producto();
        mapearDtoAEntidad(dto, p, categoria);
        return productoRepository.save(p);
    }

    @Transactional
    public Producto actualizar(Integer id, ProductoDTO dto) {
        Producto p = buscarPorId(id);
        if (!p.getSku().equalsIgnoreCase(dto.getSku()) &&
                productoRepository.existsBySkuIgnoreCase(dto.getSku())) {
            throw new BadRequestException("Ya existe otro producto con el SKU: " + dto.getSku());
        }
        Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Categoría no encontrada con ID: " + dto.getIdCategoria()));

        mapearDtoAEntidad(dto, p, categoria);
        return productoRepository.save(p);
    }

    @Transactional
    public Producto cambiarEstado(Integer id, Boolean nuevoEstado) {
        Producto p = buscarPorId(id);
        p.setEstado(nuevoEstado);
        return productoRepository.save(p);
    }

    private void mapearDtoAEntidad(ProductoDTO dto, Producto p, Categoria categoria) {
        p.setSku(dto.getSku().trim().toUpperCase());
        p.setNombre(dto.getNombre().trim());
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
