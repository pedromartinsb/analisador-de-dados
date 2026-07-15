package br.com.analisador.parser;

import br.com.analisador.domain.Client;
import br.com.analisador.domain.DataRecord;

/**
 * {@code 002çCNPJçNomeçSegmento de Negócio}
 */
public class ClientParser implements RecordParser {

    private static final int EXPECTED_FIELDS = 4;

    @Override
    public String prefix() {
        return "002";
    }

    @Override
    public DataRecord parse(String[] fields) {
        if (fields.length != EXPECTED_FIELDS) {
            throw new MalformedLineException(
                    "registro 002 esperado com %d campos, encontrado %d"
                            .formatted(EXPECTED_FIELDS, fields.length));
        }
        return new Client(fields[1], fields[2], fields[3]);
    }
}
