package br.com.analisador.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.analisador.domain.DataRecord;
import br.com.analisador.domain.Sale;

class SaleParserTest {

    private final SaleParser parser = new SaleParser();

    @Test
    void devolvePrefixo003() {
        assertEquals("003", parser.prefix());
    }

    @Test
    void caso4_deveParsearVendaComTresItens_exemploPedro() {
        // 003ç10ç[1-10-100,2-30-2.50,3-40-3.10]çPedro
        DataRecord record = parser.parse(
                new String[] {"003", "10", "[1-10-100,2-30-2.50,3-40-3.10]", "Pedro"});

        Sale sale = (Sale) record;
        assertEquals("10", sale.id());
        assertEquals("Pedro", sale.sellerName());
        assertEquals(3, sale.items().size());
        assertEquals(0, new BigDecimal("1199.00").compareTo(sale.total()));
    }

    @Test
    void caso5_devePreservarZeroAEsquerdaNoIdDaVenda() {
        // 003ç08ç[1-34-10,2-33-1.50,3-40-0.10]çPaulo
        DataRecord record = parser.parse(
                new String[] {"003", "08", "[1-34-10,2-33-1.50,3-40-0.10]", "Paulo"});

        Sale sale = (Sale) record;
        assertEquals("08", sale.id());
        assertTrue(sale.id().startsWith("0"), "o id deve permanecer String, sem virar int");
        assertEquals(0, new BigDecimal("393.50").compareTo(sale.total()));
    }

    @Test
    void caso10_deveRejeitarItemComContagemDePartesErrada() {
        // 003ç10ç[1-10]çPedro — item com 2 partes em vez de 3
        assertThrows(MalformedLineException.class,
                () -> parser.parse(new String[] {"003", "10", "[1-10]", "Pedro"}));
    }

    @Test
    void caso11_deveAceitarListaDeItensVazia() {
        // 003ç10ç[]çPedro
        DataRecord record = parser.parse(new String[] {"003", "10", "[]", "Pedro"});

        Sale sale = (Sale) record;
        assertEquals(0, sale.items().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(sale.total()));
    }

    @Test
    void deveRejeitarItensSemDelimitadores() {
        assertThrows(MalformedLineException.class,
                () -> parser.parse(new String[] {"003", "10", "1-10-100", "Pedro"}));
    }

    @Test
    void deveRejeitarLinhaComContagemDeCamposErrada() {
        assertThrows(MalformedLineException.class,
                () -> parser.parse(new String[] {"003", "10", "[1-10-100]"}));
    }
}
