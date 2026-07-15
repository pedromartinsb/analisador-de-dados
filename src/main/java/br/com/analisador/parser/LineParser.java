package br.com.analisador.parser;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.analisador.domain.DataRecord;

/**
 * Converte uma linha de texto em um {@link DataRecord}, ou registra o
 * motivo da rejeição e devolve {@code Optional.empty()}. Única fronteira
 * que engole erro de parsing (§11) — nada acima dela precisa saber que uma
 * linha pode ser inválida.
 *
 * <p>O dispatch por prefixo em {@link Map} é o que torna o sistema
 * extensível a novos tipos de registro: adicionar um {@code 004} é uma
 * classe nova mais uma entrada na lista passada ao construtor.
 */
public class LineParser {

    private static final Logger log = LoggerFactory.getLogger(LineParser.class);
    private static final String DELIMITER = "ç";

    private final Map<String, RecordParser> parsersByPrefix;

    public LineParser(List<RecordParser> parsers) {
        Objects.requireNonNull(parsers, "parsers");
        this.parsersByPrefix = parsers.stream()
                .collect(Collectors.toUnmodifiableMap(RecordParser::prefix, Function.identity()));
    }

    public Optional<DataRecord> parse(String line, long lineNumber) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }

        String[] fields = line.split(DELIMITER, -1);
        String prefix = fields[0];

        RecordParser parser = parsersByPrefix.get(prefix);
        if (parser == null) {
            log.warn("Linha {}: tipo de registro desconhecido (prefixo '{}')", lineNumber, prefix);
            return Optional.empty();
        }

        try {
            return Optional.of(parser.parse(fields));
        } catch (MalformedLineException e) {
            log.warn("Linha {}: registro malformado - {}", lineNumber, e.getMessage());
            return Optional.empty();
        }
    }
}
