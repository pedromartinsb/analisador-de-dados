package br.com.analisador.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import br.com.analisador.domain.DataRecord;
import br.com.analisador.domain.ParsedData;

/**
 * Lê um arquivo linha a linha em UTF-8 explícito (D3 — o delimitador
 * {@code ç} é multi-byte e o charset default da JVM varia por SO),
 * delegando cada linha ao {@link LineParser}. Não trata erro de conteúdo —
 * apenas propaga {@link IOException} de leitura, conforme o contrato da
 * camada (§11).
 */
public class FileParser {

    private final LineParser lineParser;

    public FileParser(LineParser lineParser) {
        this.lineParser = Objects.requireNonNull(lineParser, "lineParser");
    }

    public ParsedData parse(Path file) throws IOException {
        List<DataRecord> records = new ArrayList<>();
        AtomicLong lineNumber = new AtomicLong(0);

        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                long currentLine = lineNumber.incrementAndGet();
                lineParser.parse(line, currentLine).ifPresent(records::add);
            });
        }

        return new ParsedData(records);
    }
}
