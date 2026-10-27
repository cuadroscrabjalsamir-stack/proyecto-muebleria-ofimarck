package com.ofimarck;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ofimarck.util.CodigoPedidoUtil;

class CodigoPedidoUtilTest {

    @Test
    void generarCodigoDebeTenerFormatoCorrecto() {
        String codigo = CodigoPedidoUtil.generarCodigo(1);
        assertTrue(codigo.startsWith("ORD-"));
        assertTrue(codigo.endsWith("-0001"));
    }

    @Test
    void formatoMonedaDebeIncluirSimbolo() {
        String resultado = CodigoPedidoUtil.formatoMoneda(1250.50);
        assertTrue(resultado.startsWith("S/ "));
        assertTrue(resultado.contains("1250"));
    }
}