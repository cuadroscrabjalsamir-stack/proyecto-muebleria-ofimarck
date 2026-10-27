-- V3__triggers_views.sql
-- Funciones, triggers y vistas de reportes para el esquema muebleria.
-- Se ejecuta tras V1 (tablas) y V2 (datos semilla).

SET search_path TO muebleria, public;

-- Secuencia para el código de pedido (ORD-YYYYMMDD-XXXX)
CREATE SEQUENCE IF NOT EXISTS muebleria.pedido_codigo_seq START WITH 1 INCREMENT BY 1;

-- ------------------------------------------------------------
-- Función: código automático de pedido
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION muebleria.fn_pedido_codigo() RETURNS trigger AS $$
BEGIN
    IF NEW.codigo IS NULL OR NEW.codigo = '' THEN
        NEW.codigo := 'ORD-' || to_char(COALESCE(NEW.fecha_pedido, now()), 'YYYYMMDD')
                      || '-' || lpad(nextval('muebleria.pedido_codigo_seq')::text, 4, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS muebleria.trg_pedido_codigo ON muebleria.pedido;
CREATE TRIGGER muebleria.trg_pedido_codigo
BEFORE INSERT ON muebleria.pedido
FOR EACH ROW EXECUTE FUNCTION muebleria.fn_pedido_codigo();

-- ------------------------------------------------------------
-- Función: validar stock al insertar detalle de pedido
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION muebleria.fn_validar_stock_detalle() RETURNS trigger AS $$
DECLARE
    v_stock  INT;
    v_estado BOOLEAN;
BEGIN
    SELECT stock, estado INTO v_stock, v_estado
    FROM muebleria.producto WHERE id_producto = NEW.id_producto FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El producto % no existe', NEW.id_producto;
    END IF;
    IF NOT v_estado THEN
        RAISE EXCEPTION 'El producto % está inactivo', NEW.id_producto;
    END IF;
    IF v_stock < NEW.cantidad THEN
        RAISE EXCEPTION 'Stock insuficiente para el producto %: disponible %, solicitado %',
            NEW.id_producto, v_stock, NEW.cantidad;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS muebleria.trg_validar_stock_detalle ON muebleria.detalle_pedido;
CREATE TRIGGER muebleria.trg_validar_stock_detalle
BEFORE INSERT ON muebleria.detalle_pedido
FOR EACH ROW EXECUTE FUNCTION muebleria.fn_validar_stock_detalle();

-- ------------------------------------------------------------
-- Función: confirmar pago (descuenta stock, genera kardex, marca pedido PAGADO)
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION muebleria.fn_confirmar_pago() RETURNS trigger AS $$
DECLARE
    r        RECORD;
    v_stock  INT;
BEGIN
    IF NEW.estado = 'APROBADO' AND (OLD.estado IS NULL OR OLD.estado <> 'APROBADO') THEN
        FOR r IN SELECT id_producto, cantidad
                 FROM muebleria.detalle_pedido WHERE id_pedido = NEW.id_pedido LOOP
            SELECT stock INTO v_stock FROM muebleria.producto
            WHERE id_producto = r.id_producto FOR UPDATE;

            IF v_stock < r.cantidad THEN
                RAISE EXCEPTION 'Stock insuficiente al confirmar el pago del producto %', r.id_producto;
            END IF;

            UPDATE muebleria.producto
            SET stock = stock - r.cantidad
            WHERE id_producto = r.id_producto;

            INSERT INTO muebleria.movimiento_inventario
                (id_producto, tipo, cantidad, stock_anterior, stock_nuevo, motivo, id_pedido, id_usuario)
            VALUES
                (r.id_producto, 'SALIDA', r.cantidad, v_stock, v_stock - r.cantidad,
                 'Venta en línea confirmada', NEW.id_pedido,
                 NULLIF(current_setting('app.usuario_id', true), '')::INT);
        END LOOP;

        UPDATE muebleria.pedido
        SET estado = 'PAGADO', fecha_actualizacion = now()
        WHERE id_pedido = NEW.id_pedido AND estado = 'PENDIENTE';

        UPDATE muebleria.pago SET fecha_pago = COALESCE(fecha_pago, now())
        WHERE id_pago = NEW.id_pago;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS muebleria.trg_confirmar_pago ON muebleria.pago;
CREATE TRIGGER muebleria.trg_confirmar_pago
AFTER UPDATE OF estado ON muebleria.pago
FOR EACH ROW EXECUTE FUNCTION muebleria.fn_confirmar_pago();

-- ------------------------------------------------------------
-- Función: historial de cambios de estado de pedido
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION muebleria.fn_historial_pedido() RETURNS trigger AS $$
BEGIN
    IF NEW.estado IS DISTINCT FROM OLD.estado THEN
        INSERT INTO muebleria.historial_estado_pedido
            (id_pedido, estado_anterior, estado_nuevo, comentario, id_usuario)
        VALUES
            (NEW.id_pedido, OLD.estado, NEW.estado, 'Cambio de estado a ' || NEW.estado,
             NULLIF(current_setting('app.usuario_id', true), '')::INT);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS muebleria.trg_historial_pedido ON muebleria.pedido;
CREATE TRIGGER muebleria.trg_historial_pedido
AFTER UPDATE OF estado ON muebleria.pedido
FOR EACH ROW EXECUTE FUNCTION muebleria.fn_historial_pedido();

-- ------------------------------------------------------------
-- Función: restaurar stock al cancelar pedido
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION muebleria.fn_restaurar_stock() RETURNS trigger AS $$
DECLARE
    r       RECORD;
    v_stock INT;
BEGIN
    IF NEW.estado = 'CANCELADO' AND OLD.estado IN ('PAGADO','EN_PREPARACION','EN_TRANSITO') THEN
        FOR r IN SELECT id_producto, cantidad
                 FROM muebleria.detalle_pedido WHERE id_pedido = NEW.id_pedido LOOP
            SELECT stock INTO v_stock FROM muebleria.producto
            WHERE id_producto = r.id_producto FOR UPDATE;

            UPDATE muebleria.producto
            SET stock = stock + r.cantidad
            WHERE id_producto = r.id_producto;

            INSERT INTO muebleria.movimiento_inventario
                (id_producto, tipo, cantidad, stock_anterior, stock_nuevo, motivo, id_pedido, id_usuario)
            VALUES
                (r.id_producto, 'ENTRADA', r.cantidad, v_stock, v_stock + r.cantidad,
                 'Restauración por cancelación de pedido', NEW.id_pedido,
                 NULLIF(current_setting('app.usuario_id', true), '')::INT);
        END LOOP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS muebleria.trg_restaurar_stock ON muebleria.pedido;
CREATE TRIGGER muebleria.trg_restaurar_stock
AFTER UPDATE OF estado ON muebleria.pedido
FOR EACH ROW EXECUTE FUNCTION muebleria.fn_restaurar_stock();

-- ------------------------------------------------------------
-- Función: actualizar fecha de modificación de producto
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION muebleria.fn_producto_actualizado() RETURNS trigger AS $$
BEGIN
    NEW.fecha_actualizacion := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS muebleria.trg_producto_actualizado ON muebleria.producto;
CREATE TRIGGER muebleria.trg_producto_actualizado
BEFORE UPDATE ON muebleria.producto
FOR EACH ROW EXECUTE FUNCTION muebleria.fn_producto_actualizado();

-- ------------------------------------------------------------
-- Vistas para reportes gerenciales
-- ------------------------------------------------------------
CREATE OR REPLACE VIEW muebleria.vw_ventas_mensuales AS
SELECT date_trunc('month', p.fecha_pedido)::date AS mes,
       COUNT(*) AS pedidos,
       COALESCE(SUM(p.total), 0)::NUMERIC(12,2) AS ventas,
       COALESCE(ROUND(AVG(p.total), 2), 0) AS ticket_promedio
FROM muebleria.pedido p
WHERE p.estado IN ('PAGADO','EN_PREPARACION','EN_TRANSITO','ENTREGADO')
GROUP BY 1
ORDER BY 1;

CREATE OR REPLACE VIEW muebleria.vw_inventario_alertas AS
SELECT p.id_producto, p.sku, p.nombre, c.nombre AS categoria,
       p.stock, p.stock_minimo,
       CASE WHEN p.stock = 0 THEN 'AGOTADO'
            WHEN p.stock <= p.stock_minimo THEN 'STOCK_BAJO'
            ELSE 'OPTIMO' END AS estado_stock,
       (p.stock * p.precio)::NUMERIC(12,2) AS valor_inventario
FROM muebleria.producto p
JOIN muebleria.categoria c ON c.id_categoria = p.id_categoria
WHERE p.estado;

CREATE OR REPLACE VIEW muebleria.vw_top_productos AS
SELECT p.id_producto, p.nombre, c.nombre AS categoria,
       SUM(d.cantidad)::INT AS unidades_vendidas,
       SUM(d.subtotal)::NUMERIC(12,2) AS ingresos
FROM muebleria.detalle_pedido d
JOIN muebleria.pedido pe ON pe.id_pedido = d.id_pedido
JOIN muebleria.producto p ON p.id_producto = d.id_producto
JOIN muebleria.categoria c ON c.id_categoria = p.id_categoria
WHERE pe.estado IN ('PAGADO','EN_PREPARACION','EN_TRANSITO','ENTREGADO')
GROUP BY p.id_producto, p.nombre, c.nombre
ORDER BY unidades_vendidas DESC;
