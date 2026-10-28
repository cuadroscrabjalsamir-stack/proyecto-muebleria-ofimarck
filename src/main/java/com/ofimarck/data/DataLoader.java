package com.ofimarck.data;

import com.ofimarck.domain.Categoria;
import com.ofimarck.domain.Producto;
import com.ofimarck.domain.Rol;
import com.ofimarck.domain.Usuario;
import com.ofimarck.repository.CategoriaRepository;
import com.ofimarck.repository.ProductoRepository;
import com.ofimarck.repository.RolRepository;
import com.ofimarck.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Cargador de datos iniciales (seed) que se ejecuta al arrancar la aplicación.
 *
 * <p>Garantiza que existan los datos mínimos de funcionamiento (roles,
 * categorías, productos y usuarios de prueba) incluso si las migraciones de
 * Flyway no se aplicaron sobre una base previamente inicializada. Es
 * <b>idempotente</b>: comprueba cada registro por su clave natural
 * (nombre de rol, slug, SKU o correo) y solo inserta lo que falta, por lo que
 * puede ejecutarse en cada arranque sin duplicar información.
 *
 * <p>Principio de Responsabilidad Única (S de SOLID): esta clase solo puebla
 * datos iniciales; no contiene lógica de negocio.
 *
 * <p>Se desactiva con el perfil {@code production} para no crear usuarios de
 * prueba en entornos reales.
 */
@Component
@Profile("!production")
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(CategoriaRepository categoriaRepository,
                      ProductoRepository productoRepository,
                      RolRepository rolRepository,
                      UsuarioRepository usuarioRepository,
                      PasswordEncoder passwordEncoder) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Rol rolAdmin = obtenerOCrearRol("ADMIN", "Administrador del panel de gestión");
        Rol rolCliente = obtenerOCrearRol("CLIENTE", "Cliente del portal web");

        Categoria salas = obtenerOCrearCategoria("salas", "Salas", "Sofás, sillones y mesas de sala");
        Categoria comedores = obtenerOCrearCategoria("comedores", "Comedores", "Mesas y sillas de comedor");
        Categoria dormitorios = obtenerOCrearCategoria("dormitorios", "Dormitorios", "Camas, armarios y veladores");
        Categoria oficina = obtenerOCrearCategoria("oficina", "Oficina", "Escritorios y sillas ergonómicas");

        crearProductoSiNoExiste("SOF-001", "Sofá Seccional Cuero",
                "Sofá de tres cuerpos en cuero sintético de alta resistencia y acabados ergonómicos.",
                "Cuero sintético", "220 x 95 x 85 cm", "4200.00", 12, 3, salas, "/img/sofa-seccional.jpg");

        crearProductoSiNoExiste("COM-001", "Mesa de Comedor 6 sillas",
                "Mesa de madera maciza para 6 personas con acabado natural y sillas acolchadas.",
                "Madera maciza", "180 x 90 x 76 cm", "3800.00", 5, 3, comedores, "/img/comedor-6.jpg");

        crearProductoSiNoExiste("SIL-001", "Silla Director",
                "Silla ergonómica de oficina con tapiz de cuero, ajuste neumático y base metálica giratoria.",
                "Cuero y metal", "60 x 60 x 110 cm", "350.00", 18, 5, oficina, "/img/silla-director.jpg");

        crearProductoSiNoExiste("ARM-001", "Armario Blanco",
                "Armario moderno de dos puertas corredizas con estanterías interiores y acabado blanco mate.",
                "Melamina 18mm", "100 x 50 x 190 cm", "1350.00", 4, 2, dormitorios, "/img/armario-blanco.jpg");

        crearProductoSiNoExiste("TV-001", "Mueble para TV",
                "Mueble contemporáneo de sala para pantallas de hasta 65 pulgadas con cajones y repisas.",
                "Madera y Melamina", "160 x 40 x 50 cm", "2800.00", 6, 2, salas, "/img/mueble-tv.jpg");

        crearUsuarioSiNoExiste("Administrador", "Ofimarck", "admin@ofimarck.pe",
                "987654321", "admin123", rolAdmin);
        crearUsuarioSiNoExiste("Carlos", "Mendoza", "carlos.mendoza@gmail.com",
                "991234567", "cliente123", rolCliente);

        log.info("seed_verificado categorias={} productos={} usuarios={}",
                categoriaRepository.count(), productoRepository.count(), usuarioRepository.count());
    }

    private Rol obtenerOCrearRol(String nombre, String descripcion) {
        return rolRepository.findByNombreRol(nombre).orElseGet(() -> {
            Rol rol = new Rol();
            rol.setNombreRol(nombre);
            rol.setDescripcion(descripcion);
            rol.setEstado(true);
            log.info("seed_rol_creado nombre={}", nombre);
            return rolRepository.save(rol);
        });
    }

    private Categoria obtenerOCrearCategoria(String slug, String nombre, String descripcion) {
        return categoriaRepository.findBySlug(slug).orElseGet(() -> {
            Categoria categoria = new Categoria();
            categoria.setNombre(nombre);
            categoria.setSlug(slug);
            categoria.setDescripcion(descripcion);
            categoria.setEstado(true);
            log.info("seed_categoria_creada slug={}", slug);
            return categoriaRepository.save(categoria);
        });
    }

    private void crearProductoSiNoExiste(String sku, String nombre, String descripcion,
                                         String material, String dimensiones, String precio,
                                         int stock, int stockMinimo, Categoria categoria,
                                         String urlImagen) {
        if (productoRepository.existsBySkuIgnoreCase(sku)) {
            return;
        }
        Producto producto = new Producto();
        producto.setSku(sku);
        producto.setNombre(nombre);
        producto.setDescripcion(descripcion);
        producto.setMaterial(material);
        producto.setDimensiones(dimensiones);
        producto.setPrecio(new BigDecimal(precio));
        producto.setStock(stock);
        producto.setStockMinimo(stockMinimo);
        producto.setCategoria(categoria);
        producto.setUrlImagen(urlImagen);
        producto.setEstado(true);
        productoRepository.save(producto);
        log.info("seed_producto_creado sku={}", sku);
    }

    private void crearUsuarioSiNoExiste(String nombres, String apellidos, String correo,
                                        String telefono, String passwordPlano, Rol rol) {
        if (rol == null || usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setCorreo(correo);
        usuario.setTelefono(telefono);
        usuario.setPasswordHash(passwordEncoder.encode(passwordPlano));
        usuario.setRol(rol);
        usuario.setEstado(true);
        usuarioRepository.save(usuario);
        log.info("seed_usuario_creado correo={} rol={}", correo, rol.getNombreRol());
    }
}
