package br.com.analisador.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class ParsedDataTest {

    @Test
    void deveFiltrarRegistrosPorTipo() {
        Seller seller = new Seller("123", "Pedro", new BigDecimal("50000"));
        Client client = new Client("456", "Jose", "Rural");
        Sale sale = new Sale("1", List.of(), "Pedro");

        ParsedData data = new ParsedData(List.of(seller, client, sale));

        assertEquals(List.of(seller), data.sellers());
        assertEquals(List.of(client), data.clients());
        assertEquals(List.of(sale), data.sales());
    }
}
