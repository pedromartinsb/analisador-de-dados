package br.com.analisador.processing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.analisador.analysis.AnalysisResult;
import br.com.analisador.analysis.SalesAnalyzer;
import br.com.analisador.domain.ParsedData;
import br.com.analisador.parser.FileParser;
import br.com.analisador.report.ReportWriter;

/**
 * Testes de orquestração do {@link FileProcessor} com colaboradores
 * mockados. Complementa {@link FileProcessorIntegrationTest}, que exercita
 * o pipeline real: aqui o alvo é o contrato entre as camadas (ordem das
 * chamadas, o que é repassado ao writer) e a fronteira de contenção diante
 * de falhas que a integração não consegue forçar.
 */
@ExtendWith(MockitoExtension.class)
class FileProcessorTest {

    private static final Path ARQUIVO = Path.of("/tmp/entrada/dados.dat");

    @Mock
    private FileParser parser;

    @Mock
    private SalesAnalyzer analyzer;

    @Mock
    private ReportWriter writer;

    private FileProcessor processor;

    private final ParsedData parsed = new ParsedData(List.of());
    private final AnalysisResult result =
            new AnalysisResult(0, 0, Optional.empty(), Optional.empty());

    @BeforeEach
    void setUp() {
        processor = new FileProcessor(parser, analyzer, writer);
    }

    @Test
    void deveExecutarOPipelineNaOrdemParserAnalyzerWriter() throws IOException {
        when(parser.parse(ARQUIVO)).thenReturn(parsed);
        when(analyzer.analyze(parsed)).thenReturn(result);

        processor.process(ARQUIVO);

        InOrder inOrder = inOrder(parser, analyzer, writer);
        inOrder.verify(parser).parse(ARQUIVO);
        inOrder.verify(analyzer).analyze(parsed);
        // O writer recebe o nome do arquivo, não o caminho completo.
        inOrder.verify(writer).write("dados.dat", result);
    }

    @Test
    void naoDevePropagarNemChamarAsCamadasSeguintesQuandoOParserFalha() throws IOException {
        when(parser.parse(ARQUIVO)).thenThrow(new IOException("arquivo ilegível"));

        assertDoesNotThrow(() -> processor.process(ARQUIVO));

        verifyNoInteractions(analyzer, writer);
    }

    @Test
    void naoDevePropagarNemEscreverRelatorioQuandoOAnalyzerFalha() throws IOException {
        when(parser.parse(ARQUIVO)).thenReturn(parsed);
        when(analyzer.analyze(parsed)).thenThrow(new IllegalStateException("falha simulada"));

        assertDoesNotThrow(() -> processor.process(ARQUIVO));

        verifyNoInteractions(writer);
    }

    @Test
    void naoDevePropagarQuandoAEscritaDoRelatorioFalha() throws IOException {
        when(parser.parse(ARQUIVO)).thenReturn(parsed);
        when(analyzer.analyze(parsed)).thenReturn(result);
        doThrow(new IOException("disco cheio")).when(writer).write(any(), any());

        assertDoesNotThrow(() -> processor.process(ARQUIVO));
    }
}
