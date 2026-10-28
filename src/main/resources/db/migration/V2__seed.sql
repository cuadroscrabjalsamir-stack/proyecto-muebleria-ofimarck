-- V2__seed.sql
-- Datos semilla para pruebas del sistema Mueblería Ofimarck
-- Incluye roles, categorías, productos y usuarios de prueba con contraseñas BCrypt

SET search_path TO muebleria, public;

-- Insertar roles
INSERT INTO rol (nombre_rol, descripcion) VALUES
    ('CLIENTE', 'Cliente del portal web'),
    ('ADMIN', 'Administrador del panel de gestión')
ON CONFLICT (nombre_rol) DO NOTHING;

-- Sincronizar secuencia de rol (por si acaso)
SELECT setval('rol_id_rol_seq', (SELECT MAX(id_rol) FROM rol));

-- Insertar usuarios de prueba
-- Hashes BCrypt generados con BCryptPasswordEncoder de Spring Security.
-- Contraseñas: admin123 (admin) y cliente123 (cliente)
INSERT INTO usuario (nombres, apellidos, correo, telefono, password_hash, id_rol, estado)
SELECT 'Administrador', 'Ofimarck', 'admin@ofimarck.pe', '987654321',
       '$2a$12$EG6YV1gljb6ji4FxsD2d/uRq2No4N0QjmqmR5R7d2RobvJry8B6Me',
       (SELECT id_rol FROM rol WHERE nombre_rol = 'ADMIN'), TRUE
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'admin@ofimarck.pe');

INSERT INTO usuario (nombres, apellidos, correo, telefono, password_hash, id_rol, estado)
SELECT 'Carlos', 'Mendoza', 'carlos.mendoza@gmail.com', '991234567',
       '$2a$12$92PNR.nI06TY0/FzsU6GKexouqzryssg1IBEsQ1Gpl563W6Q71qEa',
       (SELECT id_rol FROM rol WHERE nombre_rol = 'CLIENTE'), TRUE
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'carlos.mendoza@gmail.com');

-- Insertar categorías
INSERT INTO categoria (nombre, slug, descripcion) VALUES
    ('Salas', 'salas', 'Sofás, sillones y mesas de sala'),
    ('Comedores', 'comedores', 'Mesas y sillas de comedor'),
    ('Dormitorios', 'dormitorios', 'Camas, armarios y veladores'),
    ('Oficina', 'oficina', 'Escritorios y sillas ergonómicas')
ON CONFLICT (nombre) DO NOTHING;

-- Insertar productos de ejemplo
INSERT INTO producto (sku, nombre, descripcion, material, dimensiones, precio, stock, stock_minimo, id_categoria, url_imagen, estado) VALUES
    ('SOF-001', 'Sofá Seccional Cuero', 'Sofá de tres cuerpos en cuero sintético de alta resistencia.', 'Cuero sintético', '220 x 95 x 85 cm', 4200.00, 12, 3,
     (SELECT id_categoria FROM categoria WHERE slug = 'salas'), '/img/sofa-seccional.jpg', TRUE),
    ('COM-001', 'Mesa de Comedor 6 sillas', 'Mesa de madera de 6 sillas con acabado natural.', 'Madera', '180 x 90 x 76 cm', 3800.00, 5, 3,
     (SELECT id_categoria FROM categoria WHERE slug = 'comedores'), '/img/comedor-6.jpg', TRUE),
    ('SIL-001', 'Silla Director', 'Silla de oficina con tapiz de cuero y base metálica.', 'Cuero y metal', '60 x 60 x 110 cm', 350.00, 18, 5,
     (SELECT id_categoria FROM categoria WHERE slug = 'oficina'), '/img/silla-director.jpg', TRUE),
    ('ARM-001', 'Armario Blanco', 'Armario de dos puertas con estantes y acabado mate.', 'Melamina', '100 x 50 x 190 cm', 1350.00, 4, 2,
     (SELECT id_categoria FROM categoria WHERE slug = 'dormitorios'), '/img/armario-blanco.jpg', TRUE),
    ('TV-001', 'Mueble para TV', 'Mueble de sala para TV de hasta 65 pulgadas.', 'Madera', '160 x 40 x 50 cm', 2800.00, 6, 2,
     (SELECT id_categoria FROM categoria WHERE slug = 'salas'), '/img/mueble-tv.jpg', TRUE)
ON CONFLICT (sku) DO NOTHING;
