package br.com.analisador.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import br.com.analisador.analysis.AnalysisResult;

/**
 * Escreve o relatório formatado no diretório de saída, com o nome derivado
 * do arquivo de entrada conforme D16: a última extensão do nome original é
 * removida antes de anexar {@code .done.dat} — {@code dados.dat} vira
 * {@code dados.done.dat}; {@code relatorio.vendas.dat} vira
 * {@code relatorio.vendas.done.dat} (só a última extensão sai).
 *
 * <p>Única classe deste pacote que toca disco; a formatação em si vive em
 * {@link ReportFormatter}, testável sem I/O.
 */
public class ReportWriter {

    private static final String OUTPUT_SUFFIX = ".done.dat";

    private final Path outputDirectory;
    private final ReportFormatter formatter;

    public ReportWriter(Path outputDirectory, ReportFormatter formatter) {
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    public void write(String originalFileName, AnalysisResult result) throws IOException {
        Objects.requireNonNull(originalFileName, "originalFileName");
        Objects.requireNonNull(result, "result");

        Path outputFile = outputDirectory.resolve(outputFileName(originalFileName));
        String content = formatter.format(result);
        Files.writeString(outputFile, content, StandardCharsets.UTF_8);
    }

    private String outputFileName(String originalFileName) {
        int lastDot = originalFileName.lastIndexOf('.');
        String base = (lastDot > 0) ? originalFileName.substring(0, lastDot) : originalFileName;
        return base + OUTPUT_SUFFIX;
    }
}
