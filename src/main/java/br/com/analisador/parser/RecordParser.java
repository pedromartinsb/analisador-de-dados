package br.com.analisador.parser;

import br.com.analisador.domain.DataRecord;

/**
 * Converte os campos de uma linha (já separados pelo delimitador {@code ç})
 * no registro de domínio correspondente ao seu prefixo. Implementação pura:
 * não loga, não captura — apenas lança {@link MalformedLineException} quando
 * o layout não bate (§11).
 *
 * <p>A validação de contagem de campos é responsabilidade de cada
 * implementação, não de um checador genérico: um tipo de registro futuro
 * pode ter uma contagem diferente dos três atuais.
 */
public interface RecordParser {

    /** Prefixo numérico que identifica o tipo de registro (ex.: {@code "001"}). */
    String prefix();

    /**
     * @param fields campos da linha já divididos por {@code ç} via
     *               {@code split(-1)}; {@code fields[0]} é o próprio prefixo.
     */
    DataRecord parse(String[] fields);
}
