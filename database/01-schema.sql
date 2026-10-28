-- ============================================================
-- Sistema Web Mueblería Ofimarck
-- Script DDL para PostgreSQL 15+
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS muebleria;
SET search_path TO muebleria, public;

-- ------------------------------------------------------------
-- 1. Roles, usuarios y direcciones
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rol (
    id_rol          SMALLSERIAL PRIMARY KEY,
    nombre_rol      VARCHAR(30)  NOT NULL UNIQUE,
    descripcion     VARCHAR(150),
    estado          BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario      SERIAL PRIMARY KEY,
    nombres         VARCHAR(100) NOT NULL,
    apellidos       VARCHAR(100) NOT NULL,
    correo          VARCHAR(150) NOT NULL,
    telefono        VARCHAR(20),
    password_hash   VARCHAR(255) NOT NULL,
    id_rol          SMALLINT     NOT NULL REFERENCES rol(id_rol),
    estado          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_registro  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ultimo_acceso   TIMESTAMPTZ
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuario_correo ON usuario (lower(correo));
CREATE INDEX IF NOT EXISTS ix_usuario_rol ON usuario (id_rol) WHERE estado;

CREATE TABLE IF NOT EXISTS direccion_envio (
    id_direccion    SERIAL PRIMARY KEY,
    id_usuario      INT          NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    alias           VARCHAR(50)  NOT NULL,
    destinatario    VARCHAR(150) NOT NULL,
    telefono        VARCHAR(20)  NOT NULL,
    departamento    VARCHAR(80)  NOT NULL,
    provincia       VARCHAR(80)  NOT NULL,
    distrito        VARCHAR(80)  NOT NULL,
    linea_direccion VARCHAR(200) NOT NULL,
    referencia      VARCHAR(200),
    codigo_postal   VARCHAR(10),
    predeterminada  BOOLEAN      NOT NULL DEFAULT FALSE,
    estado          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_direccion_usuario ON direccion_envio (id_usuario) WHERE estado;
CREATE UNIQUE INDEX IF NOT EXISTS ux_direccion_predeterminada ON direccion_envio (id_usuario)
    WHERE predeterminada AND estado;

-- ------------------------------------------------------------
-- 2. Catálogo
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categoria (
    id_categoria    SERIAL PRIMARY KEY,
    nombre          VARCHAR(80)  NOT NULL UNIQUE,
    slug            VARCHAR(90)  NOT NULL UNIQUE,
    descripcion     VARCHAR(255),
    estado          BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS producto (
    id_producto        SERIAL PRIMARY KEY,
    sku                VARCHAR(30)  NOT NULL UNIQUE,
    nombre             VARCHAR(150) NOT NULL,
    descripcion        TEXT,
    material           VARCHAR(100),
    dimensiones        VARCHAR(100),
    precio             NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    stock              INT          NOT NULL DEFAULT 0 CHECK (stock >= 0),
    stock_minimo       INT          NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
    id_categoria       INT          NOT NULL REFERENCES categoria(id_categoria),
    url_imagen         VARCHAR(255),
    estado             BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_producto_categoria ON producto (id_categoria) WHERE estado;
CREATE INDEX IF NOT EXISTS ix_producto_nombre ON producto (lower(nombre));
CREATE INDEX IF NOT EXISTS ix_producto_stock ON producto (stock) WHERE estado;

-- ------------------------------------------------------------
-- 3. Pedidos, detalle, pagos y envíos
-- ------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS pedido_codigo_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS pedido (
    id_pedido          BIGSERIAL PRIMARY KEY,
    codigo             VARCHAR(25)  NOT NULL UNIQUE,
    id_usuario         INT          NOT NULL REFERENCES usuario(id_usuario),
    id_direccion_envio INT          REFERENCES direccion_envio(id_direccion),
    direccion_snapshot JSONB        NOT NULL,
    subtotal           NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
    costo_envio        NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (costo_envio >= 0),
    total              NUMERIC(12,2) NOT NULL CHECK (total >= 0),
    estado             VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado IN ('PENDIENTE','PAGADO','EN_PREPARACION','EN_TRANSITO','ENTREGADO','CANCELADO','RECHAZADO')),
    observaciones      VARCHAR(300),
    fecha_pedido       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_pedido_total CHECK (total = subtotal + costo_envio)
);
CREATE INDEX IF NOT EXISTS ix_pedido_usuario_fecha ON pedido (id_usuario, fecha_pedido DESC);
CREATE INDEX IF NOT EXISTS ix_pedido_estado ON pedido (estado);

CREATE TABLE IF NOT EXISTS detalle_pedido (
    id_detalle       BIGSERIAL PRIMARY KEY,
    id_pedido        BIGINT       NOT NULL REFERENCES pedido(id_pedido) ON DELETE CASCADE,
    id_producto      INT          NOT NULL REFERENCES producto(id_producto),
    nombre_producto  VARCHAR(150) NOT NULL,
    cantidad         INT          NOT NULL CHECK (cantidad > 0),
    precio_unitario  NUMERIC(10,2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal         NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
    CONSTRAINT ux_detalle_pedido_producto UNIQUE (id_pedido, id_producto),
    CONSTRAINT chk_detalle_subtotal CHECK (subtotal = cantidad * precio_unitario)
);
CREATE INDEX IF NOT EXISTS ix_detalle_pedido ON detalle_pedido (id_pedido);
CREATE INDEX IF NOT EXISTS ix_detalle_producto ON detalle_pedido (id_producto);

CREATE TABLE IF NOT EXISTS pago (
    id_pago       BIGSERIAL PRIMARY KEY,
    id_pedido     BIGINT       NOT NULL UNIQUE REFERENCES pedido(id_pedido) ON DELETE CASCADE,
    metodo        VARCHAR(20)  NOT NULL CHECK (metodo IN ('TARJETA','TRANSFERENCIA','CONTRA_ENTREGA','YAPE','PLIN')),
    estado        VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado IN ('PENDIENTE','APROBADO','RECHAZADO','REEMBOLSADO')),
    monto         NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    referencia    VARCHAR(80),
    fecha_pago    TIMESTAMPTZ,
    creado_en     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_pago_estado ON pago (estado);

CREATE TABLE IF NOT EXISTS envio (
    id_envio       BIGSERIAL PRIMARY KEY,
    id_pedido      BIGINT       NOT NULL UNIQUE REFERENCES pedido(id_pedido) ON DELETE CASCADE,
    transportista  VARCHAR(100),
    tracking       VARCHAR(80)  UNIQUE,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado IN ('PENDIENTE','PROGRAMADO','EN_TRANSITO','ENTREGADO','FALLIDO')),
    fecha_estimada DATE,
    fecha_entrega  TIMESTAMPTZ,
    costo          NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (costo >= 0)
);
CREATE INDEX IF NOT EXISTS ix_envio_tracking ON envio (tracking);
CREATE INDEX IF NOT EXISTS ix_envio_estado ON envio (estado);

-- ------------------------------------------------------------
-- 4. Inventario y auditoría
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movimiento_inventario (
    id_movimiento  BIGSERIAL PRIMARY KEY,
    id_producto    INT          NOT NULL REFERENCES producto(id_producto),
    tipo           VARCHAR(10)  NOT NULL CHECK (tipo IN ('ENTRADA','SALIDA','AJUSTE')),
    cantidad       INT          NOT NULL CHECK (cantidad <> 0),
    stock_anterior INT          NOT NULL CHECK (stock_anterior >= 0),
    stock_nuevo    INT          NOT NULL CHECK (stock_nuevo >= 0),
    motivo         VARCHAR(200) NOT NULL,
    id_pedido      BIGINT       REFERENCES pedido(id_pedido),
    id_usuario     INT          REFERENCES usuario(id_usuario),
    fecha          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_mov_producto_fecha ON movimiento_inventario (id_producto, fecha DESC);
CREATE INDEX IF NOT EXISTS ix_mov_pedido ON movimiento_inventario (id_pedido);

CREATE TABLE IF NOT EXISTS historial_estado_pedido (
    id_historial    BIGSERIAL PRIMARY KEY,
    id_pedido       BIGINT       NOT NULL REFERENCES pedido(id_pedido) ON DELETE CASCADE,
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20)  NOT NULL
        CHECK (estado_nuevo IN ('PENDIENTE','PAGADO','EN_PREPARACION','EN_TRANSITO','ENTREGADO','CANCELADO','RECHAZADO')),
    comentario      VARCHAR(250),
    id_usuario      INT          REFERENCES usuario(id_usuario),
    fecha           TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_historial_pedido ON historial_estado_pedido (id_pedido, fecha DESC);

-- ------------------------------------------------------------
-- 5. Funciones y triggers
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_pedido_codigo() RETURNS trigger AS $$
BEGIN
    IF NEW.codigo IS NULL OR NEW.codigo = '' THEN
        NEW.codigo := 'ORD-' || to_char(COALESCE(NEW.fecha_pedido, now()), 'YYYYMMDD')
                      || '-' || lpad(nextval('muebleria.pedido_codigo_seq')::text, 4, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_pedido_codigo ON pedido;
CREATE TRIGGER trg_pedido_codigo
BEFORE INSERT ON pedido
FOR EACH ROW EXECUTE FUNCTION fn_pedido_codigo();

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

CREATE OR REPLACE FUNCTION fn_confirmar_pago() RETURNS trigger AS $$
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

DROP TRIGGER IF EXISTS trg_confirmar_pago ON pago;
CREATE TRIGGER trg_confirmar_pago
AFTER UPDATE OF estado ON pago
FOR EACH ROW EXECUTE FUNCTION fn_confirmar_pago();

CREATE OR REPLACE FUNCTION fn_historial_pedido() RETURNS trigger AS $$
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

DROP TRIGGER IF EXISTS trg_historial_pedido ON pedido;
CREATE TRIGGER trg_historial_pedido
AFTER UPDATE OF estado ON pedido
FOR EACH ROW EXECUTE FUNCTION fn_historial_pedido();

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
                 NULLIF(current_setting('app.usuario_id', true), '')::INT);
        END LOOP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_restaurar_stock ON pedido;
CREATE TRIGGER trg_restaurar_stock
AFTER UPDATE OF estado ON pedido
FOR EACH ROW EXECUTE FUNCTION fn_restaurar_stock();

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
-- 6. Vistas para reportes
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
