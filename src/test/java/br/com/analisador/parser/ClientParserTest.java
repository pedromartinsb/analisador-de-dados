package br.com.analisador.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.analisador.domain.Client;
import br.com.analisador.domain.DataRecord;

class ClientParserTest {

    private final ClientParser parser = new ClientParser();

    @Test
    void devolvePrefixo002() {
        assertEquals("002", parser.prefix());
    }

    @Test
    void caso3_deveParsearCliente() {
        // 002ç2345675434544345çJose da SilvaçRural
        DataRecord record = parser.parse(new String[] {"002", "2345675434544345", "Jose da Silva", "Rural"});

        Client client = (Client) record;
        assertEquals("2345675434544345", client.cnpj());
        assertEquals("Jose da Silva", client.name());
        assertEquals("Rural", client.businessSegment());
    }

    @Test
    void deveRejeitarLinhaComContagemDeCamposErrada() {
        assertThrows(MalformedLineException.class,
                () -> parser.parse(new String[] {"002", "123", "Jose"}));
    }
}
