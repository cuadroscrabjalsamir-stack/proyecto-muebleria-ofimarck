package com.ofimarck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ofimarck.domain.MovimientoInventario;
import com.ofimarck.domain.Producto;
import com.ofimarck.domain.Usuario;
import com.ofimarck.dto.request.AjusteInventarioDTO;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.repository.MovimientoInventarioRepository;
import com.ofimarck.repository.ProductoRepository;
import com.ofimarck.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock private MovimientoInventarioRepository movimientoRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private com.ofimarck.service.InventarioService service;

    private Producto producto;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setIdProducto(100);
        producto.setStock(5);
        producto.setEstado(true);

        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setEstado(true);

        when(productoRepository.findById(100)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registrarAjusteEntradaSumaStock() {
        AjusteInventarioDTO dto = new AjusteInventarioDTO();
        dto.setIdProducto(100);
        dto.setTipo("ENTRADA");
        dto.setCantidad(3);
        dto.setMotivo("Ingreso de taller");

        MovimientoInventario mov = service.registrarAjuste(dto, 1);

        assertThat(producto.getStock()).isEqualTo(8);
        assertThat(mov.getTipo()).isEqualTo("ENTRADA");
        assertThat(mov.getCantidad()).isEqualTo(3);
        assertThat(mov.getStockAnterior()).isEqualTo(5);
        assertThat(mov.getStockNuevo()).isEqualTo(8);
        verify(productoRepository, times(1)).save(producto);
        verify(movimientoRepository, times(1)).save(any(MovimientoInventario.class));
    }

    @Test
    void registrarAjusteAjusteAbsolutoFijaStock() {
        AjusteInventarioDTO dto = new AjusteInventarioDTO();
        dto.setIdProducto(100);
        dto.setTipo("AJUSTE");
        dto.setCantidad(10);
        dto.setMotivo("Conteo físico");

        MovimientoInventario mov = service.registrarAjuste(dto, 1);

        assertThat(producto.getStock()).isEqualTo(10);
        assertThat(mov.getStockNuevo()).isEqualTo(10);
        assertThat(mov.getCantidad()).isEqualTo(5);
    }

    @Test
    void registrarAjusteEntradaCeroFalla() {
        AjusteInventarioDTO dto = new AjusteInventarioDTO();
        dto.setIdProducto(100);
        dto.setTipo("ENTRADA");
        dto.setCantidad(0);
        dto.setMotivo("x");

        assertThatThrownBy(() -> service.registrarAjuste(dto, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("mayor a 0");
    }

    @Test
    void registrarAjusteAjusteNegativoFalla() {
        AjusteInventarioDTO dto = new AjusteInventarioDTO();
        dto.setIdProducto(100);
        dto.setTipo("AJUSTE");
        dto.setCantidad(-1);
        dto.setMotivo("x");

        assertThatThrownBy(() -> service.registrarAjuste(dto, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no puede ser negativo");
    }

    @Test
    void registrarAjusteProductoInexistente() {
        when(productoRepository.findById(999)).thenReturn(Optional.empty());

        AjusteInventarioDTO dto = new AjusteInventarioDTO();
        dto.setIdProducto(999);
        dto.setTipo("ENTRADA");
        dto.setCantidad(1);
        dto.setMotivo("x");

        assertThatThrownBy(() -> service.registrarAjuste(dto, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
