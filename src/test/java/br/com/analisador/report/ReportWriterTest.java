package br.com.analisador.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.analisador.analysis.AnalysisResult;

class ReportWriterTest {

    private final ReportFormatter formatter = new ReportFormatter();

    @Test
    void caso25_deveRemoverAExtensaoOriginalDoNomeDeSaida(@TempDir Path tempDir) throws IOException {
        ReportWriter writer = new ReportWriter(tempDir, formatter);
        AnalysisResult result = new AnalysisResult(1, 1, Optional.of("1"), Optional.of("Pedro"));

        writer.write("dados.dat", result);

        assertTrue(Files.exists(tempDir.resolve("dados.done.dat")));
    }

    @Test
    void caso25b_deveRemoverApenasAUltimaExtensaoEmNomeComPontosNoMeio(@TempDir Path tempDir) throws IOException {
        ReportWriter writer = new ReportWriter(tempDir, formatter);
        AnalysisResult result = new AnalysisResult(1, 1, Optional.of("1"), Optional.of("Pedro"));

        writer.write("relatorio.vendas.dat", result);

        assertTrue(Files.exists(tempDir.resolve("relatorio.vendas.done.dat")));
    }

    @Test
    void deveEscreverOConteudoFormatadoEmUtf8(@TempDir Path tempDir) throws IOException {
        ReportWriter writer = new ReportWriter(tempDir, formatter);
        AnalysisResult result = new AnalysisResult(2, 2, Optional.of("10"), Optional.of("José"));

        writer.write("exemplo.dat", result);

        String content = Files.readString(tempDir.resolve("exemplo.done.dat"), StandardCharsets.UTF_8);
        assertEquals(formatter.format(result), content);
    }

    @Test
    void deveGerarNomeDeSaidaMesmoSemExtensaoOriginal(@TempDir Path tempDir) throws IOException {
        ReportWriter writer = new ReportWriter(tempDir, formatter);
        AnalysisResult result = new AnalysisResult(0, 0, Optional.empty(), Optional.empty());

        writer.write("semextensao", result);

        assertTrue(Files.exists(tempDir.resolve("semextensao.done.dat")));
    }
}
