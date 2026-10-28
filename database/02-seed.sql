-- ============================================================
-- Sistema Web Mueblería Ofimarck
-- Script de Datos Semilla para PostgreSQL
-- ============================================================

SET search_path TO muebleria, public;

-- 1. Roles
INSERT INTO rol (id_rol, nombre_rol, descripcion) VALUES
(1, 'CLIENTE', 'Cliente del portal web'),
(2, 'ADMIN', 'Administrador del panel de gestión')
ON CONFLICT (id_rol) DO UPDATE SET nombre_rol = EXCLUDED.nombre_rol;

-- Sincronizar secuencia de rol
SELECT setval('rol_id_rol_seq', (SELECT MAX(id_rol) FROM rol));

-- 2. Categorías
INSERT INTO categoria (nombre, slug, descripcion) VALUES
('Salas', 'salas', 'Sofás, sillones y mesas de sala'),
('Comedores', 'comedores', 'Mesas y sillas de comedor'),
('Dormitorios', 'dormitorios', 'Camas, armarios y veladores'),
('Oficina', 'oficina', 'Escritorios y sillas ergonómicas')
ON CONFLICT (nombre) DO NOTHING;

-- 3. Productos (Catálogo Oficial del Proyecto)
INSERT INTO producto (sku, nombre, descripcion, material, dimensiones, precio, stock, stock_minimo, id_categoria, url_imagen, estado)
VALUES
('SOF-001', 'Sofá Seccional Cuero', 'Sofá de tres cuerpos en cuero sintético de alta resistencia y acabados ergonómicos.', 'Cuero sintético', '220 x 95 x 85 cm', 4200.00, 12, 3,
 (SELECT id_categoria FROM categoria WHERE slug = 'salas'), '/img/img_sofa.jpg', TRUE),
('COM-001', 'Mesa de Comedor 6 sillas', 'Mesa de madera maciza para 6 personas con acabado natural y sillas acolchadas.', 'Madera maciza', '180 x 90 x 76 cm', 3800.00, 5, 3,
 (SELECT id_categoria FROM categoria WHERE slug = 'comedores'), '/img/mesa_comedor.png', TRUE),
('SIL-001', 'Silla Director', 'Silla ergonómica de oficina con tapiz de cuero, ajuste neumático y base metálica giratoria.', 'Cuero y metal', '60 x 60 x 110 cm', 350.00, 18, 5,
 (SELECT id_categoria FROM categoria WHERE slug = 'oficina'), '/img/silla_director.png', TRUE),
('ARM-001', 'Armario Blanco', 'Armario moderno de dos puertas corredizas con estanterías interiores y acabado blanco mate.', 'Melamina 18mm', '100 x 50 x 190 cm', 1350.00, 4, 2,
 (SELECT id_categoria FROM categoria WHERE slug = 'dormitorios'), '/img/armario.png', TRUE),
('TV-001', 'Mueble para TV', 'Mueble contemporáneo de sala para pantallas de hasta 65 pulgadas con cajones y repisas.', 'Madera y Melamina', '160 x 40 x 50 cm', 2800.00, 2, 2,
 (SELECT id_categoria FROM categoria WHERE slug = 'salas'), '/img/tv_mesa.png', TRUE)
ON CONFLICT (sku) DO NOTHING;

-- 4. Usuarios Iniciales (Admin y Cliente de prueba)
-- Insertar usuarios de prueba de forma segura sin depender de ON CONFLICT complejo
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

-- 6. Pedido de prueba con pago pendiente y envío registrado
DO $$
DECLARE
    v_user_id INT;
    v_dir_id INT;
    v_prod1_id INT;
    v_prod2_id INT;
    v_ped_id BIGINT;
BEGIN
    SELECT id_usuario INTO v_user_id FROM usuario WHERE correo = 'carlos.mendoza@gmail.com';
    SELECT id_direccion INTO v_dir_id FROM direccion_envio WHERE id_usuario = v_user_id LIMIT 1;
    SELECT id_producto INTO v_prod1_id FROM producto WHERE sku = 'SIL-001';
    SELECT id_producto INTO v_prod2_id FROM producto WHERE sku = 'TV-001';

    IF v_user_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM pedido WHERE codigo = 'ORD-20261027-0001') THEN
        INSERT INTO pedido (codigo, id_usuario, id_direccion_envio, direccion_snapshot, subtotal, costo_envio, total, estado, observaciones)
        VALUES ('ORD-20261027-0001', v_user_id, v_dir_id,
                jsonb_build_object('destinatario', 'Carlos Mendoza', 'telefono', '991234567', 'direccion', 'Av. Larco 456 Dpto 302, Miraflores, Lima'),
                3150.00, 50.00, 3200.00, 'PENDIENTE', 'Entregar en horario de oficina por favor')
        RETURNING id_pedido INTO v_ped_id;

        INSERT INTO detalle_pedido (id_pedido, id_producto, nombre_producto, cantidad, precio_unitario, subtotal)
        VALUES (v_ped_id, v_prod1_id, 'Silla Director', 1, 350.00, 350.00),
               (v_ped_id, v_prod2_id, 'Mueble para TV', 1, 2800.00, 2800.00);

        INSERT INTO pago (id_pedido, metodo, estado, monto, referencia)
        VALUES (v_ped_id, 'TRANSFERENCIA', 'PENDIENTE', 3200.00, 'BCP-TRANSF-982341');

        INSERT INTO envio (id_pedido, transportista, tracking, estado, fecha_estimada, costo)
        VALUES (v_ped_id, 'Olva Courier', 'TRK-OFM-9921', 'PENDIENTE', CURRENT_DATE + INTERVAL '3 days', 50.00);

        INSERT INTO historial_estado_pedido (id_pedido, estado_anterior, estado_nuevo, comentario, id_usuario)
        VALUES (v_ped_id, NULL, 'PENDIENTE', 'Pedido creado por el cliente en línea', v_user_id);
    END IF;
END $$;
