package com.ofimarck.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ofimarck.dto.response.DashboardKpiDTO;
import com.ofimarck.dto.response.InventarioAlertaDTO;
import com.ofimarck.dto.response.TopProductoDTO;
import com.ofimarck.dto.response.VentaMensualDTO;
import com.ofimarck.repository.PedidoRepository;
import com.ofimarck.repository.ProductoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReporteService {

    @PersistenceContext
    private EntityManager entityManager;

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    public ReporteService(PedidoRepository pedidoRepository, ProductoRepository productoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
    }

    public DashboardKpiDTO obtenerDashboardKpis() {
        Query qVentas = entityManager.createNativeQuery(
                "SELECT COALESCE(SUM(total), 0) FROM muebleria.pedido " +
                        "WHERE estado IN ('PAGADO','EN_PREPARACION','EN_TRANSITO','ENTREGADO') " +
                        "AND fecha_pedido >= date_trunc('month', CURRENT_DATE)");
        BigDecimal ventasMes = (BigDecimal) qVentas.getSingleResult();

        long pendientes = pedidoRepository.countPendientes();
        long bajoStock = productoRepository.findAlertasStock().size();
        long totalProductos = productoRepository.count();

        return new DashboardKpiDTO(ventasMes, pendientes, bajoStock, totalProductos);
    }

    @SuppressWarnings("unchecked")
    public List<VentaMensualDTO> obtenerVentasMensuales() {
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
    public List<InventarioAlertaDTO> obtenerReporteInventario() {
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

    @SuppressWarnings("unchecked")
    public List<TopProductoDTO> obtenerTopProductos() {
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT id_producto, nombre, categoria, unidades_vendidas, ingresos " +
                        "FROM muebleria.vw_top_productos")
                .getResultList();

        List<TopProductoDTO> list = new ArrayList<>();
        for (Object[] r : rows) {
            list.add(new TopProductoDTO(
                    ((Number) r[0]).intValue(),
                    (String) r[1],
                    (String) r[2],
                    ((Number) r[3]).intValue(),
                    (BigDecimal) r[4]));
        }
        return list;
    }

    public byte[] exportarExcel(String tipo) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Reporte " + tipo);

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            if ("inventario".equalsIgnoreCase(tipo)) {
                List<InventarioAlertaDTO> data = obtenerReporteInventario();
                String[] columns = { "ID", "SKU", "Producto", "Categoría", "Stock Actual", "Stock Mínimo", "Estado",
                        "Valor Inventario (S/)" };

                org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columns.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerStyle);
                }

                int rowIdx = 1;
                for (InventarioAlertaDTO item : data) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(item.getIdProducto());
                    row.createCell(1).setCellValue(item.getSku());
                    row.createCell(2).setCellValue(item.getNombre());
                    row.createCell(3).setCellValue(item.getCategoria());
                    row.createCell(4).setCellValue(item.getStock());
                    row.createCell(5).setCellValue(item.getStockMinimo());
                    row.createCell(6).setCellValue(item.getEstadoStock());
                    row.createCell(7).setCellValue(
                            item.getValorInventario() != null ? item.getValorInventario().doubleValue() : 0.0);
                }
            } else {
                List<VentaMensualDTO> data = obtenerVentasMensuales();
                String[] columns = { "Mes", "Total Pedidos", "Ventas Totales (S/)", "Ticket Promedio (S/)" };

                org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columns.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerStyle);
                }

                int rowIdx = 1;
                for (VentaMensualDTO item : data) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(item.getMes().toString());
                    row.createCell(1).setCellValue(item.getPedidos());
                    row.createCell(2).setCellValue(item.getVentas() != null ? item.getVentas().doubleValue() : 0.0);
                    row.createCell(3).setCellValue(
                            item.getTicketPromedio() != null ? item.getTicketPromedio().doubleValue() : 0.0);
                }
            }

            for (int i = 0; i < 8; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportarPdf(String tipo) throws Exception {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Mueblería Ofimarck - Reporte de " + tipo.toUpperCase(), titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" "));

        if ("inventario".equalsIgnoreCase(tipo)) {
            List<InventarioAlertaDTO> data = obtenerReporteInventario();
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);

            String[] headers = { "SKU", "Producto", "Categoría", "Stock", "Mínimo", "Estado", "Valor" };
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            for (InventarioAlertaDTO item : data) {
                table.addCell(new Phrase(item.getSku(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(item.getNombre(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(item.getCategoria(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(
                        new Phrase(String.valueOf(item.getStock()), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(String.valueOf(item.getStockMinimo()),
                        FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(item.getEstadoStock(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(
                        new Phrase("S/ " + item.getValorInventario(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
            }
            document.add(table);
        } else {
            List<VentaMensualDTO> data = obtenerVentasMensuales();
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            String[] headers = { "Mes", "Pedidos", "Ventas (S/)", "Ticket Promedio (S/)" };
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            for (VentaMensualDTO item : data) {
                table.addCell(new Phrase(item.getMes().toString(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(
                        new Phrase(String.valueOf(item.getPedidos()), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase("S/ " + item.getVentas(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(
                        new Phrase("S/ " + item.getTicketPromedio(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
            }
            document.add(table);
        }

        document.close();
        return out.toByteArray();
    }
}
