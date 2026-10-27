# Sistema Web Mueblería Ofimarck — Módulo Administrador & Backend

Plataforma integral de comercio electrónico y gestión de inventarios para **Mueblería Ofimarck** (Trabajo Final — Curso Integrador I).

## 🚀 Componentes Desarrollados

### 1. Base de Datos Relacional (PostgreSQL 15+)
- **Esquema:** `muebleria` con 11 tablas relacionales normalizadas (3FN):
  - `rol`, `usuario`, `direccion_envio`
  - `categoria`, `producto`
  - `pedido`, `detalle_pedido`, `pago`, `envio`
  - `movimiento_inventario` (Kardex), `historial_estado_pedido`
- **Automatizaciones por Triggers:**
  - Generación de código de pedido (`ORD-YYYYMMDD-XXXX`).
  - Validación de stock antes del pedido.
  - Descuento automático de stock e inserción de kardex al confirmar pago.
  - Registro de auditoría por cambio de estado de pedido.
  - Restauración automática de existencias al cancelar pedido.
- **Vistas Analíticas:**
  - `vw_ventas_mensuales`
  - `vw_inventario_alertas`
  - `vw_top_productos`
- **Datos Semilla:**
  - Roles (`ADMIN`, `CLIENTE`), categorías (`Salas`, `Comedores`, `Dormitorios`, `Oficina`), catálogo oficial de productos e imágenes del proyecto, usuario administrador y pedido de demostración.

### 2. Backend (Java 17 / Spring Boot 3.2.12)
- **Arquitectura:** MVC por capas (Controlador -> Servicio -> Repositorio / DAO -> Dominio).
- **Seguridad:** Spring Security con codificación de contraseñas mediante `BCryptPasswordEncoder`.
- **Módulo de Reportes:**
  - Exportación a **Excel (.xlsx)** con `Apache POI`.
  - Exportación a **PDF (.pdf)** con `OpenPDF`.
- **API REST para Administración:**
  - `/api/admin/productos` (CRUD completo y cambio de estado).
  - `/api/admin/categorias` (CRUD completo).
  - `/api/admin/inventario/ajustes` (Entradas de taller y ajustes con kardex).
  - `/api/admin/inventario/movimientos` (Kardex histórico).
  - `/api/admin/pedidos` (Listado con filtros, detalle y actualización de estado).
  - `/api/admin/pedidos/{id}/envio` (Transportista, tracking y entrega).
  - `/api/admin/pagos/{id}/validar` (Validación de transferencias).
  - `/api/admin/reportes/dashboard` (KPIs gerenciales).
  - `/api/admin/reportes/ventas`, `/inventario`, `/top-productos`.
  - `/api/admin/reportes/{tipo}/export` (Descarga directa Excel/PDF).
  - `/api/admin/usuarios` (Gestión de usuarios y activación de cuentas).

### 3. Frontend Administrador (Bootstrap 5.3 + JavaScript ES6 + Chart.js)
- **Dashboard:** KPIs en tiempo real (Ventas del mes, pedidos pendientes, alertas de stock, catálogo total), gráficos interactivos de ventas e inventario, y tabla rápida de productos críticos.
- **Gestión de Catálogo:** Tabla con miniaturas, búsqueda por SKU/nombre, filtro por categoría, modal para registro/edición y activación/desactivación.
- **Gestión de Categorías:** CRUD dinámico y sincronizado.
- **Kardex e Inventario:** Registro de entradas de fabricación y ajustes, tabla histórica con trazabilidad de usuario y fecha.
- **Gestión de Pedidos & Logística:** Desglose de ítems, snapshot de dirección de entrega, actualización de estado y asignación de transportista/código de rastreo.
- **Validación de Pagos:** Panel para aprobar o rechazar transferencias bancarias.
- **Exportación:** Botones de descarga directa de reportes en Excel y PDF.
- **Usuarios & Roles:** Monitoreo y control de acceso.

---

## 💻 Acceso Rápido

- **URL del Panel Administrador:** [http://localhost:8080/admin](http://localhost:8080/admin)
- **Credenciales por defecto:**
  - **Correo:** `admin@ofimarck.pe`
  - **Contraseña:** `admin123`
