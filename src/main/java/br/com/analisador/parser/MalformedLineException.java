package br.com.analisador.parser;

/**
 * Sinaliza que uma linha do arquivo de entrada não corresponde ao layout do
 * seu tipo de registro (contagem de campos, item de venda malformado, campo
 * numérico inválido). Unchecked porque é tratada em exatamente um lugar
 * ({@link LineParser}) — checked forçaria {@code throws} numa cadeia sem
 * outro handler (§11).
 *
 * <p>A mensagem descreve o motivo da rejeição (ex.: "esperado 4 campos,
 * encontrado 5"), nunca o conteúdo da linha — uma linha rejeitada pode
 * conter CPF ou CNPJ, e logar o conteúdo cru seria vazamento de PII (D8).
 */
public class MalformedLineException extends RuntimeException {

    public MalformedLineException(String message) {
        super(message);
    }

    public MalformedLineException(String message, Throwable cause) {
        super(message, cause);
    }
}
