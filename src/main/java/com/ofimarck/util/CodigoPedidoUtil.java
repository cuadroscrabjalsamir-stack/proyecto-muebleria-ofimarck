package com.ofimarck.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class CodigoPedidoUtil {

    private static final String PREFIJO = "ORD";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CodigoPedidoUtil() {
    }

    public static String generarCodigo(Integer secuencia) {
        String fecha = LocalDate.now().format(FORMATO_FECHA);
        String seq = String.format("%04d", secuencia);
        return PREFIJO + "-" + fecha + "-" + seq;
    }

    public static String formatoMoneda(Double monto) {
        return String.format("S/ %.2f", monto);
    }
}