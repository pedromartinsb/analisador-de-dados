package br.com.analisador.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class SaleItemTest {

    @Test
    void deveCalcularTotalComoQuantidadeVezesPreco() {
        SaleItem item = new SaleItem("1", 10, new BigDecimal("100"));

        assertEquals(0, new BigDecimal("1000").compareTo(item.total()));
    }

    @Test
    void deveCalcularTotalComPrecoFracionado() {
        SaleItem item = new SaleItem("2", 30, new BigDecimal("2.50"));

        assertEquals(0, new BigDecimal("75.00").compareTo(item.total()));
    }
}
