package br.com.analisador.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
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
 * Testes de integração do {@link DirectoryWatcher} — casos 29 a 31 (§12).
 * Usa diretórios reais em {@code @TempDir}: o {@link java.nio.file.WatchService}
 * do NIO não tem equivalente fiel em memória, então esses testes dependem
 * do sistema de arquivos e de tempo real (não são instantâneos por
 * natureza).
 */
@Tag("integration")
class DirectoryWatcherTest {

    private Thread watcherThread;

    @AfterEach
    void stopWatcher() {
        if (watcherThread != null && watcherThread.isAlive()) {
            watcherThread.interrupt();
        }
    }

    // --- Testes de unidade dos métodos package-private (§10) ---

    @Test
    void scanExisting_deveEncontrarApenasArquivosComAExtensaoConfigurada(@TempDir Path tempDir) throws IOException {
        Path alvo = tempDir.resolve("ja-existia.dat");
        Files.writeString(alvo, "conteudo", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("ignorar.txt"), "conteudo", StandardCharsets.UTF_8);

        DirectoryWatcher watcher = new DirectoryWatcher(tempDir, ".dat");

        List<Path> encontrados = watcher.scanExisting();

        assertEquals(1, encontrados.size());
        assertEquals(alvo, encontrados.get(0));
    }

    @Test
    void isTarget_deveAceitarApenasArquivosComAExtensaoConfigurada(@TempDir Path tempDir) throws IOException {
        Path dat = tempDir.resolve("a.dat");
        Path txt = tempDir.resolve("a.txt");
        Files.writeString(dat, "x", StandardCharsets.UTF_8);
        Files.writeString(txt, "x", StandardCharsets.UTF_8);

        DirectoryWatcher watcher = new DirectoryWatcher(tempDir, ".dat");

        assertTrue(watcher.isTarget(dat));
        assertFalse(watcher.isTarget(txt));
    }

    @Test
    void isTarget_deveRejeitarDiretorio(@TempDir Path tempDir) {
        DirectoryWatcher watcher = new DirectoryWatcher(tempDir, ".dat");

        assertFalse(watcher.isTarget(tempDir));
    }

    @Test
    void awaitStable_deveRetornarSemBloquearIndefinidamenteParaArquivoJaCompleto(@TempDir Path tempDir)
            throws Exception {
        Path arquivo = tempDir.resolve("estavel.dat");
        Files.writeString(arquivo, "conteudo fixo", StandardCharsets.UTF_8);

        DirectoryWatcher watcher = new DirectoryWatcher(tempDir, ".dat");

        long inicio = System.currentTimeMillis();
        watcher.awaitStable(arquivo);
        long duracao = System.currentTimeMillis() - inicio;

        // Não é uma garantia de tempo formal — só confirma que a mitigação
        // pragmática não trava indefinidamente para um arquivo já completo.
        assertTrue(duracao < 2000);
    }

    // --- Casos 29 a 31: integração real via WatchService ---

    @Test
    void caso29_deveDetectarArquivoNovoAposIniciarOMonitoramento(@TempDir Path tempDir) throws Exception {
        CountDownLatch detectado = new CountDownLatch(1);
        Set<Path> recebidos = ConcurrentHashMap.newKeySet();

        DirectoryWatcher watcher = new DirectoryWatcher(tempDir, ".dat");
        watcherThread = startWatcher(watcher, file -> {
            recebidos.add(file);
            detectado.countDown();
        });

        Path novoArquivo = tempDir.resolve("novo.dat");
        Files.writeString(novoArquivo, "001ç1çAnaç1000", StandardCharsets.UTF_8);

        assertTrue(detectado.await(5, TimeUnit.SECONDS), "o handler deveria ter sido chamado dentro do timeout");
        assertTrue(recebidos.contains(novoArquivo));
    }

    @Test
    void caso30_deveIgnorarArquivoComExtensaoDiferente(@TempDir Path tempDir) throws Exception {
        CountDownLatch chamadoParaTxt = new CountDownLatch(1);
        CountDownLatch chamadoParaDat = new CountDownLatch(1);

        DirectoryWatcher watcher = new DirectoryWatcher(tempDir, ".dat");
        watcherThread = startWatcher(watcher, file -> {
            if (file.toString().endsWith(".txt")) {
                chamadoParaTxt.countDown();
            } else {
                chamadoParaDat.countDown();
            }
        });

        Files.writeString(tempDir.resolve("ignorar.txt"), "conteudo", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("processar.dat"), "conteudo", StandardCharsets.UTF_8);

        assertTrue(chamadoParaDat.await(5, TimeUnit.SECONDS), "o .dat deveria ter sido processado");
        assertFalse(chamadoParaTxt.await(500, TimeUnit.MILLISECONDS), "o .txt não deveria disparar o handler");
    }

    @Test
    void caso31_devePipelineCompletoProcessarArquivoComAcentuacaoUtf8ViaWatcher(@TempDir Path tempDir)
            throws Exception {
        // Diretórios de entrada e saída propositalmente distintos — ver a
        // nota na Javadoc de DirectoryWatcher sobre o ciclo de
        // retroalimentação que ocorreria se coincidissem.
        Path inputDir = Files.createDirectory(tempDir.resolve("in"));
        Path outputDir = Files.createDirectory(tempDir.resolve("out"));

        FileParser fileParser = new FileParser(
                new LineParser(List.of(new SellerParser(), new ClientParser(), new SaleParser())));
        FileProcessor processor = new FileProcessor(
                fileParser, new SalesAnalyzer(), new ReportWriter(outputDir, new ReportFormatter()));

        CountDownLatch processado = new CountDownLatch(1);
        DirectoryWatcher watcher = new DirectoryWatcher(inputDir, ".dat");
        watcherThread = startWatcher(watcher, file -> {
            processor.process(file);
            processado.countDown();
        });

        Files.writeString(inputDir.resolve("clientes.dat"),
                "002ç123çJoão de Souzaçindústria\n", StandardCharsets.UTF_8);

        assertTrue(processado.await(5, TimeUnit.SECONDS));

        String relatorio = Files.readString(outputDir.resolve("clientes.done.dat"), StandardCharsets.UTF_8);
        assertTrue(relatorio.contains("Quantidade de clientes: 1"));
    }

    private Thread startWatcher(DirectoryWatcher watcher, java.util.function.Consumer<Path> handler)
            throws InterruptedException {
        Thread thread = new Thread(() -> {
            try {
                watcher.watch(handler);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
        // Garante que o WatchService já está registrado antes do teste criar
        // arquivos, evitando corrida entre o registro e o evento de criação.
        Thread.sleep(300);
        return thread;
    }
}
