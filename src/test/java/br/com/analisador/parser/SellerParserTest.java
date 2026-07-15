package br.com.analisador.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.analisador.domain.DataRecord;
import br.com.analisador.domain.Seller;

class SellerParserTest {

    private final SellerParser parser = new SellerParser();

    @Test
    void devolvePrefixo001() {
        assertEquals("001", parser.prefix());
    }

    @Test
    void caso1_deveParsearVendedorComSalarioInteiro() {
        // 001ç1234567891234çPedroç50000
        DataRecord record = parser.parse(new String[] {"001", "1234567891234", "Pedro", "50000"});

        Seller seller = (Seller) record;
        assertEquals("1234567891234", seller.cpf());
        assertEquals("Pedro", seller.name());
        assertEquals(0, new BigDecimal("50000").compareTo(seller.salary()));
    }

    @Test
    void caso2_deveParsearSalarioComCasasDecimais() {
        // 001ç3245678865434çPauloç40000.99
        DataRecord record = parser.parse(new String[] {"001", "3245678865434", "Paulo", "40000.99"});

        Seller seller = (Seller) record;
        assertEquals(0, new BigDecimal("40000.99").compareTo(seller.salary()));
    }

    @Test
    void caso7_deveRejeitarLinhaComMenosCamposQueOEsperado() {
        // 001ç123çPedro (3 campos)
        assertThrows(MalformedLineException.class,
                () -> parser.parse(new String[] {"001", "123", "Pedro"}));
    }

    @Test
    void caso8_deveRejeitarLinhaComMaisCamposQueOEsperado() {
        // 001ç123çPedroç1ç2 (5 campos)
        assertThrows(MalformedLineException.class,
                () -> parser.parse(new String[] {"001", "123", "Pedro", "1", "2"}));
    }

    @Test
    void caso9_deveRejeitarSalarioNaoNumerico() {
        // 001ç123çPedroçabc
        assertThrows(MalformedLineException.class,
                () -> parser.parse(new String[] {"001", "123", "Pedro", "abc"}));
    }
}
