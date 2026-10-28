-- V3__triggers_views.sql
-- Triggers de negocio, valores por defecto de auditoría y vistas de reportes.
--
-- Motivo: estos objetos solo existían en database/01-schema.sql (script manual),
-- por lo que una base creada únicamente con Flyway no tenía:
--   * generación del código de pedido (pedido.codigo es NOT NULL),
--   * descuento/restauración de stock,
--   * historial automático de estados,
--   * vistas usadas por ReporteService (dashboard/reportes).
-- Al faltar, el checkout fallaba y los reportes devolvían error SQL.

SET search_path TO muebleria, public;

-- ------------------------------------------------------------
-- 0. Valores por defecto para columnas de auditoría
-- ------------------------------------------------------------
-- Las entidades JPA marcan estas columnas como insertable=false/updatable=false,
-- esperando que la base de datos las genere.
ALTER TABLE usuario ALTER COLUMN fecha_registro SET DEFAULT now();
ALTER TABLE producto ALTER COLUMN fecha_creacion SET DEFAULT now();
ALTER TABLE producto ALTER COLUMN fecha_actualizacion SET DEFAULT now();
ALTER TABLE direccion_envio ALTER COLUMN fecha_creacion SET DEFAULT now();
ALTER TABLE pedido ALTER COLUMN fecha_pedido SET DEFAULT now();
ALTER TABLE pedido ALTER COLUMN fecha_actualizacion SET DEFAULT now();
ALTER TABLE pago ALTER COLUMN creado_en SET DEFAULT now();
ALTER TABLE movimiento_inventario ALTER COLUMN fecha SET DEFAULT now();
ALTER TABLE historial_estado_pedido ALTER COLUMN fecha SET DEFAULT now();

-- Rellenar filas existentes que hayan quedado con NULL
UPDATE producto SET fecha_creacion = now() WHERE fecha_creacion IS NULL;
UPDATE producto SET fecha_actualizacion = now() WHERE fecha_actualizacion IS NULL;
UPDATE usuario SET fecha_registro = now() WHERE fecha_registro IS NULL;
UPDATE pedido SET fecha_pedido = now() WHERE fecha_pedido IS NULL;
UPDATE pedido SET fecha_actualizacion = now() WHERE fecha_actualizacion IS NULL;

-- ------------------------------------------------------------
-- 1. Código de pedido y timestamps
-- ------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS pedido_codigo_seq;

CREATE OR REPLACE FUNCTION fn_pedido_codigo() RETURNS trigger AS $$
BEGIN
    IF NEW.fecha_pedido IS NULL THEN
        NEW.fecha_pedido := now();
    END IF;

    IF NEW.codigo IS NULL OR NEW.codigo = '' THEN
        NEW.codigo := 'ORD-' || to_char(NEW.fecha_pedido, 'YYYYMMDD')
                      || '-' || lpad(nextval('muebleria.pedido_codigo_seq')::text, 4, '0');
    END IF;

    IF NEW.fecha_actualizacion IS NULL THEN
        NEW.fecha_actualizacion := now();
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_pedido_codigo ON pedido;
CREATE TRIGGER trg_pedido_codigo
BEFORE INSERT ON pedido
FOR EACH ROW EXECUTE FUNCTION fn_pedido_codigo();

-- JPA no incluye estas columnas en el UPDATE (insertable/updatable=false);
-- el trigger conserva el código y actualiza la marca de tiempo.
CREATE OR REPLACE FUNCTION fn_pedido_actualizado() RETURNS trigger AS $$
BEGIN
    IF NEW.codigo IS NULL OR NEW.codigo = '' THEN
        NEW.codigo := OLD.codigo;
    END IF;
    IF NEW.fecha_pedido IS NULL THEN
        NEW.fecha_pedido := OLD.fecha_pedido;
    END IF;
    NEW.fecha_actualizacion := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_pedido_actualizado ON pedido;
CREATE TRIGGER trg_pedido_actualizado
BEFORE UPDATE ON pedido
FOR EACH ROW EXECUTE FUNCTION fn_pedido_actualizado();

-- ------------------------------------------------------------
-- 2. Validación de stock al registrar el detalle
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_stock_detalle() RETURNS trigger AS $$
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

DROP TRIGGER IF EXISTS trg_validar_stock_detalle ON detalle_pedido;
CREATE TRIGGER trg_validar_stock_detalle
BEFORE INSERT ON detalle_pedido
FOR EACH ROW EXECUTE FUNCTION fn_validar_stock_detalle();

-- ------------------------------------------------------------
-- 3. Confirmación de pago: descuenta stock y registra movimiento
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_confirmar_pago() RETURNS trigger AS $$
DECLARE
    r         RECORD;
    v_stock   INT;
    v_usuario INT;
BEGIN
    IF NEW.estado = 'APROBADO'
       AND (TG_OP = 'INSERT' OR OLD.estado IS DISTINCT FROM 'APROBADO') THEN

        -- El movimiento de inventario se atribuye al dueño del pedido,
        -- porque movimiento_inventario.id_usuario es NOT NULL.
        SELECT id_usuario INTO v_usuario
        FROM muebleria.pedido WHERE id_pedido = NEW.id_pedido;

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
                 'Venta en línea confirmada', NEW.id_pedido, v_usuario);
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

-- Se elimina el trigger antiguo (script manual) y se separa en INSERT/UPDATE
-- para evitar doble descuento si ambos coexistieran.
DROP TRIGGER IF EXISTS trg_confirmar_pago ON pago;

DROP TRIGGER IF EXISTS trg_confirmar_pago_insert ON pago;
CREATE TRIGGER trg_confirmar_pago_insert
AFTER INSERT ON pago
FOR EACH ROW EXECUTE FUNCTION fn_confirmar_pago();

DROP TRIGGER IF EXISTS trg_confirmar_pago_update ON pago;
CREATE TRIGGER trg_confirmar_pago_update
AFTER UPDATE OF estado ON pago
FOR EACH ROW EXECUTE FUNCTION fn_confirmar_pago();

-- ------------------------------------------------------------
-- 4. Historial automático de estados del pedido
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_historial_pedido() RETURNS trigger AS $$
BEGIN
    IF NEW.estado IS DISTINCT FROM OLD.estado THEN
        INSERT INTO muebleria.historial_estado_pedido
            (id_pedido, estado_anterior, estado_nuevo, comentario, id_usuario)
        VALUES
            (NEW.id_pedido, OLD.estado, NEW.estado, 'Cambio de estado a ' || NEW.estado,
             NEW.id_usuario);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_historial_pedido ON pedido;
CREATE TRIGGER trg_historial_pedido
AFTER UPDATE OF estado ON pedido
FOR EACH ROW EXECUTE FUNCTION fn_historial_pedido();

-- ------------------------------------------------------------
-- 5. Restauración de stock al cancelar un pedido pagado
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_restaurar_stock() RETURNS trigger AS $$
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
                 NEW.id_usuario);
        END LOOP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_restaurar_stock ON pedido;
CREATE TRIGGER trg_restaurar_stock
AFTER UPDATE OF estado ON pedido
FOR EACH ROW EXECUTE FUNCTION fn_restaurar_stock();

-- ------------------------------------------------------------
-- 6. Fecha de actualización de producto
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_producto_actualizado() RETURNS trigger AS $$
BEGIN
    NEW.fecha_actualizacion := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_producto_actualizado ON producto;
CREATE TRIGGER trg_producto_actualizado
BEFORE UPDATE ON producto
FOR EACH ROW EXECUTE FUNCTION fn_producto_actualizado();

-- ------------------------------------------------------------
-- 7. Vistas para reportes
-- ------------------------------------------------------------
CREATE OR REPLACE VIEW vw_ventas_mensuales AS
SELECT date_trunc('month', p.fecha_pedido)::date AS mes,
       COUNT(*) AS pedidos,
       COALESCE(SUM(p.total), 0)::NUMERIC(12,2) AS ventas,
       COALESCE(ROUND(AVG(p.total), 2), 0) AS ticket_promedio
FROM muebleria.pedido p
WHERE p.estado IN ('PAGADO','EN_PREPARACION','EN_TRANSITO','ENTREGADO')
GROUP BY 1
ORDER BY 1;

CREATE OR REPLACE VIEW vw_inventario_alertas AS
SELECT p.id_producto, p.sku, p.nombre, c.nombre AS categoria,
       p.stock, p.stock_minimo,
       CASE WHEN p.stock = 0 THEN 'AGOTADO'
            WHEN p.stock <= p.stock_minimo THEN 'STOCK_BAJO'
            ELSE 'OPTIMO' END AS estado_stock,
       (p.stock * p.precio)::NUMERIC(12,2) AS valor_inventario
FROM muebleria.producto p
JOIN muebleria.categoria c ON c.id_categoria = p.id_categoria
WHERE p.estado;

CREATE OR REPLACE VIEW vw_top_productos AS
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
