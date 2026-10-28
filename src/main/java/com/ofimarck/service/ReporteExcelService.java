package com.ofimarck.service;

import com.ofimarck.dto.response.InventarioAlertaDTO;
import com.ofimarck.dto.response.VentaMensualDTO;
import com.ofimarck.repository.PedidoRepository;
import com.ofimarck.repository.ProductoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio dedicado a la generación de reportes en formato Excel (.xlsx) usando Apache POI.
 *
 * <p>Principio de Responsabilidad Única (S de SOLID): esta clase se encarga
 * exclusivamente de construir el libro de Excel con los datos de reportes
 * de ventas mensuales e inventario. No contiene lógica de negocio de dominio.
 *
 * <p>Uso de Apache POI (rúbrica FASE 2, sección 2.3.3 del documento):
 * <ul>
 *   <li>{@link XSSFWorkbook}: crea el archivo .xlsx en memoria.</li>
 *   <li>{@link Sheet}, {@link Row}, {@link Cell}, {@link CellStyle},
 *       {@link Font}: construyen la estructura, encabezados con estilo y
 *       datos del reporte.</li>
 *   <li>Se admiten dos tipos de reporte: "ventas" (Ventas Mensuales) y
 *       "inventario" (Inventario con alertas de stock).</li>
 * </ul>
 *
 * <p>Uso de Logback/SLF4J: registro de generación de reportes para auditoría
 * (no se loguean datos sensibles de clientes).
 */
@Service
public class ReporteExcelService {

    private static final Logger log = LoggerFactory.getLogger(ReporteExcelService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    public ReporteExcelService(PedidoRepository pedidoRepository, ProductoRepository productoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
    }

    /**
     * Genera un archivo Excel (.xlsx) con el reporte solicitado.
     *
     * @param tipo tipo de reporte: {@code "ventas"} o {@code "inventario"}
     * @return arreglo de bytes con el contenido del archivo .xlsx
     * @throws Exception si ocurre un error al escribir el libro de Excel
     */
    public byte[] generarReporte(String tipo) throws Exception {
        log.info("reporte_excel_generado tipo={}", tipo);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(capitalizar(tipo));

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setWrapText(true);

            if ("inventario".equalsIgnoreCase(tipo)) {
                generarHojaInventario(sheet, headerStyle, dataStyle);
            } else {
                // Por defecto: reporte de ventas mensuales
                generarHojaVentasMensuales(sheet, headerStyle, dataStyle);
            }

            // Autoajuste de columnas (primeras 8)
            for (int i = 0; i < 8; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void generarHojaVentasMensuales(Sheet sheet, CellStyle headerStyle, CellStyle dataStyle) {
        List<VentaMensualDTO> data = obtenerVentasMensuales();

        String[] columns = { "Mes", "Total Pedidos", "Ventas Totales (S/)", "Ticket Promedio (S/)" };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (VentaMensualDTO item : data) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getMes() != null ? item.getMes().toString() : "");
            row.createCell(1).setCellValue(item.getPedidos());
            row.createCell(2).setCellValue(
                    item.getVentas() != null ? item.getVentas().doubleValue() : 0.0);
            row.createCell(3).setCellValue(
                    item.getTicketPromedio() != null ? item.getTicketPromedio().doubleValue() : 0.0);
            for (int c = 0; c < columns.length; c++) {
                row.getCell(c).setCellStyle(dataStyle);
            }
        }
    }

    private void generarHojaInventario(Sheet sheet, CellStyle headerStyle, CellStyle dataStyle) {
        List<InventarioAlertaDTO> data = obtenerReporteInventario();

        String[] columns = { "ID", "SKU", "Producto", "Categoría", "Stock Actual",
                "Stock Mínimo", "Estado", "Valor Inventario (S/)" };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (InventarioAlertaDTO item : data) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getIdProducto());
            row.createCell(1).setCellValue(item.getSku());
            row.createCell(2).setCellValue(item.getNombre());
            row.createCell(3).setCellValue(item.getCategoria());
            row.createCell(4).setCellValue(item.getStock());
            row.createCell(5).setCellValue(item.getStockMinimo());
            row.createCell(6).setCellValue(item.getEstadoStock());
            row.createCell(7).setCellValue(
                    item.getValorInventario() != null ? item.getValorInventario().doubleValue() : 0.0);
            for (int c = 0; c < columns.length; c++) {
                row.getCell(c).setCellStyle(dataStyle);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<VentaMensualDTO> obtenerVentasMensuales() {
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT mes, pedidos, ventas, ticket_promedio FROM muebleria.vw_ventas_mensuales")
                .getResultList();

        List<VentaMensualDTO> list = new ArrayList<>();
        for (Object[] r : rows) {
            Date mesDate = (Date) r[0];
            Number pedidos = (Number) r[1];
            BigDecimal ventas = (BigDecimal) r[2];
            BigDecimal ticket = (BigDecimal) r[3];
            list.add(new VentaMensualDTO(
                    mesDate.toLocalDate(),
                    pedidos.longValue(),
                    ventas,
                    ticket));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private List<InventarioAlertaDTO> obtenerReporteInventario() {
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT id_producto, sku, nombre, categoria, stock, stock_minimo, estado_stock, valor_inventario " +
                        "FROM muebleria.vw_inventario_alertas ORDER BY stock ASC")
                .getResultList();

        List<InventarioAlertaDTO> list = new ArrayList<>();
        for (Object[] r : rows) {
            list.add(new InventarioAlertaDTO(
                    ((Number) r[0]).intValue(),
                    (String) r[1],
                    (String) r[2],
                    (String) r[3],
                    ((Number) r[4]).intValue(),
                    ((Number) r[5]).intValue(),
                    (String) r[6],
                    (BigDecimal) r[7]));
        }
        return list;
    }

    private String capitalizar(String tipo) {
        if (tipo == null || tipo.isEmpty()) {
            return "Reporte";
        }
        return tipo.substring(0, 1).toUpperCase() + tipo.substring(1).toLowerCase();
    }
}
