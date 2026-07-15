package br.com.analisador.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.analisador.domain.ParsedData;

class FileParserTest {

    private final FileParser fileParser = new FileParser(
            new LineParser(List.of(new SellerParser(), new ClientParser(), new SaleParser())));

    private static final String EXEMPLO_DO_ENUNCIADO = """
            001ç1234567891234çPedroç50000
            001ç3245678865434çPauloç40000.99
            002ç2345675434544345çJose da SilvaçRural
            002ç2345675433444345çEduardo PereiraçRural
            003ç10ç[1-10-100,2-30-2.50,3-40-3.10]çPedro
            003ç08ç[1-34-10,2-33-1.50,3-40-0.10]çPaulo
            """;

    @Test
    void deveParsearOArquivoDeExemploDoEnunciadoPorCompleto(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("exemplo.dat");
        Files.writeString(file, EXEMPLO_DO_ENUNCIADO, StandardCharsets.UTF_8);

        ParsedData data = fileParser.parse(file);

        assertEquals(2, data.sellers().size());
        assertEquals(2, data.clients().size());
        assertEquals(2, data.sales().size());
    }

    @Test
    void deveIgnorarLinhasMalformadasSemInterromperOProcessamento(@TempDir Path tempDir) throws IOException {
        String conteudo = """
                001ç1234567891234çPedroç50000
                001ç123çPedro
                002ç2345675434544345çJose da SilvaçRural
                """;
        Path file = tempDir.resolve("com-erro.dat");
        Files.writeString(file, conteudo, StandardCharsets.UTF_8);

        ParsedData data = fileParser.parse(file);

        assertEquals(1, data.sellers().size());
        assertEquals(1, data.clients().size());
    }

    @Test
    void deveLerArquivoComAcentuacaoEmUtf8(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("acentuado.dat");
        Files.writeString(file, "002ç123çJoão de Souzaçindústria\n", StandardCharsets.UTF_8);

        ParsedData data = fileParser.parse(file);

        assertEquals(1, data.clients().size());
        assertEquals("João de Souza", data.clients().get(0).name());
    }
}
