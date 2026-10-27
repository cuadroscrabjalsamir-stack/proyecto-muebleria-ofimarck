package com.ofimarck.service;

import com.ofimarck.domain.MovimientoInventario;
import com.ofimarck.domain.Producto;
import com.ofimarck.domain.Usuario;
import com.ofimarck.dto.request.AjusteInventarioDTO;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.repository.MovimientoInventarioRepository;
import com.ofimarck.repository.ProductoRepository;
import com.ofimarck.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventarioService {

    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public InventarioService(MovimientoInventarioRepository movimientoRepository,
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<MovimientoInventario> listarMovimientosRecientes() {
        return movimientoRepository.findTop50ByOrderByFechaDesc();
    }

    public List<MovimientoInventario> listarPorProducto(Integer idProducto) {
        return movimientoRepository.findByProducto_IdProductoOrderByFechaDesc(idProducto);
    }

    @Transactional
    public MovimientoInventario registrarAjuste(AjusteInventarioDTO dto, Integer idUsuario) {
        Producto producto = productoRepository.findById(dto.getIdProducto())
                .orElseThrow(() -> new BadRequestException("Producto no encontrado con ID: " + dto.getIdProducto()));

        int stockAnterior = producto.getStock();
        int stockNuevo;
        String tipo = dto.getTipo().toUpperCase().trim();

        if ("ENTRADA".equals(tipo)) {
            if (dto.getCantidad() <= 0) {
                throw new BadRequestException("La cantidad de entrada debe ser mayor a 0");
            }
            stockNuevo = stockAnterior + dto.getCantidad();
        } else if ("AJUSTE".equals(tipo)) {
            // Un ajuste puede ser absoluto (fijar nuevo stock) o relativo
            // Tratamos cantidad como nuevo valor absoluto o variación
            stockNuevo = dto.getCantidad();
            if (stockNuevo < 0) {
                throw new BadRequestException("El nuevo stock ajustado no puede ser negativo");
            }
        } else {
            throw new BadRequestException("Tipo de movimiento no válido. Use ENTRADA o AJUSTE");
        }

        producto.setStock(stockNuevo);
        productoRepository.save(producto);

        MovimientoInventario mov = new MovimientoInventario();
        mov.setProducto(producto);
        mov.setTipo(tipo);
        mov.setCantidad(stockNuevo - stockAnterior);
        mov.setStockAnterior(stockAnterior);
        mov.setStockNuevo(stockNuevo);
        mov.setMotivo(dto.getMotivo());

        if (idUsuario != null) {
            Usuario u = usuarioRepository.findById(idUsuario).orElse(null);
            mov.setUsuario(u);
        }

        return movimientoRepository.save(mov);
    }
}
