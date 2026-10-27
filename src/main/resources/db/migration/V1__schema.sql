-- V1__schema.sql
-- Esquema inicial del sistema Mueblería Ofimarck
-- Schema: muebleria

CREATE SCHEMA IF NOT EXISTS muebleria;

SET search_path TO muebleria, public;

-- Tabla: rol
CREATE TABLE rol (
    id_rol SMALLINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    nombre_rol VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(150),
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabla: categoria
CREATE TABLE categoria (
    id_categoria INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    slug VARCHAR(90) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabla: usuario
CREATE TABLE usuario (
    id_usuario INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    telefono VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    id_rol SMALLINT NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro TIMESTAMP WITH TIME ZONE,
    ultimo_acceso TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
);

-- Tabla: producto
CREATE TABLE producto (
    id_producto INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    sku VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    material VARCHAR(100),
    dimensiones VARCHAR(100),
    precio NUMERIC(10, 2) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    stock_minimo INTEGER NOT NULL DEFAULT 0,
    id_categoria INTEGER NOT NULL,
    url_imagen VARCHAR(255),
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP WITH TIME ZONE,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria)
);

-- Tabla: direccion_envio
CREATE TABLE direccion_envio (
    id_direccion INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    id_usuario INTEGER NOT NULL,
    alias VARCHAR(50) NOT NULL,
    destinatario VARCHAR(150) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    departamento VARCHAR(80) NOT NULL,
    provincia VARCHAR(80) NOT NULL,
    distrito VARCHAR(80) NOT NULL,
    linea_direccion VARCHAR(200) NOT NULL,
    referencia VARCHAR(200),
    codigo_postal VARCHAR(10),
    predeterminada BOOLEAN NOT NULL DEFAULT FALSE,
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_direccion_envio_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

-- Tabla: pedido
CREATE TABLE pedido (
    id_pedido BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    codigo VARCHAR(25) NOT NULL UNIQUE,
    id_usuario INTEGER NOT NULL,
    id_direccion_envio INTEGER,
    direccion_snapshot JSONB NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    costo_envio NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total NUMERIC(12, 2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    observaciones VARCHAR(300),
    fecha_pedido TIMESTAMP WITH TIME ZONE,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_pedido_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    CONSTRAINT fk_pedido_direccion_envio FOREIGN KEY (id_direccion_envio) REFERENCES direccion_envio(id_direccion)
);

-- Tabla: detalle_pedido
CREATE TABLE detalle_pedido (
    id_detalle BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    id_pedido BIGINT NOT NULL,
    id_producto INTEGER NOT NULL,
    nombre_producto VARCHAR(150) NOT NULL,
    cantidad INTEGER NOT NULL,
    precio_unitario NUMERIC(10, 2) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_detalle_pedido_pedido FOREIGN KEY (id_pedido) REFERENCES pedido(id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_pedido_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);

-- Tabla: pago
CREATE TABLE pago (
    id_pago BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    id_pedido BIGINT NOT NULL UNIQUE,
    metodo VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    monto NUMERIC(12, 2) NOT NULL,
    referencia VARCHAR(80),
    fecha_pago TIMESTAMP WITH TIME ZONE,
    creado_en TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_pago_pedido FOREIGN KEY (id_pedido) REFERENCES pedido(id_pedido) ON DELETE CASCADE
);

-- Tabla: envio
CREATE TABLE envio (
    id_envio BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    id_pedido BIGINT NOT NULL UNIQUE,
    transportista VARCHAR(100),
    tracking VARCHAR(80) UNIQUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_estimada DATE,
    fecha_entrega TIMESTAMP WITH TIME ZONE,
    costo NUMERIC(10, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_envio_pedido FOREIGN KEY (id_pedido) REFERENCES pedido(id_pedido) ON DELETE CASCADE
);

-- Tabla: movimiento_inventario
CREATE TABLE movimiento_inventario (
    id_movimiento BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    id_producto INTEGER NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    cantidad INTEGER NOT NULL,
    stock_anterior INTEGER NOT NULL,
    stock_nuevo INTEGER NOT NULL,
    motivo VARCHAR(200) NOT NULL,
    id_pedido BIGINT,
    id_usuario INTEGER NOT NULL,
    fecha TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_movimiento_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
    CONSTRAINT fk_movimiento_pedido FOREIGN KEY (id_pedido) REFERENCES pedido(id_pedido),
    CONSTRAINT fk_movimiento_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

-- Tabla: historial_estado_pedido
CREATE TABLE historial_estado_pedido (
    id_historial BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    id_pedido BIGINT NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo VARCHAR(20) NOT NULL,
    comentario VARCHAR(250),
    id_usuario INTEGER NOT NULL,
    fecha TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_historial_pedido FOREIGN KEY (id_pedido) REFERENCES pedido(id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_historial_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

-- Tabla: password_reset_token
CREATE TABLE password_reset_token (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    id_usuario INTEGER NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiracion TIMESTAMP WITH TIME ZONE NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_password_reset_token_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
);

-- Índices para rendimiento
CREATE INDEX idx_usuario_correo ON usuario(correo);
CREATE INDEX idx_producto_sku ON producto(sku);
CREATE INDEX idx_producto_categoria ON producto(id_categoria);
CREATE INDEX idx_pedido_usuario ON pedido(id_usuario);
CREATE INDEX idx_pedido_codigo ON pedido(codigo);
CREATE INDEX idx_pedido_estado ON pedido(estado);
CREATE INDEX idx_envio_tracking ON envio(tracking);
CREATE INDEX idx_password_reset_token ON password_reset_token(token);
