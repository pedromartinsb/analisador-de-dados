package br.com.analisador.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class SaleTest {

    @Test
    void deveSomarTotalDeTodosOsItens_exemploDoEnunciado_Pedro() {
        // 003ç10ç[1-10-100,2-30-2.50,3-40-3.10]çPedro
        Sale sale = new Sale("10", List.of(
                new SaleItem("1", 10, new BigDecimal("100")),
                new SaleItem("2", 30, new BigDecimal("2.50")),
                new SaleItem("3", 40, new BigDecimal("3.10"))
        ), "Pedro");

        assertEquals(0, new BigDecimal("1199.00").compareTo(sale.total()));
    }

    @Test
    void deveSomarTotalDeTodosOsItens_exemploDoEnunciado_Paulo() {
        // 003ç08ç[1-34-10,2-33-1.50,3-40-0.10]çPaulo
        Sale sale = new Sale("08", List.of(
                new SaleItem("1", 34, new BigDecimal("10")),
                new SaleItem("2", 33, new BigDecimal("1.50")),
                new SaleItem("3", 40, new BigDecimal("0.10"))
        ), "Paulo");

        assertEquals(0, new BigDecimal("393.50").compareTo(sale.total()));
    }

    @Test
    void totalDeVendaSemItensDeveSerZero() {
        Sale sale = new Sale("99", List.of(), "Pedro");

        assertEquals(0, BigDecimal.ZERO.compareTo(sale.total()));
    }
}
