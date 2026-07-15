package br.com.analisador.processing;

import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.analisador.analysis.AnalysisResult;
import br.com.analisador.analysis.SalesAnalyzer;
import br.com.analisador.domain.ParsedData;
import br.com.analisador.parser.FileParser;
import br.com.analisador.report.ReportWriter;

/**
 * Orquestra {@code FileParser -> SalesAnalyzer -> ReportWriter} para um
 * único arquivo (§10). Fronteira de contenção (§11): captura qualquer
 * exceção levantada ao processar o arquivo, loga {@code ERROR} com o nome
 * do arquivo, e <strong>nunca propaga</strong> — falha num arquivo não pode
 * derrubar outros arquivos nem a aplicação.
 *
 * <p>Deliberadamente não há validação de argumento nulo no início de
 * {@link #process(Path)}: se {@code file} for {@code null}, a falha ocorre
 * dentro do {@code try} (via {@link FileParser}) e é capturada como
 * qualquer outra — a fronteira de contenção vale inclusive para erros de
 * chamador, não só para dado malformado.
 *
 * <p>Captura {@link Exception}, não {@link Throwable}: erros de JVM (ex.
 * {@code OutOfMemoryError}) não devem ser mascarados como se fossem uma
 * falha de processamento de arquivo.
 */
public class FileProcessor {

    private static final Logger log = LoggerFactory.getLogger(FileProcessor.class);

    private final FileParser parser;
    private final SalesAnalyzer analyzer;
    private final ReportWriter writer;

    public FileProcessor(FileParser parser, SalesAnalyzer analyzer, ReportWriter writer) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer");
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    public void process(Path file) {
        try {
            ParsedData data = parser.parse(file);
            AnalysisResult result = analyzer.analyze(data);
            writer.write(file.getFileName().toString(), result);
        } catch (Exception e) {
            log.error("Falha ao processar o arquivo {}: {}", file, e.getMessage(), e);
        }
    }
}
