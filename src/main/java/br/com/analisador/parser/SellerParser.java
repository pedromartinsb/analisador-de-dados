package br.com.analisador.parser;

import java.math.BigDecimal;

import br.com.analisador.domain.DataRecord;
import br.com.analisador.domain.Seller;

/**
 * {@code 001çCPFçNomeçSalário}
 */
public class SellerParser implements RecordParser {

    private static final int EXPECTED_FIELDS = 4;

    @Override
    public String prefix() {
        return "001";
    }

    @Override
    public DataRecord parse(String[] fields) {
        if (fields.length != EXPECTED_FIELDS) {
            throw new MalformedLineException(
                    "registro 001 esperado com %d campos, encontrado %d"
                            .formatted(EXPECTED_FIELDS, fields.length));
        }
        String cpf = fields[1];
        String name = fields[2];
        BigDecimal salary = parseSalary(fields[3]);
        return new Seller(cpf, name, salary);
    }

    private BigDecimal parseSalary(String raw) {
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new MalformedLineException("salário inválido no registro 001", e);
        }
    }
}
