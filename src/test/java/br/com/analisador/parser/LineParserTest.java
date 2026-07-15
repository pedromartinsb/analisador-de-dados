package br.com.analisador.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.analisador.domain.DataRecord;
import br.com.analisador.domain.Seller;

class LineParserTest {

    private final LineParser lineParser = new LineParser(
            List.of(new SellerParser(), new ClientParser(), new SaleParser()));

    @Test
    void deveDelegarParaOParserCorrespondenteAoPrefixo() {
        Optional<DataRecord> result = lineParser.parse("001ç1234567891234çPedroç50000", 1);

        assertTrue(result.isPresent());
        assertEquals("Pedro", ((Seller) result.get()).name());
    }

    @Test
    void caso6_deveIgnorarPrefixoDesconhecido() {
        // 004ç... — tipo de registro inexistente
        Optional<DataRecord> result = lineParser.parse("004ç1234çqualquerçcoisa", 6);

        assertTrue(result.isEmpty());
    }

    @Test
    void caso12_deveRejeitarNomeContendoODelimitador() {
        // "Conceição" contém 'ç', quebrando o split em campo extra:
        // 001ç123çConceiçãoç50000 -> 5 campos após split, não 4.
        // O formato não define escaping; a linha é rejeitada, não corrompida
        // silenciosamente (D9).
        Optional<DataRecord> result = lineParser.parse("001ç123çConceiçãoç50000", 12);

        assertTrue(result.isEmpty());
    }

    @Test
    void caso13_deveIgnorarLinhaVaziaSemGerarRegistro() {
        assertTrue(lineParser.parse("", 13).isEmpty());
        assertTrue(lineParser.parse("   ", 14).isEmpty());
    }

    @Test
    void deveIgnorarLinhaSemDelimitadorAlgum() {
        Optional<DataRecord> result = lineParser.parse("linha completamente fora do formato", 99);

        assertTrue(result.isEmpty());
    }
}
