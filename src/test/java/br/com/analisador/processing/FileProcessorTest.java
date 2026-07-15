package br.com.analisador.processing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.analisador.analysis.SalesAnalyzer;
import br.com.analisador.parser.ClientParser;
import br.com.analisador.parser.FileParser;
import br.com.analisador.parser.LineParser;
import br.com.analisador.parser.SaleParser;
import br.com.analisador.parser.SellerParser;
import br.com.analisador.report.ReportFormatter;
import br.com.analisador.report.ReportWriter;

/**
 * Testes de integração do pipeline completo, sem mocks dos componentes
 * internos (D10): FileParser, SalesAnalyzer e ReportWriter reais,
 * exercitados de ponta a ponta a partir de arquivos em {@code @TempDir}.
 */
@Tag("integration")
class FileProcessorTest {

    private static final String EXEMPLO_DO_ENUNCIADO = """
            001ç1234567891234çPedroç50000
            001ç3245678865434çPauloç40000.99
            002ç2345675434544345çJose da SilvaçRural
            002ç2345675433444345çEduardo PereiraçRural
            003ç10ç[1-10-100,2-30-2.50,3-40-3.10]çPedro
            003ç08ç[1-34-10,2-33-1.50,3-40-0.10]çPaulo
            """;

    private FileProcessor newProcessor(Path outputDirectory) {
        FileParser fileParser = new FileParser(
                new LineParser(List.of(new SellerParser(), new ClientParser(), new SaleParser())));
        return new FileProcessor(
                fileParser, new SalesAnalyzer(), new ReportWriter(outputDirectory, new ReportFormatter()));
    }

    @Test
    void caso26_devePipelineCompletoGerarRelatorioComOOraculoDoEnunciado(@TempDir Path tempDir) throws IOException {
        Path inputFile = tempDir.resolve("exemplo.dat");
        Files.writeString(inputFile, EXEMPLO_DO_ENUNCIADO, StandardCharsets.UTF_8);

        FileProcessor processor = newProcessor(tempDir);
        processor.process(inputFile);

        Path outputFile = tempDir.resolve("exemplo.done.dat");
        assertTrue(Files.exists(outputFile));

        String expected = "Quantidade de clientes: 2\n"
                + "Quantidade de vendedores: 2\n"
                + "ID da venda mais cara: 10\n"
                + "Pior vendedor (menor volume de vendas): Paulo\n";
        assertEquals(expected, Files.readString(outputFile, StandardCharsets.UTF_8));
    }

    @Test
    void caso27_deveIgnorarLinhasInvalidasSemInterromperOProcessamento(@TempDir Path tempDir) throws IOException {
        String conteudo = """
                001ç1234567891234çPedroç50000
                001ç123çLinhaMalformada
                002ç2345675434544345çJose da SilvaçRural
                003ç10ç[1-10-100]çPedro
                003ç99ç[item-invalido]çPedro
                """;
        Path inputFile = tempDir.resolve("com-erros.dat");
        Files.writeString(inputFile, conteudo, StandardCharsets.UTF_8);

        FileProcessor processor = newProcessor(tempDir);

        assertDoesNotThrow(() -> processor.process(inputFile));

        Path outputFile = tempDir.resolve("com-erros.done.dat");
        assertTrue(Files.exists(outputFile));

        // Sobrevivem: 1 vendedor (Pedro), 1 cliente (Jose da Silva) e a
        // venda "10" (a venda "99" é descartada por item malformado).
        String expected = "Quantidade de clientes: 1\n"
                + "Quantidade de vendedores: 1\n"
                + "ID da venda mais cara: 10\n"
                + "Pior vendedor (menor volume de vendas): Pedro\n";
        assertEquals(expected, Files.readString(outputFile, StandardCharsets.UTF_8));
    }

    @Test
    void caso28_deveProcessarMultiplosArquivosViaExecutorServiceGerandoUmRelatorioCada(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Path arquivo1 = tempDir.resolve("vendas-norte.dat");
        Files.writeString(arquivo1, "001ç1çAnaç1000\n003ç1ç[1-1-100]çAna\n", StandardCharsets.UTF_8);

        Path arquivo2 = tempDir.resolve("vendas-sul.dat");
        Files.writeString(arquivo2, "001ç2çBrunoç1000\n003ç2ç[1-1-200]çBruno\n", StandardCharsets.UTF_8);

        FileProcessor processor = newProcessor(tempDir);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> processor.process(arquivo1));
        executor.submit(() -> processor.process(arquivo2));
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        String relatorio1 = Files.readString(tempDir.resolve("vendas-norte.done.dat"), StandardCharsets.UTF_8);
        String relatorio2 = Files.readString(tempDir.resolve("vendas-sul.done.dat"), StandardCharsets.UTF_8);

        assertTrue(relatorio1.contains("ID da venda mais cara: 1"));
        assertTrue(relatorio1.contains("Pior vendedor (menor volume de vendas): Ana"));
        assertTrue(relatorio2.contains("ID da venda mais cara: 2"));
        assertTrue(relatorio2.contains("Pior vendedor (menor volume de vendas): Bruno"));
    }

    @Test
    void deveConterFalhaDeIOSemPropagarQuandoArquivoNaoExiste(@TempDir Path tempDir) {
        Path arquivoInexistente = tempDir.resolve("nao-existe.dat");
        FileProcessor processor = newProcessor(tempDir);

        assertDoesNotThrow(() -> processor.process(arquivoInexistente));
    }
}
