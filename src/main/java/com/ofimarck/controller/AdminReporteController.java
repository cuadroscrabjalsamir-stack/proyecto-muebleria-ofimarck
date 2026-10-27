package com.ofimarck.controller;

import com.ofimarck.dto.response.DashboardKpiDTO;
import com.ofimarck.dto.response.InventarioAlertaDTO;
import com.ofimarck.dto.response.TopProductoDTO;
import com.ofimarck.dto.response.VentaMensualDTO;
import com.ofimarck.service.ReporteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reportes")
public class AdminReporteController {

    private final ReporteService reporteService;

    public AdminReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardKpiDTO> obtenerKpis() {
        return ResponseEntity.ok(reporteService.obtenerDashboardKpis());
    }

    @GetMapping("/ventas")
    public ResponseEntity<List<VentaMensualDTO>> ventasMensuales() {
        return ResponseEntity.ok(reporteService.obtenerVentasMensuales());
    }

    @GetMapping("/inventario")
    public ResponseEntity<List<InventarioAlertaDTO>> reporteInventario() {
        return ResponseEntity.ok(reporteService.obtenerReporteInventario());
    }

    @GetMapping("/top-productos")
    public ResponseEntity<List<TopProductoDTO>> topProductos() {
        return ResponseEntity.ok(reporteService.obtenerTopProductos());
    }

    @GetMapping("/{tipo}/export")
    public ResponseEntity<byte[]> exportarReporte(
            @PathVariable String tipo,
            @RequestParam(defaultValue = "excel") String formato) throws Exception {

        if ("pdf".equalsIgnoreCase(formato)) {
            byte[] pdfBytes = reporteService.exportarPdf(tipo);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_" + tipo + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } else {
            byte[] excelBytes = reporteService.exportarExcel(tipo);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_" + tipo + ".xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        }
    }
}
